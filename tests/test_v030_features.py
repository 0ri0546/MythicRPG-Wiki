from __future__ import annotations

import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = json.loads((ROOT / "data/generated/catalog.json").read_text(encoding="utf-8"))
REPORT = json.loads((ROOT / "data/generated/extraction-report.json").read_text(encoding="utf-8"))
SNAPSHOT = json.loads((ROOT / "config/source_snapshot.json").read_text(encoding="utf-8"))


def skill(skill_id: str) -> dict:
    return next(entry for entry in CATALOG["skills"] if entry["id"] == skill_id)


class V030FeatureTests(unittest.TestCase):
    def test_fighting_is_deep_and_keeps_complete_tree(self) -> None:
        fighting_skill = skill("fighting")
        self.assertEqual("deep", fighting_skill["coverage"])
        self.assertEqual(list(range(1, 21)), [node["id"] for node in fighting_skill["nodes"]])
        self.assertTrue(fighting_skill["editorial"]["fr"]["xp_sources"])
        self.assertIn("Barons", fighting_skill["editorial"]["fr"]["body_markdown"])

    def test_fighting_xp_formulas_and_sources(self) -> None:
        fighting = CATALOG["systems"]["fighting"]
        xp = fighting["xp"]
        self.assertEqual(5.0, xp["health_divisor"])
        self.assertEqual((1, 20), (xp["normal_min"], xp["normal_max"]))
        self.assertEqual(1.5, xp["baron_base_multiplier"])
        self.assertEqual(5, xp["baron_flat_bonus"])
        self.assertTrue(xp["source_file"].endswith("FightingEvents.java"))
        self.assertIn("max_health", xp["normal_formula"])
        self.assertIn("baron_xp_multiplier", xp["baron_formula"])

    def test_complete_baron_catalog_and_scaling(self) -> None:
        barons = CATALOG["systems"]["fighting"]["barons"]
        types = barons["types"]
        self.assertEqual(25, len(types))
        self.assertEqual(25, len({entry["id"] for entry in types}))
        self.assertTrue(all(entry["names"]["fr"] and entry["behavior"]["file"] for entry in types))
        self.assertTrue(all(entry["base_entities"] for entry in types))
        tiers = barons["promotion"]["chance_tiers"]
        self.assertEqual([(0, 9), (10, 24), (25, 49), (50, 74), (75, 100)], [(tier["min_level"], tier["max_level"]) for tier in tiers])
        self.assertEqual([0.05, 0.2, 0.3, 0.5, 0.7], [tier["chance"] for tier in tiers])
        samples = {entry["level"]: entry for entry in barons["scaling"]["samples"]}
        self.assertEqual(4.0, samples[100]["health_multiplier"])
        self.assertEqual(2.0, samples[100]["damage_multiplier"])
        self.assertEqual(2.0, samples[100]["xp_multiplier"])

    def test_baron_rewards_and_legendary_items_are_linked(self) -> None:
        fighting = CATALOG["systems"]["fighting"]
        legendary_ids = {entry["id"] for entry in fighting["legendary_items"]}
        self.assertEqual(
            {"fire_wand", "wither_shield", "heart_of_the_beam", "spider_wand", "barons_doll"},
            legendary_ids,
        )
        reward_ids = {
            reward["identifier"].split(":", 1)[1]
            for baron in fighting["barons"]["types"]
            for reward in baron["rewards"]
            if reward["identifier"].startswith("mythicrpg:")
        }
        self.assertTrue(legendary_ids.issubset(reward_ids))
        for baron in fighting["barons"]["types"]:
            for reward in baron["rewards"]:
                self.assertTrue(reward["names"]["fr"])
                self.assertGreaterEqual(reward["count"], 1)
                if reward["chance"] is not None:
                    self.assertGreaterEqual(reward["chance"], 0)
                    self.assertLessEqual(reward["chance"], 1)

    def test_crafting_is_deep_and_keeps_complete_tree(self) -> None:
        crafting_skill = skill("crafting")
        self.assertEqual("deep", crafting_skill["coverage"])
        self.assertEqual(list(range(1, 21)), [node["id"] for node in crafting_skill["nodes"]])
        body = crafting_skill["editorial"]["fr"]["body_markdown"]
        for heading in ("Craft Score", "Recyclage", "Transformations", "Lucky Blocks"):
            self.assertIn(heading, body)

    def test_craft_score_xp_and_stations(self) -> None:
        crafting = CATALOG["systems"]["crafting"]
        self.assertEqual(55, len(crafting["craft_score"]["item_points"]))
        self.assertEqual(0.08, crafting["xp"]["score_multiplier"])
        self.assertEqual(80, crafting["xp"]["max_per_action"])
        self.assertEqual(["base_score", "midnight_workshop", "mythic_inspiration", "first_craft_bonus"], crafting["xp"]["processing_order"])
        stations = {entry["id"]: entry for entry in crafting["stations"]}
        self.assertEqual({"portable", "vanilla_table", "infinite_table"}, set(stations))
        self.assertEqual(256, stations["portable"]["max_durability"])
        self.assertIsNone(stations["infinite_table"]["max_durability"])
        self.assertEqual((1, 10), (crafting["interface"]["craft_input_start"], crafting["interface"]["craft_input_end"]))

    def test_recycling_and_transformations_are_complete(self) -> None:
        crafting = CATALOG["systems"]["crafting"]
        groups = {entry["id"]: entry for entry in crafting["recycling"]["groups"]}
        self.assertEqual({"wood", "stone", "iron", "gold"}, set(groups))
        self.assertEqual(5, len(groups["wood"]["inputs"]))
        self.assertEqual(9, len(groups["iron"]["inputs"]))
        self.assertFalse(crafting["recycling"]["grants_crafting_xp"])
        pairs = crafting["transformations"]["pairs"]
        self.assertEqual(48, len(pairs))
        self.assertEqual(48, len({(entry["input"], entry["output"]) for entry in pairs}))
        self.assertEqual(1, crafting["transformations"]["charge_per_item"])

    def test_lucky_block_events_and_probabilities(self) -> None:
        lucky = CATALOG["systems"]["crafting"]["lucky_blocks"]
        self.assertEqual((-10, 10), (lucky["luck_min"], lucky["luck_max"]))
        categories = {entry["id"]: entry for entry in lucky["categories"]}
        self.assertEqual({"positive", "neutral", "negative"}, set(categories))
        self.assertEqual(21, sum(len(entry["events"]) for entry in categories.values()))
        for category in categories.values():
            self.assertEqual(category["total_weight"], sum(event["weight"] for event in category["events"]))
            self.assertAlmostEqual(100.0, sum(event["within_category_percent"] for event in category["events"]), places=2)
        samples = {entry["luck"]: entry for entry in lucky["chance_samples"]}
        self.assertEqual((40.0, 20.0, 40.0), (samples[0]["positive"], samples[0]["neutral"], samples[0]["negative"]))
        self.assertAlmostEqual(100.0, samples[10]["positive"] + samples[10]["neutral"] + samples[10]["negative"], places=3)
        deltas = {entry["delta"] for entry in lucky["infusion_rules"]}
        self.assertEqual({-3, -2, -1, 1, 2, 3}, deltas)

    def test_fighting_and_crafting_are_server_authoritative(self) -> None:
        fighting = CATALOG["systems"]["fighting"]["multiplayer"]
        crafting = CATALOG["systems"]["crafting"]["multiplayer"]
        self.assertEqual("server", fighting["promotion_authority"])
        self.assertEqual("server", fighting["damage_authority"])
        self.assertEqual("server", fighting["reward_authority"])
        self.assertEqual("server", crafting["craft_result_authority"])
        self.assertEqual("server", crafting["lucky_block_authority"])
        self.assertTrue(crafting["portable_interface_is_client_view"])

    def test_v030_components_versions_and_release_constraints(self) -> None:
        skill_page = (ROOT / "website/src/pages/skills/[id].astro").read_text(encoding="utf-8")
        for component in ("FightingExplorer", "CraftingExplorer"):
            self.assertIn(component, skill_page)
            text = (ROOT / f"website/src/components/{component}.astro").read_text(encoding="utf-8")
            self.assertNotIn(".innerHTML", text)
            self.assertIn("addEventListener", text)
        self.assertEqual("0.4.1", CATALOG["schema_version"])
        self.assertEqual("0.4.1", SNAPSHOT["project_version"])
        self.assertEqual(25, REPORT["counts"]["baron_types"])
        self.assertEqual(55, REPORT["counts"]["craft_score_items"])
        package = json.loads((ROOT / "website/package.json").read_text(encoding="utf-8"))
        lock = json.loads((ROOT / "website/package-lock.json").read_text(encoding="utf-8"))
        self.assertEqual("0.4.1", package["version"])
        self.assertEqual("0.4.1", lock["version"])
        self.assertEqual("0.4.1", lock["packages"][""]["version"])
        self.assertIn('version = "0.4.1"', (ROOT / "pyproject.toml").read_text(encoding="utf-8"))
        self.assertIn("mod-source/src/** -text", (ROOT / ".gitattributes").read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
