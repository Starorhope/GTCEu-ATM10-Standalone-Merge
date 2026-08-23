#!/usr/bin/env python3
"""Audit addon translations before and after embedding them into GTCEu.

The audit is intentionally strict about things that can break rendering at
runtime: every English key must have a Chinese value, printf-style format
arguments must remain identical, and the merged JAR must contain the exact
same language resources as the two addon JARs.
"""

from __future__ import annotations

import argparse
import collections
import json
import re
import sys
import zipfile
from pathlib import Path
from typing import Any


# Minecraft language values use Java Formatter placeholders. Capture enough
# structure to resolve implicit, explicit and '<' argument indices, including
# the two-character date/time conversions such as %1$tY.
FORMAT_TOKEN = re.compile(
    r"%(?:(?P<index>\d+)\$)?"
    r"(?P<flags>[-#+ 0,(<]*)"
    r"(?P<width>\d+)?"
    r"(?P<precision>\.\d+)?"
    r"(?P<date>[tT])?"
    r"(?P<conversion>[a-zA-Z%])"
)
ORDINARY_CONVERSIONS = frozenset("bBhHsScCdoxXeEfgGaAn%")
DATE_CONVERSIONS = frozenset("HIklMSLNpzZsQBbhAaCYyjmdeRTrDFc")
COLOR_CODE = re.compile(r"§[0-9A-FK-ORa-fk-or]")
HAN = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff]")


def load_json_member(jar: Path, member: str) -> tuple[dict[str, Any], bytes]:
    with zipfile.ZipFile(jar) as archive:
        try:
            payload = archive.read(member)
        except KeyError as exc:
            raise ValueError(f"{jar}: missing {member}") from exc
    try:
        parsed = json.loads(payload.decode("utf-8-sig"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ValueError(f"{jar}!/{member}: invalid UTF-8 JSON: {exc}") from exc
    if not isinstance(parsed, dict):
        raise ValueError(f"{jar}!/{member}: language root must be an object")
    return parsed, payload


def load_json_file(path: Path) -> dict[str, Any]:
    try:
        parsed = json.loads(path.read_text(encoding="utf-8-sig"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ValueError(f"{path}: invalid UTF-8 JSON: {exc}") from exc
    if not isinstance(parsed, dict):
        raise ValueError(f"{path}: language root must be an object")
    return parsed


def load_language_members(jar: Path, namespace: str) -> tuple[dict[str, bytes], list[str]]:
    prefix = f"assets/{namespace}/lang/"
    members: dict[str, bytes] = {}
    counts: collections.Counter[str] = collections.Counter()
    with zipfile.ZipFile(jar) as archive:
        for info in archive.infolist():
            if info.is_dir() or not info.filename.startswith(prefix) or not info.filename.endswith(".json"):
                continue
            counts[info.filename] += 1
            payload = archive.read(info)
            try:
                parsed = json.loads(payload.decode("utf-8-sig"))
            except (UnicodeDecodeError, json.JSONDecodeError) as exc:
                raise ValueError(f"{jar}!/{info.filename}: invalid UTF-8 JSON: {exc}") from exc
            if not isinstance(parsed, dict):
                raise ValueError(f"{jar}!/{info.filename}: language root must be an object")
            members[info.filename] = payload
    duplicates = sorted(name for name, count in counts.items() if count != 1)
    return members, duplicates


def placeholder_signature(value: str) -> collections.Counter[str]:
    tokens: list[str] = []
    next_implicit_index = 1
    previous_index: int | None = None
    for match in FORMAT_TOKEN.finditer(value):
        raw = match.group(0)
        explicit_index = match.group("index")
        flags = match.group("flags") or ""
        width = match.group("width") or ""
        precision = match.group("precision") or ""
        date_prefix = match.group("date") or ""
        conversion = match.group("conversion")

        # Do not mistake prose such as "250% faster" for a legal `% f`
        # placeholder. A bare space flag is too ambiguous in language text.
        if (
            flags == " "
            and explicit_index is None
            and not width
            and not precision
            and not date_prefix
        ):
            continue
        if date_prefix:
            if conversion not in DATE_CONVERSIONS:
                continue
        elif conversion not in ORDINARY_CONVERSIONS:
            continue

        if not date_prefix and conversion in {"%", "n"}:
            tokens.append(f"noarg:{conversion}")
            continue

        if "<" in flags:
            if previous_index is None:
                tokens.append(f"invalid-reuse:{raw}")
                continue
            argument_index = previous_index
        elif explicit_index is not None:
            argument_index = int(explicit_index)
        else:
            argument_index = next_implicit_index
            next_implicit_index += 1
        previous_index = argument_index

        normalized_flags = "".join(sorted(flags.replace("<", "")))
        normalized_conversion = date_prefix + conversion
        tokens.append(
            f"arg:{argument_index};flags:{normalized_flags};width:{width};"
            f"precision:{precision};conversion:{normalized_conversion}"
        )
    return collections.Counter(tokens)


def audit_namespace(
    addon_jar: Path,
    merged_jar: Path,
    namespace: str,
    expected_english: dict[str, Any] | None = None,
) -> tuple[dict[str, Any], list[str]]:
    en_member = f"assets/{namespace}/lang/en_us.json"
    zh_member = f"assets/{namespace}/lang/zh_cn.json"
    english, english_bytes = load_json_member(addon_jar, en_member)
    chinese, chinese_bytes = load_json_member(addon_jar, zh_member)
    merged_english, merged_english_bytes = load_json_member(merged_jar, en_member)
    merged_chinese, merged_chinese_bytes = load_json_member(merged_jar, zh_member)
    addon_language_members, addon_language_duplicates = load_language_members(addon_jar, namespace)
    merged_language_members, merged_language_duplicates = load_language_members(merged_jar, namespace)

    english_keys = set(english)
    chinese_keys = set(chinese)
    missing = sorted(english_keys - chinese_keys)
    extra = sorted(chinese_keys - english_keys)
    blank = sorted(
        key
        for key in english_keys & chinese_keys
        if not isinstance(chinese[key], str) or not chinese[key].strip()
    )
    non_string_english = sorted(key for key, value in english.items() if not isinstance(value, str))
    placeholder_mismatches: list[dict[str, Any]] = []
    color_code_mismatches: list[dict[str, Any]] = []
    newline_mismatches: list[dict[str, Any]] = []
    ascii_only_values: list[dict[str, str]] = []
    unchanged_values: list[dict[str, str]] = []

    for key in sorted(english_keys & chinese_keys):
        en_value = english[key]
        zh_value = chinese[key]
        if not isinstance(en_value, str) or not isinstance(zh_value, str):
            continue
        en_signature = placeholder_signature(en_value)
        zh_signature = placeholder_signature(zh_value)
        if en_signature != zh_signature:
            placeholder_mismatches.append(
                {
                    "key": key,
                    "english": en_value,
                    "chinese": zh_value,
                    "englishPlaceholders": dict(en_signature),
                    "chinesePlaceholders": dict(zh_signature),
                }
            )
        en_colors = [token.lower() for token in COLOR_CODE.findall(en_value)]
        zh_colors = [token.lower() for token in COLOR_CODE.findall(zh_value)]
        if en_colors != zh_colors:
            color_code_mismatches.append(
                {"key": key, "englishCodes": en_colors, "chineseCodes": zh_colors}
            )
        if en_value.count("\n") != zh_value.count("\n"):
            newline_mismatches.append(
                {
                    "key": key,
                    "englishNewlines": en_value.count("\n"),
                    "chineseNewlines": zh_value.count("\n"),
                }
            )
        if en_value.strip() and zh_value.strip() and not HAN.search(zh_value):
            ascii_only_values.append({"key": key, "value": zh_value})
        if en_value == zh_value:
            unchanged_values.append({"key": key, "value": zh_value})

    addon_member_names = set(addon_language_members)
    merged_member_names = set(merged_language_members)
    missing_merged_members = sorted(addon_member_names - merged_member_names)
    extra_merged_members = sorted(merged_member_names - addon_member_names)
    changed_merged_members = sorted(
        name
        for name in addon_member_names & merged_member_names
        if addon_language_members[name] != merged_language_members[name]
    )
    semantic_identity = {
        "en_us": english == merged_english,
        "zh_cn": chinese == merged_chinese,
    }

    expected_english_report: dict[str, Any] | None = None
    if expected_english is not None:
        expected_keys = set(expected_english)
        value_mismatches = sorted(
            key
            for key in english_keys & expected_keys
            if english[key] != expected_english[key]
        )
        expected_english_report = {
            "expectedKeyCount": len(expected_english),
            "missingKeys": sorted(expected_keys - english_keys),
            "extraKeys": sorted(english_keys - expected_keys),
            "valueMismatchKeys": value_mismatches,
            "semanticIdentity": english == expected_english,
        }

    errors: list[str] = []
    if missing:
        errors.append(f"{namespace}: {len(missing)} English keys have no zh_cn entry")
    if extra:
        errors.append(f"{namespace}: {len(extra)} zh_cn keys have no en_us entry")
    if blank:
        errors.append(f"{namespace}: {len(blank)} zh_cn values are blank or non-string")
    if non_string_english:
        errors.append(f"{namespace}: {len(non_string_english)} en_us values are non-string")
    if placeholder_mismatches:
        errors.append(f"{namespace}: {len(placeholder_mismatches)} placeholder signatures differ")
    if color_code_mismatches:
        errors.append(f"{namespace}: {len(color_code_mismatches)} formatting-code sequences differ")
    if newline_mismatches:
        errors.append(f"{namespace}: {len(newline_mismatches)} newline counts differ")
    if addon_language_duplicates:
        errors.append(f"{namespace}: standalone addon JAR has duplicate language members")
    if merged_language_duplicates:
        errors.append(f"{namespace}: merged JAR has duplicate language members")
    if missing_merged_members or extra_merged_members or changed_merged_members:
        errors.append(f"{namespace}: merged JAR language resource set or bytes differ")
    if not all(semantic_identity.values()):
        errors.append(f"{namespace}: merged JAR language resources are not semantically identical")
    if expected_english_report is not None and not expected_english_report["semanticIdentity"]:
        errors.append(f"{namespace}: built en_us does not equal the expected source union")

    report = {
        "namespace": namespace,
        "addonJar": str(addon_jar.resolve()),
        "englishKeyCount": len(english),
        "chineseKeyCount": len(chinese),
        "missingKeys": missing,
        "extraKeys": extra,
        "blankKeys": blank,
        "nonStringEnglishKeys": non_string_english,
        "placeholderMismatches": placeholder_mismatches,
        "colorCodeMismatches": color_code_mismatches,
        "newlineMismatches": newline_mismatches,
        # Advisory only: proper nouns, tier labels and acronyms can legitimately
        # contain no Han characters, so a human style pass owns this decision.
        "asciiOnlyChineseValues": ascii_only_values,
        "unchangedChineseValues": unchanged_values,
        "addonLanguageMembers": sorted(addon_language_members),
        "addonLanguageDuplicateMembers": addon_language_duplicates,
        "mergedLanguageMembers": sorted(merged_language_members),
        "mergedLanguageDuplicateMembers": merged_language_duplicates,
        "missingMergedLanguageMembers": missing_merged_members,
        "extraMergedLanguageMembers": extra_merged_members,
        "changedMergedLanguageMembers": changed_merged_members,
        "allLanguageMembersByteIdentical": not (
            missing_merged_members or extra_merged_members or changed_merged_members
        ),
        "mergedSemanticIdentity": semantic_identity,
        "mergedEnglishKeyCount": len(merged_english),
        "mergedChineseKeyCount": len(merged_chinese),
        "expectedEnglish": expected_english_report,
        "errors": errors,
    }
    return report, errors


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--gtna-jar", required=True, type=Path)
    parser.add_argument("--pccard-jar", required=True, type=Path)
    parser.add_argument("--gtna-main-en-us", required=True, type=Path)
    parser.add_argument("--gtna-generated-en-us", required=True, type=Path)
    parser.add_argument("--merged-jar", required=True, type=Path)
    parser.add_argument("--report", required=True, type=Path)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    inputs = [
        args.gtna_jar,
        args.pccard_jar,
        args.gtna_main_en_us,
        args.gtna_generated_en_us,
        args.merged_jar,
    ]
    absent = [str(path) for path in inputs if not path.is_file()]
    if absent:
        print("Missing JAR(s): " + ", ".join(absent), file=sys.stderr)
        return 2

    namespaces: list[dict[str, Any]] = []
    errors: list[str] = []
    try:
        expected_gtna_english = load_json_file(args.gtna_main_en_us)
        expected_gtna_english.update(load_json_file(args.gtna_generated_en_us))
        for addon_jar, namespace, expected_english in (
            (args.gtna_jar, "gtna", expected_gtna_english),
            (args.pccard_jar, "pccard", None),
        ):
            namespace_report, namespace_errors = audit_namespace(
                addon_jar, args.merged_jar, namespace, expected_english
            )
            namespaces.append(namespace_report)
            errors.extend(namespace_errors)
    except (OSError, ValueError, zipfile.BadZipFile) as exc:
        errors.append(str(exc))

    report = {
        "status": "PASS" if not errors else "FAIL",
        "mergedJar": str(args.merged_jar.resolve()),
        "namespaces": namespaces,
        "errors": errors,
    }
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if not errors else 1


if __name__ == "__main__":
    raise SystemExit(main())
