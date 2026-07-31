from __future__ import annotations

import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


class ProjectStructureTests(unittest.TestCase):
    def test_required_delivery_files_exist(self) -> None:
        required = [
            "tools/build_catalog.py",
            "config/documented_values.yaml",
            "config/source_snapshot.json",
            "data/generated/catalog.json",
            "data/generated/encyclopedia.json",
            "website/package.json",
            "website/astro.config.mjs",
            "website/src/pages/index.astro",
            "website/src/pages/systems/progression.astro",
            "website/src/components/XpExplorer.astro",
            "website/src/components/FishingExplorer.astro",
            "website/src/components/RelatedContentExplorer.astro",
            ".github/workflows/deploy-pages.yml",
            "README.md",
        ]
        missing = [path for path in required if not (ROOT / path).is_file()]
        self.assertEqual([], missing)

    def test_no_gradle_or_minecraft_execution_in_scripts(self) -> None:
        inspected = list((ROOT / "tools").rglob("*.py")) + list((ROOT / "scripts").rglob("*.py")) + list((ROOT / ".github").rglob("*.yml"))
        for path in inspected:
            text = path.read_text(encoding="utf-8").lower()
            self.assertNotRegex(text, r"\bgradlew?\b", path.as_posix())
            self.assertNotIn("runclient", text, path.as_posix())
            self.assertNotIn("runserver", text, path.as_posix())

    def test_reproducible_npm_install_is_required(self) -> None:
        workflow = (ROOT / ".github/workflows/deploy-pages.yml").read_text(encoding="utf-8")
        build_script = (ROOT / "scripts/build_all.py").read_text(encoding="utf-8")
        self.assertIn("npm ci", workflow)
        self.assertNotIn("npm install", workflow)
        self.assertRegex(
            build_script,
            r"\[\s*npm\s*,\s*[\"']ci[\"']",
        )
        self.assertNotIn("'npm', 'install'", build_script)

    def test_github_pages_base_is_not_hardcoded(self) -> None:
        config = (ROOT / "website/astro.config.mjs").read_text(encoding="utf-8")
        self.assertIn("GITHUB_REPOSITORY", config)
        self.assertIn("base", config)
        for path in (ROOT / "website/src").rglob("*.astro"):
            text = path.read_text(encoding="utf-8")
            self.assertIsNone(re.search(r"href=[\"\']/[^\"\']", text), path.as_posix())

    def test_search_index_urls_have_matching_sections(self) -> None:
        entries = json.loads((ROOT / "data/generated/search-index.json").read_text(encoding="utf-8"))
        allowed = ("/skills/", "/items/", "/recipes/", "/systems/")
        self.assertTrue(all(entry["url"].startswith(allowed) for entry in entries))


    def test_release_lockfile_status(self) -> None:
        lockfile = ROOT / "website/package-lock.json"
        if not lockfile.is_file():
            self.skipTest("package-lock.json non généré: registre npm indisponible dans cet environnement")
        data = json.loads(lockfile.read_text(encoding="utf-8"))
        self.assertEqual(3, data.get("lockfileVersion"))

    def test_browser_scripts_do_not_assign_inner_html(self) -> None:
        for path in (ROOT / "website/src").rglob("*.astro"):
            text = path.read_text(encoding="utf-8")
            self.assertNotIn(".innerHTML", text, path.as_posix())

    def test_global_progression_is_not_attached_to_mining(self) -> None:
        skill_page = (ROOT / "website/src/pages/skills/[id].astro").read_text(encoding="utf-8")
        progression_page = (ROOT / "website/src/pages/systems/progression.astro").read_text(encoding="utf-8")
        self.assertNotIn("value.id.startsWith('progression.')", skill_page)
        self.assertIn('XpExplorer', progression_page)
        self.assertIn('catalog.systems.progression', progression_page)


if __name__ == "__main__":
    unittest.main()
