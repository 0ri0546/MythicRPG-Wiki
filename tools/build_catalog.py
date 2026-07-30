#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = SCRIPT_DIR.parent
sys.path.insert(0, str(SCRIPT_DIR))

from mythicwiki.build import build_catalog  # noqa: E402


def main() -> int:
    parser = argparse.ArgumentParser(description="Extrait statiquement les données MythicRPG et construit le catalogue du wiki.")
    parser.add_argument("--source", type=Path, default=PROJECT_ROOT / "mod-source/src", help="Dossier src du mod")
    parser.add_argument("--project-root", type=Path, default=PROJECT_ROOT)
    args = parser.parse_args()
    source = args.source.resolve()
    project_root = args.project_root.resolve()
    if not (source / "main/java").is_dir() or not (source / "main/resources").is_dir():
        parser.error(f"Source invalide: {source} doit contenir main/java et main/resources")
    report = build_catalog(project_root, source)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if report["status"] == "ok" else 1


if __name__ == "__main__":
    raise SystemExit(main())
