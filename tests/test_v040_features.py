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


class V040FeatureTests(unittest.TestCase):
    def test_all_nine_skills_are_deep_and_complete(self) -> None:
        self.assertEqual(9, len(CATALOG["skills"]))
        for entry in CATALOG["skills"]:
            self.assertEqual("deep", entry["coverage"], entry["id"])
            self.assertEqual(list(range(1, 21)), [node["id"] for node in entry["nodes"]], entry["id"])
            self.assertTrue(entry["editorial"]["fr"]["xp_sources"], entry["id"])
            self.assertTrue(entry["editorial"]["fr"]["key_systems"], entry["id"])
            self.assertTrue(entry["editorial"]["fr"]["multiplayer"], entry["id"])

    def test_traveling_extraction_is_complete(self) -> None:
        traveling = CATALOG["systems"]["traveling"]
        self.assertEqual(5, traveling["xp"]["movement"]["xp"])
        self.assertEqual(100.0, traveling["xp"]["movement"]["traveled_distance_required"])
        self.assertEqual(70.0, traveling["xp"]["movement"]["direct_distance_required"])
        self.assertEqual([0.1, 0.15], traveling["xp"]["multipliers"]["travel_xp_perks"])
        self.assertEqual(0.25, traveling["xp"]["multipliers"]["discovery_xp_perk"])
        self.assertEqual(22, len(traveling["mounts"]["types"]))
        self.assertEqual((17, 5), (traveling["mounts"]["land_count"], traveling["mounts"]["flying_count"]))
        self.assertEqual(22, len(traveling["tools"]["monumental_compass"]["modules"]))
        self.assertEqual(32.0, traveling["tools"]["grappling_hook"]["max_range_blocks"])
        self.assertTrue(traveling["vehicles"]["fishing_boat"]["traveling_xp_via_movement_tracker"])

    def test_traveling_server_and_persistence_rules(self) -> None:
        traveling = CATALOG["systems"]["traveling"]
        self.assertEqual("server", traveling["multiplayer"]["xp_authority"])
        self.assertTrue(traveling["multiplayer"]["movement_state_persistent_per_player"])
        self.assertTrue(traveling["multiplayer"]["mount_owner_data_persistent"])
        self.assertTrue(traveling["tools"]["death_recall"]["owner_bound"])
        self.assertTrue(traveling["tools"]["death_recall"]["dimension_persisted"])

    def test_building_catalog_plans_and_antiexploit(self) -> None:
        building = CATALOG["systems"]["building"]
        groups = building["xp"]["block_groups"]
        self.assertEqual([3, 4, 5], [entry["xp"] for entry in groups])
        self.assertEqual([35, 56, 65], [len(entry["blocks"]) for entry in groups])
        self.assertEqual(156, building["xp"]["eligible_block_count"])
        anti = building["xp"]["anti_exploitation"]
        self.assertEqual(512, anti["maximum_position_history"])
        self.assertEqual(256, anti["maximum_material_history"])
        self.assertEqual(30.0, anti["position_expiry_minutes"])
        self.assertEqual([0.2, 0.04, 0.008], anti["position_reuse_multipliers"])
        self.assertEqual((8, 12), (building["plans"]["plan_2d"]["base_max_size"], building["plans"]["plan_2d"]["upgraded_max_size"]))
        self.assertEqual(8, building["plans"]["plan_3d"]["maximum_size"])

    def test_building_tools_and_decorative_content(self) -> None:
        building = CATALOG["systems"]["building"]
        self.assertEqual([1, 2, 3], [entry["extra_reach"] for entry in building["builder_tools"]["reach_perks"]])
        self.assertEqual([5, 10, 15], [entry["range"] for entry in building["reserve"]["perk_ranges"]])
        self.assertEqual(8, building["reserve"]["maximum_chests_per_player"])
        decorative = building["decorative_content"]
        self.assertEqual(20, len(decorative["vertical_slabs"]["types"]))
        self.assertEqual(113, decorative["blank_block"]["material_count"])
        self.assertEqual(32, decorative["static_decoration"]["effect_count"])
        self.assertTrue(decorative["static_decoration"]["owner_protected"])

    def test_farming_xp_growth_and_objects(self) -> None:
        farming = CATALOG["systems"]["farming"]
        self.assertEqual([2, 3, 3, 4], [entry["xp"] for entry in farming["xp"]["harvest_categories"]])
        self.assertEqual(8, farming["xp"]["breeding"]["xp"])
        self.assertEqual(96, farming["xp"]["area_harvest"]["maximum_blocks"])
        self.assertEqual(6, farming["xp"]["anti_automation"]["recent_replant_protection_ticks"])
        self.assertEqual([0.001, 0.002, 0.003], farming["perk_values"]["enchanted_seed_chances"])
        self.assertEqual([3, 5, 7], farming["perk_values"]["farmer_reach_radii"])
        self.assertEqual(200, farming["growth"]["living_field"]["extra_attempts_per_second"])
        self.assertEqual(10.0, farming["growth"]["living_field"]["extra_attempts_per_tick"])
        self.assertEqual(54, farming["objects"]["food_backpack"]["slots"])
        self.assertTrue(farming["death_and_persistence"]["preserves_food_backpacks"])

    def test_woodcutting_xp_drops_and_tools(self) -> None:
        wood = CATALOG["systems"]["woodcutting"]
        self.assertEqual(2, wood["xp"]["log_xp"])
        self.assertEqual(32, wood["xp"]["timber"]["maximum_additional_blocks"])
        self.assertEqual([0.05, 0.15, 0.2], wood["drops"]["double_drop_chances"])
        self.assertEqual([0.001, 0.0015, 0.002, 0.003], wood["drops"]["enchanted_wood_chances"])
        self.assertEqual(8, len(wood["drops"]["random_saplings"]))
        self.assertEqual(5.0, wood["tree_growth"]["cooldown_seconds"])
        self.assertEqual(30, wood["enchanted_axe"]["offhand_durability_cost"])
        self.assertEqual((6, 1.2), (wood["wood_eater"]["hunger"], wood["wood_eater"]["saturation_modifier"]))

    def test_woodcutting_chest_modules_are_safe_and_complete(self) -> None:
        modules = CATALOG["systems"]["woodcutting"]["chest_modules"]
        self.assertEqual(3, len(modules["tiers"]))
        self.assertEqual([9, 18, 27], [entry["extra_slots"] for entry in modules["tiers"]])
        self.assertEqual([36, 45, 54], [entry["single_chest_capacity"] for entry in modules["tiers"]])
        self.assertEqual([72, 90, 108], [entry["double_chest_capacity_with_two_equal_modules"] for entry in modules["tiers"]])
        self.assertEqual(108, modules["maximum_total_storage"])
        self.assertEqual(27, modules["persistent_extra_storage_per_physical_chest"])
        self.assertTrue(modules["safe_shrink_requires_all_items_to_fit"])
        self.assertTrue(modules["atomic_repack_on_shrink"])
        self.assertTrue(modules["hoppers_use_active_capacity"])

    def test_components_navigation_and_safe_dom(self) -> None:
        skill_page = (ROOT / "website/src/pages/skills/[id].astro").read_text(encoding="utf-8")
        for component in ("TravelingExplorer", "BuildingExplorer", "FarmingExplorer", "WoodcuttingExplorer"):
            self.assertIn(component, skill_page)
            text = (ROOT / f"website/src/components/{component}.astro").read_text(encoding="utf-8")
            self.assertNotIn(".innerHTML", text)
        for anchor in ("#mounts", "#structure-modules", "#building-plans", "#building-decoration", "#farming-growth", "#chest-modules"):
            self.assertIn(anchor, skill_page)

    def test_search_entries_for_new_systems(self) -> None:
        entries = json.loads((ROOT / "data/generated/search-index.json").read_text(encoding="utf-8"))
        types = [entry["type"] for entry in entries]
        self.assertEqual(22, types.count("mount"))
        self.assertEqual(22, types.count("structure_module"))
        urls = {entry["url"] for entry in entries if entry["type"] in {"mount", "structure_module"}}
        self.assertEqual({"/skills/traveling/#mounts", "/skills/traveling/#structure-modules"}, urls)

    def test_v040_versions_and_delivery_constraints(self) -> None:
        self.assertEqual("0.4.1", CATALOG["schema_version"])
        self.assertEqual("0.4.1", SNAPSHOT["project_version"])
        package = json.loads((ROOT / "website/package.json").read_text(encoding="utf-8"))
        lock = json.loads((ROOT / "website/package-lock.json").read_text(encoding="utf-8"))
        self.assertEqual("0.4.1", package["version"])
        self.assertEqual("0.4.1", lock["version"])
        self.assertEqual("0.4.1", lock["packages"][""]["version"])
        self.assertIn('version = "0.4.1"', (ROOT / "pyproject.toml").read_text(encoding="utf-8"))
        self.assertIn("mod-source/src/** -text", (ROOT / ".gitattributes").read_text(encoding="utf-8"))
        self.assertTrue((ROOT / "website/package-lock.json").is_file())
        self.assertEqual("d39d740f8d5f36d37a0e24539287247e1b98c89f49c47a6a99fd6a83988b8b3f", CATALOG["source"]["tree_sha256"])

    def test_report_contains_new_counts(self) -> None:
        counts = REPORT["counts"]
        self.assertEqual(22, counts["traveling_mounts"])
        self.assertEqual(22, counts["traveling_structure_modules"])
        self.assertEqual(156, counts["building_xp_blocks"])
        self.assertEqual(20, counts["building_vertical_slabs"])
        self.assertEqual(32, counts["building_static_effects"])
        self.assertEqual(4, counts["farming_harvest_categories"])
        self.assertEqual(3, counts["woodcutting_chest_modules"])
        self.assertEqual(752, counts["search_entries"])


if __name__ == "__main__":
    unittest.main()
