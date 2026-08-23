#!/usr/bin/env python3
from __future__ import annotations

import argparse
from collections import Counter
import hashlib
import io
import json
import re
import tomllib
import zipfile
from pathlib import Path


EXPECTED_GTCEU_DEPENDENCIES = {
    "neoforge": ("required", "[21.1.240,)", "BOTH"),
    "minecraft": ("required", "[1.21.1]", "BOTH"),
    "ae2": ("required", "[19.2.17]", "BOTH"),
    "guideme": ("required", "[21.1.17]", "BOTH"),
    "ldlib": ("required", "[1.0.41]", "BOTH"),
    "extendedae": ("optional", "[1.21-2.2.35-neoforge]", "BOTH"),
    "advanced_ae": ("required", "[1.6.12-1.21.1]", "BOTH"),
    "ae2jeiintegration": ("required", "[1.2.1]", "CLIENT"),
}


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest().upper()


def report_path(path: Path) -> str:
    resolved = path.resolve()
    try:
        return resolved.relative_to(Path.cwd().resolve()).as_posix()
    except ValueError:
        return f"<external>/{resolved.name}"


def signature(name: str) -> bool:
    upper = name.upper()
    leaf = upper.rsplit("/", 1)[-1]
    return (
        upper == "META-INF/INDEX.LIST"
        or upper.startswith("META-INF/")
        and (leaf.startswith("SIG-") or upper.endswith((".SF", ".RSA", ".DSA", ".EC")))
    )


def manifest_mixins(raw: bytes) -> list[str]:
    text = re.sub(r"\r?\n ", "", raw.decode("utf-8"))
    match = re.search(r"(?m)^MixinConfigs:\s*(.+)$", text)
    return [] if match is None else [part.strip() for part in match.group(1).split(",") if part.strip()]


def read_toml(jar: zipfile.ZipFile) -> dict:
    for name in ("META-INF/neoforge.mods.toml", "META-INF/mods.toml"):
        try:
            return tomllib.loads(jar.read(name).decode("utf-8"))
        except KeyError:
            pass
    raise ValueError("mod metadata missing")


def inspect_jar(path: Path) -> tuple[dict, dict]:
    raw = path.read_bytes()
    with zipfile.ZipFile(io.BytesIO(raw)) as jar:
        broken = jar.testzip()
        if broken is not None:
            raise ValueError(f"CRC failure in {path}: {broken}")
        names = [info.filename for info in jar.infolist() if not info.is_dir()]
        exact_duplicates = sorted(name for name, count in Counter(names).items() if count > 1)
        folded: dict[str, list[str]] = {}
        for name in names:
            folded.setdefault(name.casefold(), []).append(name)
        case_duplicates = sorted(values for values in folded.values() if len(values) > 1)
        if exact_duplicates or case_duplicates:
            raise ValueError(f"duplicate paths in {path}: {exact_duplicates}, {case_duplicates}")
        signatures = sorted(name for name in names if signature(name))
        if signatures:
            raise ValueError(f"signature material in {path}: {signatures}")

        json_files = [name for name in names if name.lower().endswith(".json")]
        for name in json_files:
            json.loads(jar.read(name).decode("utf-8-sig"))

        metadata = read_toml(jar)
        mods = {entry["modId"]: entry.get("version") for entry in metadata.get("mods", [])}
        configs = manifest_mixins(jar.read("META-INF/MANIFEST.MF"))
        checked_mixins: list[str] = []
        for config_name in configs:
            if config_name not in names:
                raise ValueError(f"missing mixin config {config_name} in {path}")
            config = json.loads(jar.read(config_name).decode("utf-8"))
            package = config.get("package", "")
            for section in ("mixins", "client", "server"):
                for relative in config.get(section, []):
                    full_name = relative if relative.startswith(package + ".") else f"{package}.{relative}"
                    class_name = full_name.replace(".", "/") + ".class"
                    if class_name not in names:
                        raise ValueError(f"missing configured mixin class {class_name} in {path}")
                    checked_mixins.append(full_name)

        dependencies = metadata.get("dependencies", {})
        result = {
            "path": report_path(path),
            "length": len(raw),
            "sha256": sha256(raw),
            "entry_count": len(names),
            "zip_crc_test": "passed",
            "exact_duplicate_paths": 0,
            "case_insensitive_duplicate_paths": 0,
            "signature_entries": [],
            "json_files_parsed": len(json_files),
            "mods": mods,
            "manifest_mixin_configs": configs,
            "configured_mixin_classes_checked": len(checked_mixins),
            "license_entries": sorted(
                name for name in names if re.search(r"(?i)(license|copying|credits|notice)", name)
            ),
        }
        return result, {"metadata": metadata, "names": names, "jar_bytes": raw, "dependencies": dependencies}


def dependency_tuple(entry: dict) -> tuple[str, str, str]:
    return entry.get("type", "required"), entry["versionRange"], entry.get("side", "BOTH")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--gtna", required=True, type=Path)
    parser.add_argument("--pccard", required=True, type=Path)
    parser.add_argument("--merged", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()

    reports: dict[str, dict] = {}
    details: dict[str, dict] = {}
    for label, path in (("gtna", args.gtna), ("pccard", args.pccard), ("merged", args.merged)):
        reports[label], details[label] = inspect_jar(path)

    if set(reports["gtna"]["mods"]) != {"gtna"}:
        raise ValueError(f"unexpected standalone GTNA logical mods: {reports['gtna']['mods']}")
    if set(reports["pccard"]["mods"]) != {"pccard"}:
        raise ValueError(f"unexpected standalone PCCard logical mods: {reports['pccard']['mods']}")
    mod_annotation_descriptor = b"Lnet/neoforged/fml/common/Mod;"
    standalone_entry_points = {
        "gtna": ("com/raishxn/gtna/GTNAStandalone.class", b"gtna"),
        "pccard": ("yuuki1293/pccard/PCCardStandalone.class", b"pccard"),
    }
    for label, (entry_name, mod_id) in standalone_entry_points.items():
        if entry_name not in details[label]["names"]:
            raise ValueError(f"missing standalone entry point {entry_name} in {label}")
        with zipfile.ZipFile(io.BytesIO(details[label]["jar_bytes"])) as standalone:
            entry_bytes = standalone.read(entry_name)
        if mod_annotation_descriptor not in entry_bytes or mod_id not in entry_bytes:
            raise ValueError(f"invalid standalone @Mod entry point {entry_name}")

    actual_entries = details["merged"]["dependencies"].get("gtceu", [])
    dependency_ids = [entry["modId"] for entry in actual_entries]
    duplicate_dependencies = sorted(
        mod_id for mod_id, count in Counter(dependency_ids).items() if count > 1
    )
    if duplicate_dependencies:
        raise ValueError(f"duplicate gtceu dependency declarations: {duplicate_dependencies}")
    actual = {entry["modId"]: dependency_tuple(entry) for entry in actual_entries}
    mismatches = {
        mod_id: {"actual": actual.get(mod_id), "expected": expected}
        for mod_id, expected in EXPECTED_GTCEU_DEPENDENCIES.items()
        if actual.get(mod_id) != expected
    }
    if mismatches:
        raise ValueError(f"unexpected merged gtceu dependencies: {mismatches}")
    reports["merged"]["verified_dependencies"] = {
        dependency: list(actual[dependency]) for dependency in EXPECTED_GTCEU_DEPENDENCIES
    }
    reports["merged"]["gtceu_dependency_count"] = len(actual_entries)

    expected_mods = {"gtceu"}
    if set(reports["merged"]["mods"]) != expected_mods:
        raise ValueError(f"unexpected merged logical mods: {reports['merged']['mods']}")
    if "gtna" in details["merged"]["dependencies"] or "pccard" in details["merged"]["dependencies"]:
        raise ValueError("addon dependency tables must not create logical addon containers")
    expected_configs = [
        "gtceu.mixins.json",
        "gtna.mixins.json",
        "pccard.mixins.json",
        "gtceu-embedded-addons.mixins.json",
    ]
    if reports["merged"]["manifest_mixin_configs"] != expected_configs:
        raise ValueError("unexpected merged mixin config order")

    required_embedded_entries = {
        "com/raishxn/gtna/GTNACORE.class",
        "com/raishxn/gtna/GTNAGTAddon.class",
        "com/raishxn/gtna/GTNAGTAddonMerged.class",
        "yuuki1293/pccard/PCCard.class",
        "dev/codex/atm10merge/mixin/GTCEuMergedAddonsMixin.class",
        "gtceu-embedded-addons.mixins.json",
    }
    missing_embedded_entries = sorted(required_embedded_entries - set(details["merged"]["names"]))
    if missing_embedded_entries:
        raise ValueError(f"missing embedded feature/bootstrap entries: {missing_embedded_entries}")
    forbidden_standalone_entries = {
        "com/raishxn/gtna/GTNAStandalone.class",
        "yuuki1293/pccard/PCCardStandalone.class",
    }
    leaked_standalone_entries = sorted(forbidden_standalone_entries & set(details["merged"]["names"]))
    if leaked_standalone_entries:
        raise ValueError(f"standalone @Mod entry points leaked into merged jar: {leaked_standalone_entries}")
    gt_addon_annotation_descriptor = b"Lcom/gregtechceu/gtceu/api/addon/GTAddon;"
    with zipfile.ZipFile(io.BytesIO(details["merged"]["jar_bytes"])) as merged:
        annotated_embedded_cores = sorted(
            name for name in ("com/raishxn/gtna/GTNACORE.class", "yuuki1293/pccard/PCCard.class")
            if mod_annotation_descriptor in merged.read(name)
        )
        merged_gt_addon = merged.read("com/raishxn/gtna/GTNAGTAddonMerged.class")
        gtna_core = merged.read("com/raishxn/gtna/GTNACORE.class")
        gtna_client_proxy = merged.read("com/raishxn/gtna/client/ClientProxy.class")
    if annotated_embedded_cores:
        raise ValueError(f"embedded core classes still carry @Mod: {annotated_embedded_cores}")
    if gt_addon_annotation_descriptor not in merged_gt_addon or b"gtceu" not in merged_gt_addon:
        raise ValueError("merged GTNA GTAddon entry point is not bound to gtceu")
    if b"GTDynamicDataPack" not in gtna_core or b"GTDynamicResourcePack" not in gtna_client_proxy:
        raise ValueError("GTNA dynamic data/resource pack namespace bridge is missing")
    reports["merged"]["embedded_feature_modules"] = ["gtna", "pccard"]
    reports["merged"]["bootstrap_bridge"] = "dev.codex.atm10merge.mixin.GTCEuMergedAddonsMixin"
    reports["merged"]["gt_addon_entry_point"] = "com.raishxn.gtna.GTNAGTAddonMerged"

    nested_name = "META-INF/jarjar/ldlib-neoforge-1.21.1-1.0.41.jar"
    with zipfile.ZipFile(io.BytesIO(details["merged"]["jar_bytes"])) as merged:
        nested = merged.read(nested_name)
    with zipfile.ZipFile(io.BytesIO(nested)) as ldlib:
        ldlib_metadata = read_toml(ldlib)
        ldlib_mods = {entry["modId"]: entry.get("version") for entry in ldlib_metadata.get("mods", [])}
        if ldlib_mods.get("ldlib") != "1.0.41":
            raise ValueError(f"unexpected nested LDLib metadata: {ldlib_mods}")
        if ldlib.testzip() is not None:
            raise ValueError("nested LDLib CRC failure")
    reports["merged"]["nested_ldlib"] = {
        "entry": nested_name,
        "length": len(nested),
        "sha256": sha256(nested),
        "mods": ldlib_mods,
        "zip_crc_test": "passed",
    }

    final = {"status": "passed", "artifacts": reports}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(final, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(final, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
