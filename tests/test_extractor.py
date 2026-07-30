from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))

from mythicwiki.build import build_catalog
from mythicwiki.utils import tree_sha256

SOURCE = ROOT / "mod-source/src"


class ExtractorInvariantTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.source_hash_before = tree_sha256(SOURCE)
        cls.report = build_catalog(ROOT, SOURCE)
        cls.source_hash_after = tree_sha256(SOURCE)
        cls.catalog = json.loads((ROOT / "data/generated/catalog.json").read_text(encoding="utf-8"))
        cls.encyclopedia = json.loads((ROOT / "data/generated/encyclopedia.json").read_text(encoding="utf-8"))

    def test_source_is_not_modified(self) -> None:
        self.assertEqual(self.source_hash_before, self.source_hash_after)

    def test_report_is_clean(self) -> None:
        self.assertEqual("ok", self.report["status"])
        self.assertEqual([], self.report["errors"])
        self.assertTrue(self.report["static_inspection_only"])

    def test_project_invariants(self) -> None:
        counts = self.report["counts"]
        self.assertEqual(9, counts["skills"])
        self.assertEqual(180, counts["perks"])
        self.assertGreater(counts["items"], 0)
        self.assertGreater(counts["recipes"], 0)
        classified_items = (
            counts["items_confirmed"]
            + counts["items_dynamic_probable"]
            + counts["items_model_only"]
        )
        self.assertEqual(counts["items"], classified_items)

    def test_every_skill_has_twenty_unique_nodes(self) -> None:
        all_slugs: set[str] = set()
        for skill in self.catalog["skills"]:
            ids = [node["id"] for node in skill["nodes"]]
            self.assertEqual(list(range(1, 21)), ids, skill["id"])
            slugs = {node["slug"] for node in skill["nodes"]}
            self.assertEqual(20, len(slugs), skill["id"])
            self.assertTrue(all_slugs.isdisjoint(slugs), skill["id"])
            all_slugs.update(slugs)

    def test_documented_values_are_sourced_not_hardcoded_in_pages(self) -> None:
        expected_ids = {
            "progression.xp_curve_coefficient",
            "progression.xp_curve_exponent",
            "eating.vanilla_food_xp",
            "eating.vanilla_soup_xp",
            "eating.cake_slice_xp",
            "mining.vein_mining_max_blocks",
        }
        values = {entry["id"]: entry for entry in self.catalog["values"]}
        self.assertTrue(expected_ids.issubset(values))
        for value_id in expected_ids:
            source = values[value_id]["source"]
            self.assertTrue(source["file"].endswith(".java"), value_id)
            self.assertTrue(source["symbol"], value_id)
            self.assertIsInstance(values[value_id]["value"], (int, float, bool, str))

    def test_translations_are_symmetric(self) -> None:
        self.assertEqual(self.catalog["locales"]["fr_fr"], self.catalog["locales"]["en_us"])
        self.assertGreater(self.catalog["locales"]["fr_fr"], 0)

    def test_item_evidence_statuses_are_consistent(self) -> None:
        allowed = {"confirmed", "dynamic_probable", "model_only"}
        for item in self.catalog["items"]:
            self.assertIn(item["registration_status"], allowed)
            if item["registration_status"] == "confirmed":
                self.assertTrue(item["registration_evidence"])
            elif item["registration_status"] == "dynamic_probable":
                self.assertFalse(item["registration_evidence"])
                self.assertTrue(item["evidence"]["recipe_reference"])
            else:
                self.assertFalse(item["registration_evidence"])
                self.assertFalse(item["evidence"]["recipe_reference"])

    def test_no_broken_internal_relations(self) -> None:
        item_ids = {item["id"] for item in self.catalog["items"]}
        for recipe in self.catalog["recipes"]:
            result = recipe["result"]["id"]
            if result.startswith("mythicrpg:"):
                self.assertIn(result.split(":", 1)[1], item_ids, recipe["id"])
        for skill in self.catalog["skills"]:
            node_ids = {node["id"] for node in skill["nodes"]}
            for node in skill["nodes"]:
                self.assertTrue(set(node["parent_ids"]).issubset(node_ids), node["slug"])

    def test_no_environment_path_leaks(self) -> None:
        serialized = json.dumps(self.catalog, ensure_ascii=False)
        self.assertNotIn("/mnt/data/", serialized)
        self.assertNotIn("wiki_v011_work", serialized)

    def test_encyclopedia_uses_shared_editorial_and_filtered_values(self) -> None:
        self.assertEqual(9, len(self.encyclopedia["skills"]))
        value_ids = {value["id"] for value in self.encyclopedia["values"]}
        self.assertIn("eating.vanilla_food_xp", value_ids)
        self.assertNotIn("progression.xp_curve_coefficient", value_ids)
        mining = next(skill for skill in self.encyclopedia["skills"] if skill["id"] == "mining")
        self.assertTrue(mining["summary"]["fr"])


if __name__ == "__main__":
    unittest.main()
