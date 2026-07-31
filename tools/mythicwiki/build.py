from __future__ import annotations

import json
import shutil
import unicodedata
from pathlib import Path
from typing import Any

import yaml

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
from .v040_extract import (
    extract_traveling_system,
    extract_building_system,
    extract_farming_system,
    extract_woodcutting_system,
)
from .utils import tree_sha256, write_json



def _normalized_search(value: str) -> str:
    normalized = unicodedata.normalize("NFKD", value.casefold())
    return "".join(character for character in normalized if not unicodedata.combining(character))


def _apply_perk_icons(project_root: Path, skills: list[dict[str, Any]], items: list[dict[str, Any]]) -> dict[str, int]:
    config_path = project_root / "config/perk_icons.yaml"
    config = yaml.safe_load(config_path.read_text(encoding="utf-8")) or {}
    defaults = config.get("defaults") or {}
    rules = config.get("rules") or []
    item_lookup = {item["id"]: item for item in items}
    counts = {"specific": 0, "fallback": 0, "missing_texture": 0}
    for skill in skills:
        default_id = str(defaults.get(skill["id"], ""))
        for node in skill["nodes"]:
            haystack = _normalized_search(" ".join([
                node["names"].get("fr", ""),
                node["names"].get("en", ""),
                node["descriptions"].get("fr", ""),
                node["descriptions"].get("en", ""),
                " ".join(bonus.get("type", "") for bonus in node.get("bonuses", [])),
            ]))
            chosen_id = default_id
            mapping_kind = "fallback"
            matched_terms: list[str] = []
            for rule in rules:
                if rule.get("skill") != skill["id"]:
                    continue
                terms = [str(term) for term in rule.get("terms", [])]
                if any(_normalized_search(term) in haystack for term in terms):
                    chosen_id = str(rule.get("icon", default_id))
                    mapping_kind = "specific"
                    matched_terms = terms
                    break
            item = item_lookup.get(chosen_id)
            fallback_name = chosen_id.replace("_", " ").title() if chosen_id else skill["names"]["fr"]
            node["icon"] = {
                "item_id": chosen_id or None,
                "identifier": item["identifier"] if item else None,
                "texture": item["texture"] if item else None,
                "names": item["names"] if item else {"fr": fallback_name, "en": fallback_name},
                "mapping": mapping_kind,
                "matched_terms": matched_terms,
                "source": "config/perk_icons.yaml",
            }
            counts[mapping_kind] += 1
            if not node["icon"]["texture"]:
                counts["missing_texture"] += 1
    return counts

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
    recipes = extract_recipes(resources_root, locales, items, project_root / "config/recipe_tag_variants.yaml")
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

    perk_icon_counts = _apply_perk_icons(project_root, skills, items)

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
        "traveling": extract_traveling_system(java_root, locales),
        "building": extract_building_system(java_root, locales),
        "farming": extract_farming_system(java_root, locales),
        "woodcutting": extract_woodcutting_system(java_root, locales),
    }

    catalog = {
        "schema_version": "0.4.1",
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
    for mount in systems["traveling"]["mounts"]["types"]:
        search_entries.append({"type": "mount", "id": mount["id"], "title": mount["names"], "url": "/skills/traveling/#mounts"})
    for module in systems["traveling"]["tools"]["monumental_compass"]["modules"]:
        search_entries.append({"type": "structure_module", "id": module["id"], "title": module["names"], "url": "/skills/traveling/#structure-modules"})
    for slab in systems["building"]["decorative_content"]["vertical_slabs"]["types"]:
        search_entries.append({"type": "block", "id": slab["id"], "title": slab["names"], "url": "/skills/building/#vertical-slabs"})
    for module in systems["woodcutting"]["chest_modules"]["tiers"]:
        search_entries.append({"type": "item", "id": module["id"], "title": module["names"], "url": "/skills/woodcutting/#chest-modules"})

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
            "traveling": {
                "mounts": [{"id": mount["id"], "names": mount["names"], "flying": mount["flying"]} for mount in systems["traveling"]["mounts"]["types"]],
                "vehicles": systems["traveling"]["vehicles"],
            },
            "building": {
                "plans": systems["building"]["plans"],
                "decorative_content": {
                    "vertical_slab_count": len(systems["building"]["decorative_content"]["vertical_slabs"]["types"]),
                    "static_effect_count": systems["building"]["decorative_content"]["static_decoration"]["effect_count"],
                },
            },
            "farming": {
                "harvest_categories": systems["farming"]["xp"]["harvest_categories"],
                "objects": systems["farming"]["objects"],
            },
            "woodcutting": {
                "xp": systems["woodcutting"]["xp"],
                "chest_module_tiers": systems["woodcutting"]["chest_modules"]["tiers"],
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
            "recipes_shaped": sum(recipe["visual"]["kind"] == "shaped" for recipe in recipes),
            "recipes_shapeless": sum(recipe["visual"]["kind"] == "shapeless" for recipe in recipes),
            "recipes_with_visuals": sum(bool(recipe.get("visual")) for recipe in recipes),
            "perk_icons_specific": perk_icon_counts["specific"],
            "perk_icons_fallback": perk_icon_counts["fallback"],
            "perk_icons_missing_texture": perk_icon_counts["missing_texture"],
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
            "traveling_mounts": len(systems["traveling"]["mounts"]["types"]),
            "traveling_structure_modules": len(systems["traveling"]["tools"]["monumental_compass"]["modules"]),
            "building_xp_blocks": sum(len(group["blocks"]) for group in systems["building"]["xp"]["block_groups"]),
            "building_vertical_slabs": len(systems["building"]["decorative_content"]["vertical_slabs"]["types"]),
            "building_static_effects": systems["building"]["decorative_content"]["static_decoration"]["effect_count"],
            "farming_harvest_categories": len(systems["farming"]["xp"]["harvest_categories"]),
            "farming_living_field_blocks": len(systems["farming"]["growth"]["living_field"]["affected_blocks"]),
            "woodcutting_chest_modules": len(systems["woodcutting"]["chest_modules"]["tiers"]),
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
