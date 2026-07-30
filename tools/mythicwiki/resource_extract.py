from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from .utils import copy_if_exists, read_text, unique_sorted


def load_translations(resources_root: Path) -> dict[str, dict[str, str]]:
    lang_root = resources_root / "assets/mythicrpg/lang"
    locales: dict[str, dict[str, str]] = {}
    for path in sorted(lang_root.glob("*.json")):
        raw = json.loads(read_text(path))
        locales[path.stem] = {str(k): str(v) for k, v in raw.items()}
    return locales


def _translated(locales: dict[str, dict[str, str]], key: str, fallback: str) -> dict[str, str]:
    return {
        "fr": locales.get("fr_fr", {}).get(key, fallback),
        "en": locales.get("en_us", {}).get(key, fallback),
    }


def _model_texture(model_data: dict[str, Any], kind: str, content_id: str) -> str | None:
    textures = model_data.get("textures")
    if isinstance(textures, dict):
        for key in ("layer0", "all", "particle", "side", "top"):
            value = textures.get(key)
            if isinstance(value, str) and not value.startswith("#"):
                return value
    parent = model_data.get("parent")
    if isinstance(parent, str) and parent.startswith("mythicrpg:block/"):
        return parent.replace("mythicrpg:block/", "mythicrpg:block/")
    return f"mythicrpg:{kind}/{content_id}"


def _copy_texture(resources_root: Path, public_root: Path, texture_ref: str | None) -> str | None:
    if not texture_ref or ":" not in texture_ref:
        return None
    namespace, relative = texture_ref.split(":", 1)
    if namespace != "mythicrpg":
        return None
    source = resources_root / f"assets/mythicrpg/textures/{relative}.png"
    destination = public_root / f"textures/{relative}.png"
    if copy_if_exists(source, destination):
        return f"/generated/textures/{relative}.png"
    return None


def extract_content(resources_root: Path, public_root: Path, locales: dict[str, dict[str, str]], literal_regs: dict[str, set[str]]) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    assets = resources_root / "assets/mythicrpg"
    block_ids = {path.stem for path in (assets / "blockstates").glob("*.json")}
    items: list[dict[str, Any]] = []
    for model_path in sorted((assets / "models/item").glob("*.json")):
        content_id = model_path.stem
        model = json.loads(read_text(model_path))
        is_block_item = content_id in block_ids
        translation_key = ("block" if is_block_item else "item") + f".mythicrpg.{content_id}"
        fallback = content_id.replace("_", " ").title()
        texture_ref = _model_texture(model, "block" if is_block_item else "item", content_id)
        names = _translated(locales, translation_key, fallback)
        texture = _copy_texture(resources_root, public_root, texture_ref)
        literal_item_registration = content_id in literal_regs["item"]
        literal_block_registration = is_block_item and content_id in literal_regs["block"]
        items.append({
            "id": content_id,
            "identifier": f"mythicrpg:{content_id}",
            "kind": "block_item" if is_block_item else "item",
            "names": names,
            "translation_key": translation_key,
            "model": f"mythicrpg:item/{content_id}",
            "texture": texture,
            "registration_evidence": literal_item_registration or literal_block_registration,
            "registration_status": "confirmed" if literal_item_registration or literal_block_registration else "model_only",
            "evidence": {
                "literal_item_registration": literal_item_registration,
                "literal_block_registration": literal_block_registration,
                "recipe_reference": False,
                "translation": translation_key in locales.get("fr_fr", {}) or translation_key in locales.get("en_us", {}),
                "texture": texture is not None,
                "blockstate": is_block_item,
            },
            "extraction": {"method": "item_model", "file": model_path.relative_to(resources_root).as_posix()},
        })
    blocks: list[dict[str, Any]] = []
    for state_path in sorted((assets / "blockstates").glob("*.json")):
        content_id = state_path.stem
        translation_key = f"block.mythicrpg.{content_id}"
        fallback = content_id.replace("_", " ").title()
        texture = None
        item_match = next((item for item in items if item["id"] == content_id), None)
        if item_match:
            texture = item_match["texture"]
        blocks.append({
            "id": content_id,
            "identifier": f"mythicrpg:{content_id}",
            "names": _translated(locales, translation_key, fallback),
            "texture": texture,
            "registration_evidence": content_id in literal_regs["block"],
            "extraction": {"method": "blockstate", "file": state_path.relative_to(resources_root).as_posix()},
        })
    return items, blocks


def extract_recipes(resources_root: Path, locales: dict[str, dict[str, str]]) -> list[dict[str, Any]]:
    recipe_root = resources_root / "data/mythicrpg/recipe"
    recipes: list[dict[str, Any]] = []
    if not recipe_root.is_dir():
        recipe_root = resources_root / "data/mythicrpg/recipes"
    for path in sorted(recipe_root.rglob("*.json")):
        data = json.loads(read_text(path))
        recipe_type = str(data.get("type", "unknown"))
        result_raw = data.get("result")
        if isinstance(result_raw, str):
            result_id, count = result_raw, 1
        elif isinstance(result_raw, dict):
            result_id = str(result_raw.get("id") or result_raw.get("item") or "unknown")
            count = int(result_raw.get("count", 1))
        else:
            result_id, count = "unknown", 1
        ingredients: list[str] = []
        if isinstance(data.get("ingredients"), list):
            for ingredient in data["ingredients"]:
                if isinstance(ingredient, str):
                    ingredients.append(ingredient)
                elif isinstance(ingredient, dict):
                    ingredients.append(str(ingredient.get("item") or ingredient.get("tag") or ingredient.get("id") or "unknown"))
        if isinstance(data.get("key"), dict):
            for ingredient in data["key"].values():
                if isinstance(ingredient, str):
                    ingredients.append(ingredient)
                elif isinstance(ingredient, dict):
                    ingredients.append(str(ingredient.get("item") or ingredient.get("tag") or ingredient.get("id") or "unknown"))
        local_id = path.relative_to(recipe_root).with_suffix("").as_posix()
        result_local = result_id.split(":", 1)[-1]
        key = f"item.mythicrpg.{result_local}"
        if key not in locales.get("fr_fr", {}):
            key = f"block.mythicrpg.{result_local}"
        fallback = result_local.replace("_", " ").title()
        recipes.append({
            "id": local_id,
            "type": recipe_type,
            "result": {"id": result_id, "count": count, "names": _translated(locales, key, fallback)},
            "ingredients": unique_sorted(ingredients),
            "pattern": data.get("pattern"),
            "group": data.get("group"),
            "extraction": {"method": "recipe_json", "file": path.relative_to(resources_root).as_posix()},
        })
    return recipes
