from __future__ import annotations

import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = json.loads((ROOT / "data/generated/catalog.json").read_text(encoding="utf-8"))
REPORT = json.loads((ROOT / "data/generated/extraction-report.json").read_text(encoding="utf-8"))


class V02FeatureTests(unittest.TestCase):
    def test_all_nine_skills_have_structured_editorial(self) -> None:
        self.assertEqual(9, len(CATALOG["skills"]))
        deep = set()
        for skill in CATALOG["skills"]:
            editorial = skill["editorial"]["fr"]
            self.assertTrue(editorial["summary"], skill["id"])
            self.assertTrue(editorial["body_markdown"], skill["id"])
            self.assertTrue(editorial["key_systems"], skill["id"])
            self.assertTrue(editorial["xp_sources"], skill["id"])
            self.assertTrue(editorial["multiplayer"], skill["id"])
            self.assertIn(skill["coverage"], {"deep", "structured"})
            if skill["coverage"] == "deep":
                deep.add(skill["id"])
        self.assertEqual({"mining", "eating", "fishing", "fighting", "crafting", "traveling", "building", "farming", "woodcutting"}, deep)

    def test_progression_curve_and_reference_levels(self) -> None:
        progression = CATALOG["systems"]["progression"]
        self.assertEqual(100, progression["max_level"])
        self.assertEqual(10, progression["node_unlock_cost"])
        by_level = {entry["level"]: entry for entry in progression["levels"]}
        self.assertEqual(186, by_level[10]["cumulative"])
        self.assertEqual(2228, by_level[100]["cumulative"])
        self.assertEqual(0, by_level[100]["required"])

    def test_mining_specialized_data(self) -> None:
        mining = CATALOG["systems"]["mining"]
        self.assertEqual(5, len(mining["fossils"]["families"]))
        self.assertEqual(5, len(mining["fossils"]["rarities"]))
        self.assertEqual(10, mining["area_effects"]["vein_mining"]["max_extra_blocks"])
        self.assertTrue(mining["area_effects"]["mining_3x3"]["independent_toggle"])

    def test_eating_specialized_data(self) -> None:
        eating = CATALOG["systems"]["eating"]
        self.assertEqual(47, len(eating["cooking_recipes"]))
        self.assertEqual(49, len(eating["ingredients"]))
        self.assertEqual(1, len(eating["dynamic_ingredient_sources"]))
        self.assertEqual(50, REPORT["counts"]["culinary_ingredients"])
        self.assertEqual({"fixed", "generic"}, {recipe["kind"] for recipe in eating["cooking_recipes"]})

    def test_fishing_specialized_data(self) -> None:
        fishing = CATALOG["systems"]["fishing"]
        self.assertEqual(5, len(fishing["families"]))
        self.assertEqual(5, len(fishing["rarities"]))
        self.assertEqual(5, len(fishing["rarity_distributions"]))
        self.assertEqual(3, len(fishing["sea_monsters"]["types"]))
        for distribution in fishing["rarity_distributions"]:
            self.assertAlmostEqual(100.0, sum(distribution["percentages"].values()))
        games = {entry["rarity"]: entry["game"] for entry in fishing["mini_games"]}
        self.assertEqual("precision", games["epic"])
        self.assertEqual("cards", games["legendary"])
        self.assertEqual("grid", games["mythic"])
        self.assertEqual(3, fishing["inventories"]["fishing_boat_capacity"])

    def test_interactive_components_use_safe_dom_apis(self) -> None:
        components = [
            "XpExplorer.astro", "SkillTree.astro", "MiningExplorer.astro",
            "EatingExplorer.astro", "FishingExplorer.astro",
            "FightingExplorer.astro", "CraftingExplorer.astro",
            "RelatedContentExplorer.astro", "RecipeExplorer.astro",
        ]
        for filename in components:
            text = (ROOT / "website/src/components" / filename).read_text(encoding="utf-8")
            self.assertNotIn(".innerHTML", text, filename)
            self.assertTrue("addEventListener" in text or filename == "RelatedContentExplorer.astro", filename)

    def test_source_is_officially_src92(self) -> None:
        self.assertEqual("src(92).zip", CATALOG["source"]["canonical_source"])
        source_note = (ROOT / "mod-source/SOURCE.txt").read_text(encoding="utf-8")
        self.assertIn("source officielle : src(92).zip", source_note)


if __name__ == "__main__":
    unittest.main()
