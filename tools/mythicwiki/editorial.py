from __future__ import annotations

from pathlib import Path
from typing import Any

import yaml

from .utils import read_text


def read_markdown_with_frontmatter(path: Path) -> dict[str, Any]:
    text = read_text(path)
    if not text.startswith("---\n"):
        return {"frontmatter": {}, "body": text.strip()}
    end = text.find("\n---\n", 4)
    if end < 0:
        raise ValueError(f"Frontmatter non terminé: {path}")
    frontmatter = yaml.safe_load(text[4:end]) or {}
    if not isinstance(frontmatter, dict):
        raise ValueError(f"Frontmatter invalide: {path}")
    return {"frontmatter": frontmatter, "body": text[end + 5 :].strip()}


def load_skill_editorial(content_root: Path) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for locale_dir in sorted(content_root.iterdir()):
        if not locale_dir.is_dir():
            continue
        locale = locale_dir.name
        for path in sorted((locale_dir / "skills").glob("*.md")):
            parsed = read_markdown_with_frontmatter(path)
            frontmatter = parsed["frontmatter"]
            skill_id = str(frontmatter.get("id", path.stem)).lower()
            entry = result.setdefault(skill_id, {"locales": {}})
            entry["locales"][locale] = {
                "summary": str(frontmatter.get("summary", "")).strip(),
                "body_markdown": parsed["body"],
            }
            for key in ("status", "introduced_in", "visibility", "spoiler"):
                if key in frontmatter:
                    entry[key] = frontmatter[key]
    return result
