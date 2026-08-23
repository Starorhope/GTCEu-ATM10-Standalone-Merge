#!/usr/bin/env python3
"""Verify the generated metadata for every independently buildable project."""

from __future__ import annotations

import argparse
import tomllib
from pathlib import Path


EXPECTED_NEOFORGE_RANGE = "[21.1.240,)"


def neoforge_ranges(path: Path) -> list[str]:
    metadata = tomllib.loads(path.read_text(encoding="utf-8"))
    dependency_groups = metadata.get("dependencies", {})
    return [
        dependency.get("versionRange", "")
        for dependencies in dependency_groups.values()
        for dependency in dependencies
        if dependency.get("modId") == "neoforge"
    ]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("metadata", nargs="+", type=Path)
    args = parser.parse_args()

    failures: list[str] = []
    for path in args.metadata:
        ranges = neoforge_ranges(path)
        if ranges != [EXPECTED_NEOFORGE_RANGE]:
            failures.append(f"{path}: expected {EXPECTED_NEOFORGE_RANGE}, found {ranges}")
        else:
            print(f"{path}: NeoForge {EXPECTED_NEOFORGE_RANGE}")

    if failures:
        raise SystemExit("\n".join(failures))


if __name__ == "__main__":
    main()
