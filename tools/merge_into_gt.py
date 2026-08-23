#!/usr/bin/env python3
"""Embed the two ported feature modules into the exact ATM10 GTCEu mod JAR.

The output exposes only GTCEu's logical mod container. GTNA and PCCard content
is bootstrapped by a required bridge Mixin, while LDLib 1.x remains a nested
JarJar dependency instead of being shaded into GTCEu.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import tempfile
import tomllib
import zipfile
from dataclasses import dataclass
from pathlib import Path


BASE_ONLY = {
    "META-INF/MANIFEST.MF",
    "META-INF/neoforge.mods.toml",
    "META-INF/mods.toml",
    "META-INF/accesstransformer.cfg",
    "META-INF/jarjar/metadata.json",
    "pack.mcmeta",
}

MERGED_LINE_FILES = {
    "kubejs.plugins.txt",
}

# These GT-owned resources are deliberate Nexus replacements. All other
# non-identical collisions fail closed so ATM10 fork resources cannot be
# overwritten accidentally.
ALLOWED_REPLACEMENTS = {
    "assets/gtceu/textures/item/material_sets/metallic/ingot_double.png",
    "assets/gtceu/textures/item/material_sets/metallic/ingot_double_overlay.png",
    "assets/gtceu/textures/item/material_sets/metallic/ingot_double_secondary.png",
    "gtceu_atm10_embedded/kubejs/client_scripts/codex_gtceu_jei_cleanup.js",
}

EXPECTED_KUBEJS_PROVIDERS = [
    "com.gregtechceu.gtceu.integration.kjs.GregTechKubeJSPlugin",
    "com.raishxn.gtna.integration.kubejs.GTNAKubeJSPlugin",
]

EXPECTED_MANIFEST_MIXIN_CONFIGS = [
    "gtceu.mixins.json",
    "gtna.mixins.json",
    "pccard.mixins.json",
    "gtceu-embedded-addons.mixins.json",
]

BRIDGE_ENTRIES = {
    "dev/codex/atm10merge/mixin/GTCEuMergedAddonsMixin.class",
    "gtceu-embedded-addons.mixins.json",
}

# A bridge built with the JDK ``jar`` tool normally carries this harmless
# generated manifest. It is input metadata, not bridge payload: the merged JAR
# must retain and amend GTCEu's own manifest instead.
BRIDGE_IGNORED_ENTRIES = {
    "META-INF/MANIFEST.MF",
}

EXPECTED_BRIDGE_MIXIN_CONFIG = {
    "required": True,
    "minVersion": "0.8",
    "package": "dev.codex.atm10merge.mixin",
    "compatibilityLevel": "JAVA_21",
    "mixins": ["GTCEuMergedAddonsMixin"],
    "injectors": {"defaultRequire": 1},
}

EMBEDDED_FEATURE_ENTRIES = {
    "com/raishxn/gtna/GTNACORE.class",
    "yuuki1293/pccard/PCCard.class",
}

STANDALONE_WRAPPERS = {
    "gtna": "com/raishxn/gtna/GTNAStandalone.class",
    "pccard": "yuuki1293/pccard/PCCardStandalone.class",
}

CONSOLIDATED_DEPENDENCIES = [
    {
        "modId": "neoforge",
        "type": "required",
        "versionRange": "[21.1.248]",
        "ordering": "NONE",
        "side": "BOTH",
    },
    {
        "modId": "minecraft",
        "type": "required",
        "versionRange": "[1.21.1]",
        "ordering": "NONE",
        "side": "BOTH",
    },
    {
        "modId": "ae2",
        "type": "required",
        "versionRange": "[19.2.17]",
        "ordering": "AFTER",
        "side": "BOTH",
    },
    {
        "modId": "guideme",
        "type": "required",
        "versionRange": "[21.1.17]",
        "ordering": "AFTER",
        "side": "BOTH",
    },
    {
        "modId": "ldlib",
        "type": "required",
        "versionRange": "[1.0.41]",
        "ordering": "AFTER",
        "side": "BOTH",
    },
    {
        "modId": "extendedae",
        "type": "optional",
        "versionRange": "[1.21-2.2.35-neoforge]",
        "ordering": "AFTER",
        "side": "BOTH",
    },
    {
        "modId": "advanced_ae",
        "type": "required",
        "versionRange": "[1.6.12-1.21.1]",
        "ordering": "AFTER",
        "side": "BOTH",
    },
    {
        "modId": "ae2jeiintegration",
        "type": "required",
        "versionRange": "[1.2.1]",
        "ordering": "AFTER",
        "side": "CLIENT",
    },
]

CONSOLIDATED_DEPENDENCY_IDS = {
    dependency["modId"] for dependency in CONSOLIDATED_DEPENDENCIES
}

MOD_ANNOTATION_DESCRIPTOR = b"Lnet/neoforged/fml/common/Mod;"


@dataclass
class Entry:
    info: zipfile.ZipInfo
    data: bytes


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest().upper()


def is_signature(name: str) -> bool:
    upper = name.upper()
    leaf = upper.rsplit("/", 1)[-1]
    return (
        upper == "META-INF/INDEX.LIST"
        or upper.startswith("META-INF/")
        and leaf.startswith("SIG-")
        or upper.startswith("META-INF/")
        and upper.endswith((".SF", ".RSA", ".DSA", ".EC"))
    )


def clone_info(source: zipfile.ZipInfo, name: str | None = None) -> zipfile.ZipInfo:
    result = zipfile.ZipInfo(name or source.filename, source.date_time)
    result.compress_type = source.compress_type
    result.comment = source.comment
    result.extra = source.extra
    result.create_system = source.create_system
    result.create_version = source.create_version
    result.extract_version = source.extract_version
    result.flag_bits = source.flag_bits
    result.internal_attr = source.internal_attr
    result.external_attr = source.external_attr
    return result


def read_entries(path: Path) -> dict[str, Entry]:
    result: dict[str, Entry] = {}
    with zipfile.ZipFile(path) as jar:
        for info in jar.infolist():
            if info.is_dir() or is_signature(info.filename):
                continue
            if info.filename in result:
                raise ValueError(f"duplicate entry in {path}: {info.filename}")
            result[info.filename] = Entry(clone_info(info), jar.read(info))
    return result


def read_mod_toml(entries: dict[str, Entry], jar: Path) -> str:
    for name in ("META-INF/neoforge.mods.toml", "META-INF/mods.toml"):
        entry = entries.get(name)
        if entry is not None:
            return entry.data.decode("utf-8")
    raise ValueError(f"{jar} has no NeoForge mod metadata")


def assert_single_logical_mod(text: str, expected_mod_id: str) -> None:
    metadata = tomllib.loads(text)
    mod_ids = [mod.get("modId") for mod in metadata.get("mods", [])]
    if mod_ids != [expected_mod_id]:
        raise ValueError(
            f"{expected_mod_id} input exposes unexpected logical mods: {mod_ids}"
        )


def dependency_blocks(text: str) -> list[tuple[int, int, str]]:
    header_pattern = re.compile(
        r"(?m)^[ \t]*(\[\[[^\]\r\n]+\]\]|\[[^\]\r\n]+\])[ \t]*(?:#.*)?$"
    )
    headers = list(header_pattern.finditer(text))
    blocks: list[tuple[int, int, str]] = []
    for index, header in enumerate(headers):
        raw_header = header.group(1)
        if raw_header != "[[dependencies.gtceu]]":
            continue
        end = len(text)
        for following in headers[index + 1 :]:
            following_header = following.group(1)
            if (
                following_header.startswith("[dependencies.gtceu.")
                and not following_header.startswith("[[")
            ):
                continue
            end = following.start()
            break
        block = text[header.start() : end]
        mod_ids = re.findall(
            r'(?m)^\s*modId\s*=\s*"([^"]+)"\s*(?:#.*)?$', block
        )
        if len(mod_ids) != 1:
            raise ValueError(f"malformed dependencies.gtceu block: {block!r}")
        blocks.append((header.start(), end, mod_ids[0]))
    return blocks


def render_dependency(dependency: dict[str, str]) -> str:
    return "\n".join(
        [
            "[[dependencies.gtceu]]",
            f'    modId = "{dependency["modId"]}"',
            f'    type = "{dependency["type"]}"',
            f'    versionRange = "{dependency["versionRange"]}"',
            f'    ordering = "{dependency["ordering"]}"',
            f'    side = "{dependency["side"]}"',
        ]
    )


def dependency_list(metadata: dict[str, object], owner: str) -> list[dict[str, object]]:
    dependencies = metadata.get("dependencies", {})
    if not isinstance(dependencies, dict):
        raise ValueError("TOML dependencies root is not a table")
    entries = dependencies.get(owner, [])
    if not isinstance(entries, list):
        raise ValueError(f"dependencies.{owner} is not an array of tables")
    return entries


def assert_unique_dependencies(entries: list[dict[str, object]], owner: str) -> None:
    mod_ids = [entry.get("modId") for entry in entries]
    duplicates = sorted(
        mod_id for mod_id in set(mod_ids) if mod_ids.count(mod_id) > 1
    )
    if duplicates:
        raise ValueError(f"duplicate dependencies.{owner} entries: {duplicates}")


def validate_consolidated_metadata(
    metadata: dict[str, object], preserved_dependencies: list[dict[str, object]]
) -> list[dict[str, object]]:
    mod_ids = [mod.get("modId") for mod in metadata.get("mods", [])]
    if mod_ids != ["gtceu"]:
        raise ValueError(f"merged metadata must expose only gtceu, found: {mod_ids}")

    dependency_owners = set(metadata.get("dependencies", {}))
    if dependency_owners != {"gtceu"}:
        raise ValueError(
            f"merged metadata has unexpected dependency owners: {sorted(dependency_owners)}"
        )

    entries = dependency_list(metadata, "gtceu")
    assert_unique_dependencies(entries, "gtceu")
    by_mod_id = {entry.get("modId"): entry for entry in entries}
    for expected in CONSOLIDATED_DEPENDENCIES:
        actual = by_mod_id.get(expected["modId"])
        if actual != expected:
            raise ValueError(
                f"unexpected consolidated dependency {expected['modId']}: "
                f"{actual}; expected {expected}"
            )

    actual_preserved = [
        entry for entry in entries if entry.get("modId") not in CONSOLIDATED_DEPENDENCY_IDS
    ]
    if actual_preserved != preserved_dependencies:
        raise ValueError("base dependency declarations changed during consolidation")
    return entries


def consolidate_base_toml(
    base_toml: str, extra_mixin_configs: list[str]
) -> tuple[str, list[dict[str, object]]]:
    base_metadata = tomllib.loads(base_toml)
    base_mod_ids = [mod.get("modId") for mod in base_metadata.get("mods", [])]
    if base_mod_ids != ["gtceu"]:
        raise ValueError(f"base JAR exposes unexpected logical mods: {base_mod_ids}")
    base_dependencies = dependency_list(base_metadata, "gtceu")
    assert_unique_dependencies(base_dependencies, "gtceu")
    preserved_dependencies = [
        entry
        for entry in base_dependencies
        if entry.get("modId") not in CONSOLIDATED_DEPENDENCY_IDS
    ]

    spans = [
        (start, end)
        for start, end, mod_id in dependency_blocks(base_toml)
        if mod_id in CONSOLIDATED_DEPENDENCY_IDS
    ]
    output: list[str] = []
    cursor = 0
    for start, end in spans:
        output.append(base_toml[cursor:start])
        cursor = end
    output.append(base_toml[cursor:])
    consolidated = "".join(output).rstrip()
    consolidated += "\n\n# ---- dependencies required by embedded GTNA/PCCard ----\n"
    consolidated += "\n\n".join(
        render_dependency(dependency) for dependency in CONSOLIDATED_DEPENDENCIES
    )

    declared_mixins = mixin_configs(consolidated)
    duplicate_mixins = sorted(
        config for config in set(declared_mixins) if declared_mixins.count(config) > 1
    )
    if duplicate_mixins:
        raise ValueError(f"base TOML has duplicate mixin declarations: {duplicate_mixins}")
    missing_mixins = [config for config in extra_mixin_configs if config not in declared_mixins]
    if missing_mixins:
        consolidated += "\n\n# ---- embedded feature/bootstrap mixins ----\n"
        consolidated += "\n".join(
            f'[[mixins]]\n    config = "{config}"\n' for config in missing_mixins
        ).rstrip()
    consolidated += "\n"

    output_metadata = tomllib.loads(consolidated)
    validate_consolidated_metadata(output_metadata, preserved_dependencies)
    output_mixins = mixin_configs(consolidated)
    for config in extra_mixin_configs:
        if output_mixins.count(config) != 1:
            raise ValueError(f"embedded mixin declaration count is not one: {config}")
    return consolidated, preserved_dependencies


def assert_default_access_transformers(text: str, mod_id: str) -> None:
    blocks = re.findall(
        r"(?ms)^\s*\[\[accessTransformers\]\](.*?)(?=^\s*\[\[|\Z)", text
    )
    for block in blocks:
        files = re.findall(r'(?m)^\s*file\s*=\s*"([^"]+)"\s*$', block)
        if files != ["META-INF/accesstransformer.cfg"]:
            raise ValueError(f"{mod_id} uses unsupported access transformer declarations: {files}")


def assert_no_nested_jars(entries: dict[str, Entry], mod_id: str) -> None:
    nested = sorted(name for name in entries if name.startswith("META-INF/jarjar/"))
    if not nested:
        return
    if nested == ["META-INF/jarjar/metadata.json"]:
        metadata = json.loads(entries[nested[0]].data.decode("utf-8"))
        if not metadata.get("jars"):
            return
    raise ValueError(f"{mod_id} unexpectedly contains nested JarJar inputs: {nested}")


def class_has_utf8(data: bytes, value: bytes) -> bool:
    if len(value) > 0xFFFF:
        raise ValueError("class UTF-8 probe is too long")
    return b"\x01" + len(value).to_bytes(2, "big") + value in data


def mod_entrypoint_classes(entries: dict[str, Entry], mod_id: str) -> list[str]:
    encoded_mod_id = mod_id.encode("utf-8")
    return sorted(
        name
        for name, entry in entries.items()
        if name.endswith(".class")
        and class_has_utf8(entry.data, MOD_ANNOTATION_DESCRIPTOR)
        and class_has_utf8(entry.data, encoded_mod_id)
    )


def assert_standalone_wrapper(
    entries: dict[str, Entry], mod_id: str, wrapper: str
) -> None:
    if wrapper not in entries:
        raise ValueError(f"{mod_id} standalone wrapper is missing: {wrapper}")
    entrypoints = mod_entrypoint_classes(entries, mod_id)
    if entrypoints != [wrapper]:
        raise ValueError(
            f"{mod_id} must expose exactly its standalone wrapper as @Mod entrypoint: "
            f"{entrypoints}"
        )


def assert_bridge_entries(entries: dict[str, Entry]) -> None:
    actual = set(entries) - BRIDGE_IGNORED_ENTRIES
    if actual != BRIDGE_ENTRIES:
        raise ValueError(
            f"bridge JAR payload entries changed: {sorted(actual)}; "
            f"expected {sorted(BRIDGE_ENTRIES)}"
        )
    config = json.loads(
        entries["gtceu-embedded-addons.mixins.json"].data.decode("utf-8")
    )
    if config != EXPECTED_BRIDGE_MIXIN_CONFIG:
        raise ValueError(
            f"unexpected bridge Mixin config: {config}; "
            f"expected {EXPECTED_BRIDGE_MIXIN_CONFIG}"
        )


def merge_unique_lines(first: bytes, second: bytes) -> bytes:
    output: list[str] = []
    seen: set[str] = set()
    for raw in (first, second):
        for line in raw.decode("utf-8").splitlines():
            value = line.strip()
            if value and value not in seen:
                seen.add(value)
                output.append(value)
    return ("\n".join(output) + "\n").encode("utf-8")


def license_collision_name(name: str, mod_id: str) -> str:
    leaf = Path(name).name
    return f"META-INF/licenses/{mod_id}/{leaf}"


def merge_addon_entries(
    merged: dict[str, Entry],
    addon: dict[str, Entry],
    mod_id: str,
    excluded_entries: set[str] | None = None,
) -> list[str]:
    excluded_entries = excluded_entries or set()
    replacements: list[str] = []
    for name, incoming in addon.items():
        if name in excluded_entries:
            continue
        if name in BASE_ONLY or name.startswith("META-INF/jarjar/"):
            continue
        existing = merged.get(name)
        if existing is None:
            merged[name] = incoming
            continue
        if existing.data == incoming.data:
            continue
        if name in MERGED_LINE_FILES or name.startswith("META-INF/services/"):
            existing.data = merge_unique_lines(existing.data, incoming.data)
            continue
        if name in ALLOWED_REPLACEMENTS:
            merged[name] = incoming
            replacements.append(name)
            continue
        if re.match(r"(?i)^(META-INF/)?(LICENSE|NOTICE|COPYING|CREDITS)", name):
            renamed = license_collision_name(name, mod_id)
            if renamed in merged and merged[renamed].data != incoming.data:
                raise ValueError(f"license collision after rename: {renamed}")
            merged[renamed] = Entry(clone_info(incoming.info, renamed), incoming.data)
            continue
        raise ValueError(f"unapproved non-identical collision from {mod_id}: {name}")
    return replacements


def merge_access_transformers(base: bytes, addons: list[tuple[str, dict[str, Entry]]]) -> bytes:
    chunks = [base.decode("utf-8").rstrip()]
    for mod_id, entries in addons:
        entry = entries.get("META-INF/accesstransformer.cfg")
        if entry is None or not entry.data.strip():
            continue
        chunks.append(f"# ---- merged from {mod_id} ----\n{entry.data.decode('utf-8').strip()}")
    return ("\n\n".join(chunk for chunk in chunks if chunk) + "\n").encode("utf-8")


def mixin_configs(toml: str) -> list[str]:
    return re.findall(r'(?m)^\s*config\s*=\s*"([^"]+\.mixins\.json)"\s*$', toml)


def manifest_mixin_configs(manifest: bytes) -> list[str]:
    text = manifest.decode("utf-8")
    unfolded = re.sub(r"\r?\n ", "", text)
    match = re.search(r"(?m)^MixinConfigs:\s*(.+)$", unfolded)
    if match is None:
        return []
    return [value.strip() for value in match.group(1).split(",") if value.strip()]


def merge_manifest(base: bytes, configs: list[str]) -> bytes:
    text = base.decode("utf-8").replace("\r\n", "\n")
    value = ",".join(dict.fromkeys(configs))
    prefix = "MixinConfigs: "
    remaining = value
    physical_lines: list[str] = []
    first_capacity = 70 - len(prefix.encode("utf-8"))
    physical_lines.append(prefix + remaining[:first_capacity])
    remaining = remaining[first_capacity:]
    while remaining:
        physical_lines.append(" " + remaining[:69])
        remaining = remaining[69:]
    replacement = "\n".join(physical_lines)
    if re.search(r"(?m)^MixinConfigs:.*(?:\n [^\n]*)*", text):
        text = re.sub(
            r"(?m)^MixinConfigs:.*(?:\n [^\n]*)*",
            replacement,
            text,
            count=1,
        )
    else:
        text = text.rstrip("\n") + "\n" + replacement + "\n"
    return text.replace("\n", "\r\n").encode("utf-8")


def add_nested_ldlib(merged: dict[str, Entry], ldlib: Path) -> None:
    metadata_name = "META-INF/jarjar/metadata.json"
    metadata = json.loads(merged[metadata_name].data.decode("utf-8"))
    nested_name = "META-INF/jarjar/ldlib-neoforge-1.21.1-1.0.41.jar"
    identifier = {
        "group": "com.lowdragmc.ldlib",
        "artifact": "ldlib-neoforge-1.21.1",
    }
    if any(
        jar.get("path") == nested_name or jar.get("identifier") == identifier
        for jar in metadata["jars"]
    ):
        raise ValueError("LDLib JarJar metadata already exists")
    metadata["jars"].append(
        {
            "identifier": identifier,
            "version": {"range": "[1.0.41,)", "artifactVersion": "1.0.41"},
            "path": nested_name,
            "isObfuscated": False,
        }
    )
    merged[metadata_name].data = (json.dumps(metadata, indent=2) + "\n").encode("utf-8")
    nested_data = ldlib.read_bytes()
    nested_info = zipfile.ZipInfo(nested_name, (2026, 8, 21, 0, 0, 0))
    nested_info.compress_type = zipfile.ZIP_STORED
    merged[nested_name] = Entry(nested_info, nested_data)


def write_jar(path: Path, entries: dict[str, Entry]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(dir=path.parent, suffix=".jar", delete=False) as tmp:
        temporary = Path(tmp.name)
    try:
        with zipfile.ZipFile(temporary, "w", allowZip64=True) as jar:
            names = sorted(entries)
            if "META-INF/MANIFEST.MF" in names:
                names.remove("META-INF/MANIFEST.MF")
                names.insert(0, "META-INF/MANIFEST.MF")
            for name in names:
                entry = entries[name]
                jar.writestr(clone_info(entry.info, name), entry.data)
        temporary.replace(path)
    finally:
        temporary.unlink(missing_ok=True)


def validate_output(
    path: Path,
    expected_configs: list[str],
    ldlib_sha256: str,
    preserved_dependencies: list[dict[str, object]],
    bridge_entry_hashes: dict[str, str],
) -> dict[str, object]:
    with zipfile.ZipFile(path) as jar:
        broken = jar.testzip()
        if broken is not None:
            raise ValueError(f"CRC failure in merged JAR entry: {broken}")
        names = [info.filename for info in jar.infolist() if not info.is_dir()]
        if len(names) != len(set(names)):
            raise ValueError("merged JAR contains duplicate entries")
        signatures = sorted(name for name in names if is_signature(name))
        if signatures:
            raise ValueError(f"merged JAR unexpectedly contains signatures: {signatures}")

        output_toml = jar.read("META-INF/neoforge.mods.toml").decode("utf-8")
        metadata = tomllib.loads(output_toml)
        mod_ids = [mod.get("modId") for mod in metadata.get("mods", [])]
        dependencies = validate_consolidated_metadata(metadata, preserved_dependencies)

        missing_embedded_entries = sorted(
            (EMBEDDED_FEATURE_ENTRIES | BRIDGE_ENTRIES) - set(names)
        )
        if missing_embedded_entries:
            raise ValueError(
                f"missing embedded feature/bootstrap entries: {missing_embedded_entries}"
            )
        leaked_wrappers = sorted(set(STANDALONE_WRAPPERS.values()) & set(names))
        if leaked_wrappers:
            raise ValueError(f"standalone @Mod wrappers leaked into output: {leaked_wrappers}")

        jar_entries = {
            name: Entry(clone_info(jar.getinfo(name)), jar.read(name))
            for name in names
            if name.endswith(".class")
        }
        dangling_entrypoints = {
            mod_id: mod_entrypoint_classes(jar_entries, mod_id)
            for mod_id in STANDALONE_WRAPPERS
        }
        dangling_entrypoints = {
            mod_id: classes
            for mod_id, classes in dangling_entrypoints.items()
            if classes
        }
        if dangling_entrypoints:
            raise ValueError(
                f"embedded feature @Mod entrypoints remain in output: {dangling_entrypoints}"
            )
        for name, expected_hash in bridge_entry_hashes.items():
            if sha256(jar.read(name)) != expected_hash:
                raise ValueError(f"bridge entry checksum mismatch: {name}")

        actual_configs = manifest_mixin_configs(jar.read("META-INF/MANIFEST.MF"))
        if actual_configs != expected_configs:
            raise ValueError(
                f"unexpected manifest mixins: {actual_configs}; expected {expected_configs}"
            )
        for config in actual_configs:
            if config not in names:
                raise ValueError(f"missing declared mixin config: {config}")
            json.loads(jar.read(config).decode("utf-8"))
        toml_configs = mixin_configs(output_toml)
        for config in expected_configs:
            if toml_configs.count(config) != 1:
                raise ValueError(
                    f"TOML mixin declaration count for {config} is {toml_configs.count(config)}"
                )

        kubejs_providers = [
            line.strip()
            for line in jar.read("kubejs.plugins.txt").decode("utf-8").splitlines()
            if line.strip()
        ]
        if kubejs_providers != EXPECTED_KUBEJS_PROVIDERS:
            raise ValueError(f"unexpected KubeJS providers: {kubejs_providers}")

        jarjar = json.loads(jar.read("META-INF/jarjar/metadata.json").decode("utf-8"))
        jarjar_entries = jarjar.get("jars", [])
        nested_paths = [entry["path"] for entry in jarjar_entries]
        nested_identifiers = [
            (entry["identifier"]["group"], entry["identifier"]["artifact"])
            for entry in jarjar_entries
        ]
        if len(nested_paths) != 4 or len(nested_paths) != len(set(nested_paths)):
            raise ValueError(f"unexpected JarJar paths: {nested_paths}")
        if len(nested_identifiers) != len(set(nested_identifiers)):
            raise ValueError(f"duplicate JarJar identifiers: {nested_identifiers}")
        for nested_path in nested_paths:
            if nested_path not in names:
                raise ValueError(f"missing nested JarJar payload: {nested_path}")
        nested_ldlib = "META-INF/jarjar/ldlib-neoforge-1.21.1-1.0.41.jar"
        if sha256(jar.read(nested_ldlib)) != ldlib_sha256:
            raise ValueError("nested LDLib checksum mismatch")

    return {
        "zip_crc_test": "passed",
        "logical_mod_ids": mod_ids,
        "gtceu_dependencies": dependencies,
        "embedded_feature_modules": ["gtna", "pccard"],
        "dangling_addon_mod_entrypoints": {},
        "manifest_mixin_configs": actual_configs,
        "kubejs_providers": kubejs_providers,
        "jarjar_paths": nested_paths,
        "jarjar_identifiers": nested_identifiers,
        "signature_entries": [],
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True, type=Path)
    parser.add_argument("--gtna", required=True, type=Path)
    parser.add_argument("--pccard", required=True, type=Path)
    parser.add_argument("--bridge", required=True, type=Path)
    parser.add_argument("--ldlib", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--manifest", required=True, type=Path)
    args = parser.parse_args()

    resolved_inputs = {
        "base": args.base.resolve(),
        "gtna": args.gtna.resolve(),
        "pccard": args.pccard.resolve(),
        "bridge": args.bridge.resolve(),
        "ldlib": args.ldlib.resolve(),
    }
    for path in resolved_inputs.values():
        if not path.is_file():
            raise FileNotFoundError(path)
    if args.output.resolve() in set(resolved_inputs.values()):
        raise ValueError("output must not overwrite any merge input")

    merged = read_entries(args.base)
    base_entry_hashes = {name: sha256(entry.data) for name, entry in merged.items()}
    addons = [
        ("gtna", args.gtna, read_entries(args.gtna)),
        ("pccard", args.pccard, read_entries(args.pccard)),
    ]
    bridge_entries = read_entries(args.bridge)
    base_toml = read_mod_toml(merged, args.base)
    addon_tomls = [(mod_id, read_mod_toml(entries, path)) for mod_id, path, entries in addons]
    assert_single_logical_mod(base_toml, "gtceu")
    for mod_id, toml in addon_tomls:
        assert_single_logical_mod(toml, mod_id)
        assert_default_access_transformers(toml, mod_id)
    for mod_id, _, entries in addons:
        assert_no_nested_jars(entries, mod_id)
        assert_standalone_wrapper(entries, mod_id, STANDALONE_WRAPPERS[mod_id])
    assert_no_nested_jars(bridge_entries, "bridge")
    assert_bridge_entries(bridge_entries)

    replacements: list[str] = []
    for mod_id, _, entries in addons:
        replacements.extend(
            merge_addon_entries(
                merged,
                entries,
                mod_id,
                {STANDALONE_WRAPPERS[mod_id]},
            )
        )
    bridge_replacements = merge_addon_entries(merged, bridge_entries, "bridge")
    if bridge_replacements:
        raise ValueError(f"bridge unexpectedly replaced base resources: {bridge_replacements}")
    if sorted(replacements) != sorted(ALLOWED_REPLACEMENTS):
        raise ValueError(
            "intentional GT replacement set changed: "
            f"{sorted(replacements)}; expected {sorted(ALLOWED_REPLACEMENTS)}"
        )

    merged["META-INF/accesstransformer.cfg"].data = merge_access_transformers(
        merged["META-INF/accesstransformer.cfg"].data,
        [(mod_id, entries) for mod_id, _, entries in addons],
    )

    configs = manifest_mixin_configs(merged["META-INF/MANIFEST.MF"].data)
    for mod_id, addon_path, entries in addons:
        manifest = entries.get("META-INF/MANIFEST.MF")
        if manifest is None:
            raise ValueError(f"{mod_id} has no manifest")
        addon_configs = manifest_mixin_configs(manifest.data)
        declared_configs = mixin_configs(read_mod_toml(entries, addon_path))
        expected_addon_config = f"{mod_id}.mixins.json"
        if addon_configs != [expected_addon_config]:
            raise ValueError(f"unexpected {mod_id} manifest mixins: {addon_configs}")
        if declared_configs.count(expected_addon_config) != 1:
            raise ValueError(
                f"{mod_id} TOML does not declare exactly one {expected_addon_config}"
            )
        configs.extend(addon_configs)
    configs.append("gtceu-embedded-addons.mixins.json")
    if configs != EXPECTED_MANIFEST_MIXIN_CONFIGS:
        raise ValueError(
            f"unexpected merged Mixin config order: {configs}; "
            f"expected {EXPECTED_MANIFEST_MIXIN_CONFIGS}"
        )

    combined_toml, preserved_dependencies = consolidate_base_toml(
        base_toml, EXPECTED_MANIFEST_MIXIN_CONFIGS[1:]
    )
    merged["META-INF/neoforge.mods.toml"].data = combined_toml.encode("utf-8")
    merged["META-INF/MANIFEST.MF"].data = merge_manifest(
        merged["META-INF/MANIFEST.MF"].data, configs
    )
    add_nested_ldlib(merged, args.ldlib)

    missing_base_entries = sorted(set(base_entry_hashes) - set(merged))
    if missing_base_entries:
        raise ValueError(f"base entries disappeared during merge: {missing_base_entries}")
    changed_base_entries = sorted(
        name
        for name, digest in base_entry_hashes.items()
        if sha256(merged[name].data) != digest
    )
    permitted_base_changes = {
        "META-INF/MANIFEST.MF",
        "META-INF/neoforge.mods.toml",
        "META-INF/accesstransformer.cfg",
        "META-INF/jarjar/metadata.json",
        *MERGED_LINE_FILES,
        *ALLOWED_REPLACEMENTS,
    }
    permitted_base_changes.update(
        name for name in changed_base_entries if name.startswith("META-INF/services/")
    )
    unexpected_base_changes = sorted(set(changed_base_entries) - permitted_base_changes)
    if unexpected_base_changes:
        raise ValueError(f"unexpected mutations to base entries: {unexpected_base_changes}")

    nested_ldlib_sha256 = sha256(args.ldlib.read_bytes())
    bridge_entry_hashes = {
        name: sha256(bridge_entries[name].data) for name in sorted(BRIDGE_ENTRIES)
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(
        dir=args.output.parent, prefix=f".{args.output.name}.", suffix=".staging.jar", delete=False
    ) as tmp:
        staged_output = Path(tmp.name)
    try:
        write_jar(staged_output, merged)
        validation = validate_output(
            staged_output,
            EXPECTED_MANIFEST_MIXIN_CONFIGS,
            nested_ldlib_sha256,
            preserved_dependencies,
            bridge_entry_hashes,
        )
        staged_output.replace(args.output)
    finally:
        staged_output.unlink(missing_ok=True)

    report = {
        "resolved_inputs": {
            label: str(path) for label, path in resolved_inputs.items()
        },
        "base": str(args.base.resolve()),
        "base_sha256": sha256(args.base.read_bytes()),
        "gtna": str(args.gtna.resolve()),
        "gtna_sha256": sha256(args.gtna.read_bytes()),
        "pccard": str(args.pccard.resolve()),
        "pccard_sha256": sha256(args.pccard.read_bytes()),
        "bridge": str(args.bridge.resolve()),
        "bridge_sha256": sha256(args.bridge.read_bytes()),
        "bridge_entries": bridge_entry_hashes,
        "nested_ldlib": str(args.ldlib.resolve()),
        "nested_ldlib_sha256": nested_ldlib_sha256,
        "output": str(args.output.resolve()),
        "output_sha256": sha256(args.output.read_bytes()),
        "logical_mods": ["gtceu", "ldlib"],
        "embedded_feature_modules": ["gtna", "pccard"],
        "intentional_gt_resource_replacements": sorted(set(replacements)),
        "changed_base_entries": changed_base_entries,
        "preserved_base_entry_count": len(base_entry_hashes) - len(changed_base_entries),
        "added_entry_count": len(merged) - len(base_entry_hashes),
        "entry_count": len(merged),
        "output_signed": False,
        "validation": validation,
    }
    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    args.manifest.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
