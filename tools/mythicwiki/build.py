from __future__ import annotations

import json
import shutil
from pathlib import Path
from typing import Any

from .editorial import load_skill_editorial
from .java_extract import extract_literal_registrations, extract_skill_ids, extract_skill_tree, load_documented_values
from .resource_extract import extract_content, extract_recipes, load_translations
from .systems_extract import (
    extract_eating_system,
    extract_fishing_system,
    extract_fighting_system,
    extract_crafting_system,
    extract_mining_system,
    extract_progression_system,
    extract_skill_analysis,
)
from .utils import tree_sha256, write_json


def _skill_tree_path(java_root: Path, skill_id: str) -> Path:
    return java_root / f"com/mythicrpg/{skill_id}/{skill_id.title().replace('_', '')}SkillTree.java"


def _translation(locales: dict[str, dict[str, str]], locale: str, key: str, fallback: str) -> str:
    return locales.get(locale, {}).get(key, fallback)


def _related_content(skill_id: str, items: list[dict[str, Any]], recipes: list[dict[str, Any]]) -> dict[str, Any]:
    keyword_rules: dict[str, tuple[str, ...]] = {
        "mining": ("fossil", "archae", "drill", "incubator", "palette", "aegis", "growth_totem"),
        "fighting": ("baron", "wand", "legendary_shield", "heart_of_the_beam"),
        "woodcutting": ("wood", "axe", "chest_module", "modular_chest", "sapling"),
        "farming": ("farm", "crop", "flower", "food_backpack", "breeding"),
        "crafting": ("craft", "lucky", "repair_kit", "coin_toss", "transformation"),
        "traveling": ("travel", "mount", "saddle", "grappling", "minecart", "vehicle", "compass", "recall"),
        "building": ("building", "builder", "architect", "blank_block", "static_decoration", "plan_", "scaffolding", "miniature"),
        "fishing": ("fish", "bait", "rune", "nessie", "megalodon", "whale", "scale", "weather_wand", "codex"),
        "eating": ("dish", "cooking", "chef", "fridge", "serving_plate", "delivery_phone", "signature"),
    }
    keywords = keyword_rules.get(skill_id, ())

    def matches(identifier: str) -> bool:
        normalized = identifier.lower()
        return any(keyword in normalized for keyword in keywords)

    item_ids = sorted(item["id"] for item in items if matches(item["id"]))
    recipe_ids = sorted(
        recipe["id"]
        for recipe in recipes
        if matches(recipe["id"]) or matches(recipe["result"]["id"])
    )
    return {
        "items": item_ids,
        "recipes": recipe_ids,
        "method": "identifier_keyword_inference",
        "confidence": "editorial_association_not_runtime_registration",
    }


def build_catalog(project_root: Path, source_root: Path) -> dict[str, Any]:
    java_root = source_root / "main/java"
    resources_root = source_root / "main/resources"
    output_root = project_root / "data/generated"
    public_generated = project_root / "website/public/generated"
    if public_generated.exists():
        shutil.rmtree(public_generated)
    public_generated.mkdir(parents=True, exist_ok=True)

    locales = load_translations(resources_root)
    literal_regs = extract_literal_registrations(java_root)
    items, blocks = extract_content(resources_root, public_generated, locales, literal_regs)
    recipes = extract_recipes(resources_root, locales)
    editorial = load_skill_editorial(project_root / "documentation/content")
    skill_ids = extract_skill_ids(java_root / "com/mythicrpg/core/SkillType.java")

    skills: list[dict[str, Any]] = []
    warnings: list[str] = []
    errors: list[str] = []
    for skill_id in skill_ids:
        path = _skill_tree_path(java_root, skill_id)
        if not path.is_file():
            errors.append(f"Arbre de skill introuvable: {path.relative_to(source_root)}")
            continue
        nodes = extract_skill_tree(path, skill_id)
        for node in nodes:
            node["extraction"]["file"] = path.relative_to(source_root).as_posix()
        if len(nodes) != 20:
            errors.append(f"{skill_id}: {len(nodes)} perks extraits au lieu de 20")
        skill_editorial = editorial.get(skill_id, {})
        locale_content = skill_editorial.get("locales", {})
        skill_key = f"skill.mythicrpg.{skill_id}"
        for node in nodes:
            fallback = f"{skill_id.title()} {node['id']}"
            node["names"] = {
                "fr": _translation(locales, "fr_fr", node["name_key"], fallback),
                "en": _translation(locales, "en_us", node["name_key"], fallback),
            }
            node["descriptions"] = {
                "fr": _translation(locales, "fr_fr", node["description_key"], ""),
                "en": _translation(locales, "en_us", node["description_key"], ""),
            }
        skill = {
            "id": skill_id,
            "names": {
                "fr": _translation(locales, "fr_fr", skill_key, skill_id.title()),
                "en": _translation(locales, "en_us", skill_key, skill_id.title()),
            },
            "status": skill_editorial.get("status", "unknown"),
            "introduced_in": skill_editorial.get("introduced_in"),
            "visibility": skill_editorial.get("visibility", ["website", "encyclopedia"]),
            "coverage": skill_editorial.get("coverage", "structured"),
            "editorial": locale_content,
            "nodes": nodes,
            "extraction": {"method": "skill_tree_java", "file": path.relative_to(source_root).as_posix()},
        }
        skill["analysis"] = extract_skill_analysis(skill)
        skills.append(skill)

    values, value_errors = load_documented_values(project_root / "config/documented_values.yaml", java_root)
    errors.extend(value_errors)

    recipe_result_ids = {recipe["result"]["id"].split(":", 1)[-1] for recipe in recipes}
    recipe_reference_ids: set[str] = set(recipe_result_ids)
    for recipe in recipes:
        for ingredient in recipe.get("ingredients", []):
            if isinstance(ingredient, str) and ingredient.startswith("mythicrpg:"):
                recipe_reference_ids.add(ingredient.split(":", 1)[-1])
    for item in items:
        item["recipe_available"] = item["id"] in recipe_result_ids
        item["evidence"]["recipe_reference"] = item["id"] in recipe_reference_ids
        if item["registration_evidence"]:
            item["registration_status"] = "confirmed"
        elif item["evidence"]["recipe_reference"]:
            item["registration_status"] = "dynamic_probable"
        else:
            item["registration_status"] = "model_only"
    item_ids = {item["id"] for item in items}
    for recipe in recipes:
        local = recipe["result"]["id"].split(":", 1)[-1]
        if recipe["result"]["id"].startswith("mythicrpg:") and local not in item_ids:
            warnings.append(f"Résultat de recette sans modèle d'item: {recipe['id']} -> {local}")

    for skill in skills:
        skill["related_content"] = _related_content(skill["id"], items, recipes)

    systems = {
        "progression": extract_progression_system(java_root, values),
        "mining": extract_mining_system(java_root, locales),
        "eating": extract_eating_system(java_root, locales),
        "fishing": extract_fishing_system(java_root, locales),
        "fighting": extract_fighting_system(java_root, locales),
        "crafting": extract_crafting_system(java_root, locales),
    }

    catalog = {
        "schema_version": "0.3.0",
        "source": {
            "canonical_source": "src(92).zip",
            "received_archive": "src(92).zip",
            "archive_sha256": "4b27706a74060dee996d9c369aa83387e6d68bda2cc69f1be9a5349d3b0c9b96",
            "tree_sha256": tree_sha256(source_root),
            "inspection": "static_only",
            "gradle_run": False,
            "minecraft_run": False,
            "source_root": "mod-source/src",
        },
        "locales": {locale: len(entries) for locale, entries in locales.items()},
        "skills": skills,
        "items": items,
        "blocks": blocks,
        "recipes": recipes,
        "values": values,
        "systems": systems,
    }

    search_entries: list[dict[str, Any]] = []
    for skill in skills:
        search_entries.append({"type": "skill", "id": skill["id"], "title": skill["names"], "url": f"/skills/{skill['id']}/"})
        for node in skill["nodes"]:
            search_entries.append({"type": "perk", "id": node["slug"], "title": node["names"], "url": f"/skills/{skill['id']}/#{node['slug']}"})
    for item in items:
        search_entries.append({"type": "item", "id": item["id"], "title": item["names"], "url": f"/items/{item['id']}/"})
    for recipe in recipes:
        search_entries.append({"type": "recipe", "id": recipe["id"], "title": recipe["result"]["names"], "url": f"/recipes/{recipe['id']}/"})
    search_entries.append({
        "type": "system",
        "id": "progression",
        "title": {"fr": "Progression globale", "en": "Global progression"},
        "url": "/systems/progression/",
    })
    for family in systems["mining"]["fossils"]["families"]:
        search_entries.append({"type": "family", "id": f"fossil-{family['id']}", "title": family["names"], "url": "/skills/mining/#fossils"})
    for recipe in systems["eating"]["cooking_recipes"]:
        search_entries.append({"type": "cooking_recipe", "id": recipe["id"], "title": recipe["names"], "url": "/skills/eating/#cooking-recipes"})
    for family in systems["fishing"]["families"]:
        search_entries.append({"type": "family", "id": f"fishing-{family['id']}", "title": family["names"], "url": "/skills/fishing/#families"})
    for monster in systems["fishing"]["sea_monsters"]["types"]:
        search_entries.append({"type": "entity", "id": monster["id"], "title": monster["names"], "url": "/skills/fishing/#sea-monsters"})
    for baron in systems["fighting"]["barons"]["types"]:
        search_entries.append({"type": "baron", "id": baron["id"], "title": baron["names"], "url": f"/skills/fighting/#baron-{baron['id']}"})
    for item in systems["fighting"]["legendary_items"]:
        search_entries.append({"type": "legendary_item", "id": item["id"], "title": item["names"], "url": "/skills/fighting/#legendary-items"})
    for event_category in systems["crafting"]["lucky_blocks"]["categories"]:
        for event in event_category["events"]:
            search_entries.append({"type": "lucky_event", "id": event["id"], "title": event["names"], "url": "/skills/crafting/#lucky-blocks"})

    encyclopedia = {
        "schema_version": catalog["schema_version"],
        "source": catalog["source"],
        "skills": [
            {
                "id": skill["id"],
                "names": skill["names"],
                "summary": {
                    locale: content.get("summary", "")
                    for locale, content in skill["editorial"].items()
                },
                "nodes": [
                    {"id": node["id"], "names": node["names"], "descriptions": node["descriptions"]}
                    for node in skill["nodes"]
                ],
            }
            for skill in skills
            if "encyclopedia" in skill.get("visibility", [])
        ],
        "values": [value for value in values if "encyclopedia" in value.get("audiences", [])],
        "systems": {
            "progression": {
                "max_level": systems["progression"]["max_level"],
                "node_unlock_cost": systems["progression"]["node_unlock_cost"],
            },
            "mining": {"fossil_families": systems["mining"]["fossils"]["families"]},
            "eating": {
                "dish_categories": systems["eating"]["dish_categories"],
                "dish_rarities": systems["eating"]["dish_rarities"],
            },
            "fishing": {
                "families": systems["fishing"]["families"],
                "rarities": systems["fishing"]["rarities"],
                "mini_games": systems["fishing"]["mini_games"],
            },
            "fighting": {
                "baron_types": [
                    {"id": baron["id"], "names": baron["names"], "summary": baron["behavior"]["summary"]}
                    for baron in systems["fighting"]["barons"]["types"]
                ],
            },
            "crafting": {
                "stations": systems["crafting"]["stations"],
                "recycling_groups": systems["crafting"]["recycling"]["groups"],
            },
        },
    }

    report = {
        "status": "ok" if not errors else "error",
        "counts": {
            "skills": len(skills),
            "perks": sum(len(skill["nodes"]) for skill in skills),
            "items": len(items),
            "blocks": len(blocks),
            "recipes": len(recipes),
            "documented_values": len(values),
            "search_entries": len(search_entries),
            "items_confirmed": sum(item["registration_status"] == "confirmed" for item in items),
            "items_dynamic_probable": sum(item["registration_status"] == "dynamic_probable" for item in items),
            "items_model_only": sum(item["registration_status"] == "model_only" for item in items),
            "fossil_families": len(systems["mining"]["fossils"]["families"]),
            "fossil_rarities": len(systems["mining"]["fossils"]["rarities"]),
            "cooking_recipes": len(systems["eating"]["cooking_recipes"]),
            "culinary_ingredients": len(systems["eating"]["ingredients"]) + len(systems["eating"]["dynamic_ingredient_sources"]),
            "fishing_families": len(systems["fishing"]["families"]),
            "fishing_rarities": len(systems["fishing"]["rarities"]),
            "sea_monsters": len(systems["fishing"]["sea_monsters"]["types"]),
            "baron_types": len(systems["fighting"]["barons"]["types"]),
            "baron_legendary_items": len(systems["fighting"]["legendary_items"]),
            "craft_score_items": len(systems["crafting"]["craft_score"]["item_points"]),
            "craft_transformations": len(systems["crafting"]["transformations"]["pairs"]),
            "craft_recycle_groups": len(systems["crafting"]["recycling"]["groups"]),
            "lucky_block_events": sum(len(category["events"]) for category in systems["crafting"]["lucky_blocks"]["categories"]),
        },
        "errors": errors,
        "warnings": warnings,
        "static_inspection_only": True,
    }

    site_data_root = project_root / "website/src/data/generated"
    write_json(output_root / "catalog.json", catalog)
    write_json(output_root / "search-index.json", search_entries)
    write_json(output_root / "encyclopedia.json", encyclopedia)
    write_json(output_root / "extraction-report.json", report)
    write_json(site_data_root / "catalog.json", catalog)
    write_json(site_data_root / "extraction-report.json", report)
    write_json(public_generated / "catalog.json", catalog)
    write_json(public_generated / "search-index.json", search_entries)
    write_json(public_generated / "extraction-report.json", report)
    return report
