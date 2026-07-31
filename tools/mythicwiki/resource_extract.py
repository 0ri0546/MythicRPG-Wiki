from __future__ import annotations

import json
from pathlib import Path
from typing import Any

import yaml

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


def _readable_identifier(identifier: str) -> str:
    value = identifier.split(":", 1)[-1].replace("_", " ").replace("/", " ")
    return value[:1].upper() + value[1:]


def _load_item_tags(resources_root: Path) -> dict[str, list[str]]:
    tags: dict[str, list[str]] = {}
    root = resources_root / "data/mythicrpg/tags/item"
    if not root.is_dir():
        return tags
    for path in sorted(root.rglob("*.json")):
        data = json.loads(read_text(path))
        values = [str(value) for value in data.get("values", []) if isinstance(value, str) and not value.startswith("#")]
        tag_id = f"mythicrpg:{path.relative_to(root).with_suffix('').as_posix()}"
        tags[tag_id] = values
    return tags


def _load_editorial_tag_variants(path: Path | None) -> dict[str, list[str]]:
    if path is None or not path.is_file():
        return {}
    data = yaml.safe_load(read_text(path)) or {}
    return {
        str(tag): [str(value) for value in values]
        for tag, values in (data.get("tags") or {}).items()
        if isinstance(values, list)
    }


def _visual_item(identifier: str, item_lookup: dict[str, dict[str, Any]]) -> dict[str, Any]:
    local_id = identifier.split(":", 1)[-1]
    item = item_lookup.get(identifier) or item_lookup.get(f"mythicrpg:{local_id}")
    fallback = _readable_identifier(identifier)
    return {
        "id": identifier,
        "names": item.get("names", {"fr": fallback, "en": fallback}) if item else {"fr": fallback, "en": fallback},
        "texture": item.get("texture") if item else None,
    }


def _ingredient_descriptor(
    raw: Any,
    item_lookup: dict[str, dict[str, Any]],
    tag_variants: dict[str, list[str]],
) -> dict[str, Any]:
    if isinstance(raw, str):
        identifier = raw
        kind = "tag" if raw.startswith("#") else "item"
        if kind == "tag":
            identifier = raw[1:]
    elif isinstance(raw, dict):
        if raw.get("item") or raw.get("id"):
            identifier = str(raw.get("item") or raw.get("id"))
            kind = "item"
        elif raw.get("tag"):
            identifier = str(raw["tag"])
            kind = "tag"
        else:
            identifier, kind = "unknown", "unknown"
    else:
        identifier, kind = "unknown", "unknown"

    if kind == "item":
        visual = _visual_item(identifier, item_lookup)
        return {
            "kind": "item",
            "id": identifier,
            "names": visual["names"],
            "representative": visual,
            "variants": [visual],
            "variant_count": 1,
            "variants_complete": True,
        }

    variants = [_visual_item(value, item_lookup) for value in tag_variants.get(identifier, [])]
    representative = variants[0] if variants else {
        "id": identifier,
        "names": {"fr": _readable_identifier(identifier), "en": _readable_identifier(identifier)},
        "texture": None,
    }
    label = _readable_identifier(identifier)
    return {
        "kind": "tag" if kind == "tag" else "unknown",
        "id": identifier,
        "names": {"fr": f"Tag {label}", "en": f"Tag {label}"},
        "representative": representative,
        "variants": variants,
        "variant_count": len(variants),
        "variants_complete": bool(variants),
    }


def extract_recipes(
    resources_root: Path,
    locales: dict[str, dict[str, str]],
    items: list[dict[str, Any]],
    tag_variants_config: Path | None = None,
) -> list[dict[str, Any]]:
    recipe_root = resources_root / "data/mythicrpg/recipe"
    recipes: list[dict[str, Any]] = []
    if not recipe_root.is_dir():
        recipe_root = resources_root / "data/mythicrpg/recipes"
    item_lookup = {item["identifier"]: item for item in items}
    tag_variants = _load_item_tags(resources_root)
    tag_variants.update(_load_editorial_tag_variants(tag_variants_config))

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

        result_local = result_id.split(":", 1)[-1]
        key = f"item.mythicrpg.{result_local}"
        if key not in locales.get("fr_fr", {}):
            key = f"block.mythicrpg.{result_local}"
        fallback = result_local.replace("_", " ").title()
        result_visual = _visual_item(result_id, item_lookup)
        result = {
            "id": result_id,
            "count": count,
            "names": _translated(locales, key, fallback),
            "texture": result_visual["texture"],
        }

        ingredient_ids: list[str] = []
        visual: dict[str, Any]
        if recipe_type == "minecraft:crafting_shaped":
            pattern = [str(row) for row in data.get("pattern", [])]
            key_data = data.get("key") if isinstance(data.get("key"), dict) else {}
            key_descriptors = {
                symbol: _ingredient_descriptor(raw, item_lookup, tag_variants)
                for symbol, raw in key_data.items()
            }
            slots: list[dict[str, Any] | None] = [None] * 9
            for row_index, row in enumerate(pattern[:3]):
                for column_index, symbol in enumerate(row[:3]):
                    if symbol == " ":
                        continue
                    descriptor = key_descriptors.get(symbol)
                    slots[row_index * 3 + column_index] = descriptor
                    if descriptor:
                        ingredient_ids.append(descriptor["id"])
            visual = {
                "kind": "shaped",
                "station": "minecraft:crafting_table",
                "slots": slots,
                "ingredients": [],
                "pattern_width": max((len(row) for row in pattern), default=0),
                "pattern_height": len(pattern),
                "shiftable": len(pattern) < 3 or max((len(row) for row in pattern), default=0) < 3,
            }
        elif recipe_type == "minecraft:crafting_shapeless":
            descriptors = [
                _ingredient_descriptor(raw, item_lookup, tag_variants)
                for raw in data.get("ingredients", [])
            ]
            ingredient_ids.extend(descriptor["id"] for descriptor in descriptors)
            visual = {
                "kind": "shapeless",
                "station": "minecraft:crafting_table",
                "slots": [],
                "ingredients": descriptors,
                "pattern_width": None,
                "pattern_height": None,
                "shiftable": True,
            }
        else:
            descriptors = [
                _ingredient_descriptor(raw, item_lookup, tag_variants)
                for raw in data.get("ingredients", [])
            ]
            ingredient_ids.extend(descriptor["id"] for descriptor in descriptors)
            visual = {
                "kind": "station",
                "station": recipe_type,
                "slots": [],
                "ingredients": descriptors,
                "pattern_width": None,
                "pattern_height": None,
                "shiftable": False,
            }

        local_id = path.relative_to(recipe_root).with_suffix("").as_posix()
        recipes.append({
            "id": local_id,
            "type": recipe_type,
            "result": result,
            "ingredients": unique_sorted(ingredient_ids),
            "pattern": data.get("pattern"),
            "group": data.get("group"),
            "category": data.get("category"),
            "visual": visual,
            "extraction": {"method": "recipe_json", "file": path.relative_to(resources_root).as_posix()},
        })
    return recipes
