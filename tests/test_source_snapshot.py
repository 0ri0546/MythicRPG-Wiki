from __future__ import annotations

import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SNAPSHOT = json.loads((ROOT / "config/source_snapshot.json").read_text(encoding="utf-8"))
CATALOG = json.loads((ROOT / "data/generated/catalog.json").read_text(encoding="utf-8"))
REPORT = json.loads((ROOT / "data/generated/extraction-report.json").read_text(encoding="utf-8"))


class SourceSnapshotTests(unittest.TestCase):
    def test_source_identity_matches_declared_snapshot(self) -> None:
        self.assertEqual(SNAPSHOT["tree_sha256"], CATALOG["source"]["tree_sha256"])
        self.assertEqual("src(91).zip", SNAPSHOT["canonical_source"])
        self.assertEqual("src(92).zip", SNAPSHOT["received_archive"])

    def test_counts_match_version_snapshot(self) -> None:
        expected = SNAPSHOT["counts"]
        actual = REPORT["counts"]
        for key in (
            "skills",
            "perks",
            "items",
            "blocks",
            "recipes",
            "documented_values",
            "search_entries",
            "items_confirmed",
            "items_dynamic_probable",
            "items_model_only",
        ):
            self.assertEqual(expected[key], actual[key], key)
        self.assertEqual(expected["translations_fr_fr"], CATALOG["locales"]["fr_fr"])
        self.assertEqual(expected["translations_en_us"], CATALOG["locales"]["en_us"])
        source_files = sum(1 for path in (ROOT / "mod-source/src").rglob("*") if path.is_file())
        self.assertEqual(expected["source_files"], source_files)

    def test_tracked_values_match_version_snapshot(self) -> None:
        actual = {entry["id"]: entry["value"] for entry in CATALOG["values"]}
        self.assertEqual(SNAPSHOT["tracked_values"], actual)


if __name__ == "__main__":
    unittest.main()
