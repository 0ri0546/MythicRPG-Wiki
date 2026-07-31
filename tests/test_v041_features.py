from __future__ import annotations

import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = json.loads((ROOT / "data/generated/catalog.json").read_text(encoding="utf-8"))
REPORT = json.loads((ROOT / "data/generated/extraction-report.json").read_text(encoding="utf-8"))
SNAPSHOT = json.loads((ROOT / "config/source_snapshot.json").read_text(encoding="utf-8"))


class V041FeatureTests(unittest.TestCase):
    def test_all_calculators_use_deferred_number_commit(self) -> None:
        utility = (ROOT / "website/src/scripts/number-input.ts").read_text(encoding="utf-8")
        self.assertIn("raw === ''", utility)
        self.assertIn("Number.isFinite", utility)
        for component in ("XpExplorer", "EatingExplorer", "FightingExplorer", "CraftingExplorer"):
            text = (ROOT / f"website/src/components/{component}.astro").read_text(encoding="utf-8")
            self.assertIn("readNumberInput", text, component)
            self.assertIn("commitNumberInput", text, component)
            self.assertIn("addEventListener('blur'", text, component)
        self.assertNotIn("quantityInput.value = String(quantity)", (ROOT / "website/src/components/EatingExplorer.astro").read_text(encoding="utf-8"))
        self.assertNotIn("levelInput.value = String(level)", (ROOT / "website/src/components/FightingExplorer.astro").read_text(encoding="utf-8"))

    def test_every_recipe_has_visual_data(self) -> None:
        self.assertEqual(188, len(CATALOG["recipes"]))
        self.assertEqual(188, REPORT["counts"]["recipes_with_visuals"])
        self.assertEqual(129, REPORT["counts"]["recipes_shaped"])
        self.assertEqual(59, REPORT["counts"]["recipes_shapeless"])
        for recipe in CATALOG["recipes"]:
            visual = recipe["visual"]
            self.assertIn(visual["kind"], {"shaped", "shapeless", "station"})
            self.assertGreaterEqual(recipe["result"]["count"], 1)
            self.assertIn("texture", recipe["result"])
            if visual["kind"] == "shaped":
                self.assertEqual(9, len(visual["slots"]), recipe["id"])
                self.assertTrue(recipe["pattern"], recipe["id"])
            if visual["kind"] == "shapeless":
                self.assertEqual([], visual["slots"])
                self.assertGreater(len(visual["ingredients"]), 0)

    def test_tag_ingredients_explain_variants(self) -> None:
        tags = [
            ingredient
            for recipe in CATALOG["recipes"]
            for ingredient in [*recipe["visual"]["ingredients"], *[slot for slot in recipe["visual"]["slots"] if slot]]
            if ingredient["kind"] == "tag"
        ]
        self.assertGreater(len(tags), 0)
        self.assertTrue(any(tag["variant_count"] > 1 for tag in tags))
        for tag in tags:
            self.assertIn("representative", tag)
            self.assertIn("variants_complete", tag)

    def test_recipe_component_is_visual_and_accessible(self) -> None:
        text = (ROOT / "website/src/components/RecipeVisual.astro").read_text(encoding="utf-8")
        for marker in ("crafting-grid", "recipe-arrow", "recipe-result", "Recette sans forme", "Variantes acceptées"):
            self.assertIn(marker, text)
        self.assertIn("aria-label", text)
        self.assertIn("alt=", text)
        self.assertNotIn("innerHTML", text)
        page = (ROOT / "website/src/pages/recipes/[id].astro").read_text(encoding="utf-8")
        self.assertIn("<RecipeVisual recipe={recipe}", page)

    def test_all_perks_have_centralized_icons(self) -> None:
        nodes = [node for skill in CATALOG["skills"] for node in skill["nodes"]]
        self.assertEqual(180, len(nodes))
        self.assertEqual(180, REPORT["counts"]["perk_icons_specific"] + REPORT["counts"]["perk_icons_fallback"])
        for node in nodes:
            icon = node["icon"]
            self.assertIn(icon["mapping"], {"specific", "fallback"})
            self.assertEqual("config/perk_icons.yaml", icon["source"])
            self.assertTrue(icon["item_id"] or icon["names"]["fr"])
        self.assertTrue((ROOT / "config/perk_icons.yaml").is_file())

    def test_perk_click_target_covers_children(self) -> None:
        tree = (ROOT / "website/src/components/SkillTree.astro").read_text(encoding="utf-8")
        css = (ROOT / "website/src/styles/global.css").read_text(encoding="utf-8")
        self.assertIn("skill-node-visual", tree)
        self.assertIn("skill-node-number", tree)
        self.assertIn("event.target.closest('[data-node]')", tree)
        self.assertRegex(css, r"\.skill-node\s*>\s*\*\s*\{[^}]*pointer-events:\s*none")
        self.assertRegex(css, r"\.skill-tree-lines\s*\{[^}]*pointer-events:\s*none")
        self.assertIn(".skill-node:focus-visible", css)

    def test_checkboxes_use_shared_vertical_alignment(self) -> None:
        css = (ROOT / "website/src/styles/global.css").read_text(encoding="utf-8")
        self.assertRegex(css, r"\.form-choice\s*\{[^}]*display:\s*inline-flex[^}]*align-items:\s*center")
        self.assertRegex(css, r"\.form-choice input\[type=\"checkbox\"\][^{]*\{[^}]*margin:\s*0")
        for path in (ROOT / "website/src/components").glob("*.astro"):
            text = path.read_text(encoding="utf-8")
            for match in re.finditer(r'<input type="checkbox"', text):
                self.assertIn('class="form-choice"', text[max(0, match.start() - 120):match.start()], path.name)

    def test_eating_filters_search_translated_ingredients(self) -> None:
        text = (ROOT / "website/src/components/EatingExplorer.astro").read_text(encoding="utf-8")
        self.assertIn("normalizeSearch", text)
        self.assertIn("ingredient.names.fr", text)
        self.assertIn("ingredient.names.en", text)
        self.assertIn("ingredientSearchText(ingredient.item)", text)
        self.assertIn("data-ingredient-empty", text)
        self.assertIn("data-cooking-empty", text)
        self.assertIn("data-ingredient-reset", text)
        self.assertIn("data-cooking-reset", text)
        self.assertNotIn("innerHTML", text)

    def test_v041_versions_and_snapshot(self) -> None:
        self.assertEqual("0.4.1", CATALOG["schema_version"])
        self.assertEqual("0.4.1", SNAPSHOT["project_version"])
        package = json.loads((ROOT / "website/package.json").read_text(encoding="utf-8"))
        lock = json.loads((ROOT / "website/package-lock.json").read_text(encoding="utf-8"))
        self.assertEqual("0.4.1", package["version"])
        self.assertEqual("0.4.1", lock["version"])
        self.assertEqual("0.4.1", lock["packages"][""]["version"])
        self.assertIn('version = "0.4.1"', (ROOT / "pyproject.toml").read_text(encoding="utf-8"))
        self.assertEqual(CATALOG["source"]["tree_sha256"], SNAPSHOT["tree_sha256"])


if __name__ == "__main__":
    unittest.main()
