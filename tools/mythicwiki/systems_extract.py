from __future__ import annotations

import ast
import re
from pathlib import Path
from typing import Any

from .utils import find_balanced_calls, parse_java_number, parse_java_string, read_text, split_top_level, strip_java_comments


def _translated(locales: dict[str, dict[str, str]], key: str, fallback: str) -> dict[str, str]:
    return {
        "fr": locales.get("fr_fr", {}).get(key, fallback),
        "en": locales.get("en_us", {}).get(key, fallback),
    }


def _safe_number(expression: str) -> int | float | None:
    direct = parse_java_number(expression)
    if direct is not None:
        return direct
    cleaned = expression.strip().replace("_", "")
    cleaned = re.sub(r"(?<=\d)[fFdDlL]\b", "", cleaned)
    if not re.fullmatch(r"[\d\s+\-*/().]+", cleaned):
        return None
    try:
        tree = ast.parse(cleaned, mode="eval")
    except SyntaxError:
        return None

    def evaluate(node: ast.AST) -> float:
        if isinstance(node, ast.Expression):
            return evaluate(node.body)
        if isinstance(node, ast.Constant) and isinstance(node.value, (int, float)):
            return float(node.value)
        if isinstance(node, ast.UnaryOp) and isinstance(node.op, (ast.UAdd, ast.USub)):
            value = evaluate(node.operand)
            return value if isinstance(node.op, ast.UAdd) else -value
        if isinstance(node, ast.BinOp) and isinstance(node.op, (ast.Add, ast.Sub, ast.Mult, ast.Div)):
            left = evaluate(node.left)
            right = evaluate(node.right)
            if isinstance(node.op, ast.Add):
                return left + right
            if isinstance(node.op, ast.Sub):
                return left - right
            if isinstance(node.op, ast.Mult):
                return left * right
            return left / right
        raise ValueError("unsupported expression")

    try:
        value = evaluate(tree)
    except (ValueError, ZeroDivisionError):
        return None
    return int(value) if value.is_integer() else value


def _enum_constants(path: Path, enum_name: str) -> list[tuple[str, list[str]]]:
    source = strip_java_comments(read_text(path))
    match = re.search(rf"\benum\s+{re.escape(enum_name)}\s*\{{", source)
    if not match:
        raise ValueError(f"Enum {enum_name} introuvable dans {path}")
    start = match.end()
    depth = 0
    state = "code"
    end = None
    index = start
    while index < len(source):
        char = source[index]
        next_char = source[index + 1] if index + 1 < len(source) else ""
        if state == "code":
            if char == '"':
                state = "string"
            elif char == "'":
                state = "char"
            elif char in "([{":
                depth += 1
            elif char in ")]}":
                depth -= 1
            elif char == ";" and depth == 0:
                end = index
                break
        elif state == "string":
            if char == "\\" and next_char:
                index += 1
            elif char == '"':
                state = "code"
        else:
            if char == "\\" and next_char:
                index += 1
            elif char == "'":
                state = "code"
        index += 1
    if end is None:
        raise ValueError(f"Fin des constantes de {enum_name} introuvable")

    entries: list[tuple[str, list[str]]] = []
    for raw_entry in split_top_level(source[start:end]):
        raw_entry = raw_entry.strip()
        if not raw_entry:
            continue
        constant_match = re.match(r"([A-Z][A-Z0-9_]*)\s*(?:\((.*)\))?$", raw_entry, re.S)
        if not constant_match:
            continue
        args = split_top_level(constant_match.group(2)) if constant_match.group(2) is not None else []
        entries.append((constant_match.group(1), args))
    return entries


def _constant(path: Path, symbol: str) -> int | float | bool | str | None:
    source = strip_java_comments(read_text(path))
    match = re.search(
        rf"\bstatic\s+final\s+[\w<>?,.\[\]]+\s+{re.escape(symbol)}\s*=\s*([^;]+);",
        source,
    )
    if not match:
        return None
    expression = match.group(1).strip()
    value = _safe_number(expression)
    if value is not None:
        return value
    string = parse_java_string(expression)
    if string is not None:
        return string
    if expression in {"true", "false"}:
        return expression == "true"
    return None


def _int_array(path: Path, symbol: str) -> list[int]:
    source = strip_java_comments(read_text(path))
    match = re.search(rf"\b{re.escape(symbol)}\s*=\s*\{{([^}}]+)\}}", source, re.S)
    if not match:
        return []
    values: list[int] = []
    for token in split_top_level(match.group(1)):
        value = _safe_number(token)
        if isinstance(value, (int, float)):
            values.append(int(value))
    return values


def extract_progression_system(java_root: Path, values: list[dict[str, Any]]) -> dict[str, Any]:
    by_id = {entry["id"]: entry for entry in values}
    coefficient = float(by_id["progression.xp_curve_coefficient"]["value"])
    exponent = float(by_id["progression.xp_curve_exponent"]["value"])
    max_level = int(by_id["progression.max_level"]["value"])
    unlock_cost = int(by_id["progression.node_unlock_cost"]["value"])
    levels: list[dict[str, int]] = []
    cumulative = 0
    for level in range(1, max_level + 1):
        required = max(1, round(coefficient * (level ** exponent))) if level < max_level else 0
        levels.append({"level": level, "required": required, "cumulative": cumulative})
        cumulative += required
    return {
        "formula": {
            "kind": "power_round_minimum",
            "coefficient": coefficient,
            "exponent": exponent,
            "minimum": 1,
            "expression": "max(1, round(coefficient × level^exponent))",
        },
        "max_level": max_level,
        "max_skill_points": int(by_id["progression.max_skill_points"]["value"]),
        "node_unlock_cost": unlock_cost,
        "points_per_level": 1,
        "levels": levels,
        "reference_levels": [10, 20, 30, 40, 50, 75, 100],
        "status": "beta",
        "extraction": {
            "method": "java_constants_and_formula_reproduction",
            "files": [
                "com/mythicrpg/core/SkillProgress.java",
                "com/mythicrpg/core/SkillTreeManager.java",
            ],
        },
    }


def extract_mining_system(java_root: Path, locales: dict[str, dict[str, str]]) -> dict[str, Any]:
    family_path = java_root / "com/mythicrpg/mining/archaeology/FossilFamily.java"
    rarity_path = java_root / "com/mythicrpg/mining/archaeology/FossilRarity.java"
    families: list[dict[str, Any]] = []
    for constant, args in _enum_constants(family_path, "FossilFamily"):
        family_id = parse_java_string(args[0]) if args else constant.lower()
        translation_key = parse_java_string(args[1]) if len(args) > 1 else f"family.mythicrpg.fossil.{family_id}"
        if not family_id:
            continue
        families.append({
            "id": family_id,
            "names": _translated(locales, translation_key or "", family_id.replace("_", " ").title()),
        })

    rarities: list[dict[str, Any]] = []
    for constant, args in _enum_constants(rarity_path, "FossilRarity"):
        if len(args) < 5:
            continue
        rarity_id = parse_java_string(args[0]) or constant.lower()
        rank = _safe_number(args[1])
        weight = _safe_number(args[2])
        cleaning_ticks = _safe_number(args[3])
        incubation_ticks = _safe_number(args[4])
        rarities.append({
            "id": rarity_id,
            "names": _translated(locales, f"rarity.mythicrpg.fossil.{rarity_id}", rarity_id.title()),
            "rank": int(rank or 0),
            "generation_weight": int(weight or 0),
            "generation_percent": float(weight or 0),
            "cleaning_ticks": int(cleaning_ticks or 0),
            "cleaning_seconds": round(float(cleaning_ticks or 0) / 20.0, 2),
            "incubation_ticks": int(incubation_ticks or 0),
            "incubation_minutes": round(float(incubation_ticks or 0) / 1200.0, 2),
        })

    site_path = java_root / "com/mythicrpg/mining/archaeology/FossilSiteGenerator.java"
    return {
        "fossils": {
            "families": families,
            "rarities": rarities,
            "site_generation": {
                "min_size": _constant(site_path, "MIN_SITE_SIZE"),
                "max_size": _constant(site_path, "MAX_SITE_SIZE"),
                "min_y": _constant(site_path, "MIN_GENERATION_Y"),
                "max_y": _constant(site_path, "MAX_GENERATION_Y"),
            },
        },
        "area_effects": {
            "vein_mining": {
                "max_extra_blocks": _constant(java_root / "com/mythicrpg/mining/MiningAreaEffects.java", "VEIN_MINING_MAX_BLOCKS"),
                "toggle_default": True,
                "server_authoritative": True,
            },
            "mining_3x3": {
                "extra_positions": 8,
                "independent_toggle": True,
                "server_authoritative": True,
            },
        },
        "extraction": {
            "method": "specialized_java_enum_and_constant_extraction",
            "files": [
                family_path.relative_to(java_root).as_posix(),
                rarity_path.relative_to(java_root).as_posix(),
                site_path.relative_to(java_root).as_posix(),
                "com/mythicrpg/mining/MiningAreaEffects.java",
            ],
        },
    }


def extract_eating_system(java_root: Path, locales: dict[str, dict[str, str]]) -> dict[str, Any]:
    rarity_path = java_root / "com/mythicrpg/eating/DishRarity.java"
    category_path = java_root / "com/mythicrpg/eating/DishCategory.java"
    recipe_path = java_root / "com/mythicrpg/eating/CookingRecipeRegistry.java"
    ingredient_path = java_root / "com/mythicrpg/eating/CulinaryIngredientRegistry.java"

    categories = [
        {
            "id": constant.lower(),
            "names": _translated(locales, f"dish_category.mythicrpg.{constant.lower()}", constant.title()),
        }
        for constant, _ in _enum_constants(category_path, "DishCategory")
    ]
    rarities: list[dict[str, Any]] = []
    for constant, args in _enum_constants(rarity_path, "DishRarity"):
        if len(args) < 2:
            continue
        rarity_id = constant.lower()
        rarities.append({
            "id": rarity_id,
            "names": _translated(locales, f"dish_rarity.mythicrpg.{rarity_id}", constant.title()),
            "rank": int(_safe_number(args[0]) or 0),
            "saturation": float(_safe_number(args[1]) or 0),
        })

    recipe_source = strip_java_comments(read_text(recipe_path))
    recipes: list[dict[str, Any]] = []
    for raw_call in find_balanced_calls(recipe_source, "fixed"):
        args = split_top_level(raw_call)
        recipe_id = parse_java_string(args[0]) if args else None
        if not recipe_id or len(args) < 4:
            continue
        category_match = re.search(r"DishCategory\.([A-Z_]+)", args[1])
        rarity_match = re.search(r"DishRarity\.([A-Z_]+)", args[2])
        ingredients = [
            {"item": f"minecraft:{item.lower()}", "category": category.lower()}
            for item, category in re.findall(
                r"ingredient\(\s*Items\.([A-Z0-9_]+)\s*,\s*FoodCategory\.([A-Z0-9_]+)\s*\)",
                raw_call,
            )
        ]
        recipes.append({
            "id": recipe_id,
            "kind": "fixed",
            "names": _translated(locales, f"dish.mythicrpg.{recipe_id}", recipe_id.replace("_", " ").title()),
            "category": category_match.group(1).lower() if category_match else "unknown",
            "rarity": rarity_match.group(1).lower() if rarity_match else "unknown",
            "shelf_life_days": int(_safe_number(args[3]) or 0),
            "ingredients": ingredients,
        })
    for raw_call in find_balanced_calls(recipe_source, "generic"):
        args = split_top_level(raw_call)
        recipe_id = parse_java_string(args[0]) if args else None
        if not recipe_id or len(args) < 3:
            continue
        category_match = re.search(r"DishCategory\.([A-Z_]+)", args[1])
        hints = re.findall(r'"([a-z0-9_]+)"', args[2])
        recipes.append({
            "id": recipe_id,
            "kind": "generic",
            "names": _translated(locales, f"dish.mythicrpg.{recipe_id}", recipe_id.replace("_", " ").title()),
            "category": category_match.group(1).lower() if category_match else "unknown",
            "rarity": "dynamic",
            "shelf_life_days": 2,
            "ingredient_hints": hints,
            "ingredients": [],
        })

    ingredient_source = strip_java_comments(read_text(ingredient_path))
    ingredients: list[dict[str, Any]] = []
    for raw_call in find_balanced_calls(ingredient_source, "register"):
        args = split_top_level(raw_call)
        if len(args) < 3:
            continue
        item_match = re.fullmatch(r"Items\.([A-Z0-9_]+)", args[0].strip())
        score = _safe_number(args[1])
        if not item_match or score is None:
            continue
        category_ids = [match.lower() for match in re.findall(r"\b([A-Z][A-Z0-9_]*)\(\)", ",".join(args[2:]))]
        item_id = item_match.group(1).lower()
        ingredients.append({
            "id": f"minecraft:{item_id}",
            "names": {"fr": item_id.replace("_", " ").title(), "en": item_id.replace("_", " ").title()},
            "score": int(score),
            "categories": category_ids,
        })

    food_categories = sorted({category for ingredient in ingredients for category in ingredient["categories"]})
    return {
        "dish_categories": categories,
        "dish_rarities": rarities,
        "food_categories": [
            {
                "id": category,
                "names": _translated(locales, f"food_category.mythicrpg.{category}", category.replace("_", " ").title()),
            }
            for category in food_categories
        ],
        "ingredients": ingredients,
        "cooking_recipes": sorted(recipes, key=lambda recipe: (recipe["kind"], recipe["category"], recipe["id"])),
        "dynamic_ingredient_sources": [
            {
                "id": "mythicrpg:fishing_catch",
                "description": {
                    "fr": "Les prises Fishing deviennent des ingrédients dynamiques selon leur famille et leur rareté.",
                    "en": "Fishing catches become dynamic ingredients based on their family and rarity.",
                },
            }
        ],
        "extraction": {
            "method": "specialized_java_registry_extraction",
            "files": [
                category_path.relative_to(java_root).as_posix(),
                rarity_path.relative_to(java_root).as_posix(),
                recipe_path.relative_to(java_root).as_posix(),
                ingredient_path.relative_to(java_root).as_posix(),
            ],
        },
    }


def extract_fishing_system(java_root: Path, locales: dict[str, dict[str, str]]) -> dict[str, Any]:
    family_path = java_root / "com/mythicrpg/fishing/FishingFamily.java"
    rarity_path = java_root / "com/mythicrpg/fishing/FishingRarity.java"
    balance_path = java_root / "com/mythicrpg/fishing/FishingBalance.java"
    monster_path = java_root / "com/mythicrpg/fishing/SeaMonsterType.java"

    families = [
        {
            "id": constant.lower(),
            "names": _translated(locales, f"fishing.family.mythicrpg.{constant.lower()}", constant.title()),
            "dimension_rule": (
                "nether_only" if constant == "INFERNAL" else "end_only" if constant == "VOID" else "overworld"
            ),
        }
        for constant, _ in _enum_constants(family_path, "FishingFamily")
    ]

    rarities: list[dict[str, Any]] = []
    for constant, args in _enum_constants(rarity_path, "FishingRarity"):
        if len(args) < 3:
            continue
        rarity_id = constant.lower()
        rarities.append({
            "id": rarity_id,
            "names": _translated(locales, f"rarity.mythicrpg.{rarity_id}", constant.title()),
            "rank": int(_safe_number(args[0]) or 0),
            "base_weight": int(_safe_number(args[1]) or 0),
            "xp": int(_safe_number(args[2]) or 0),
        })

    weight_sets = []
    for symbol, bait_id in (
        ("BASE_WEIGHTS", "none"),
        ("BAIT_I_WEIGHTS", "bait_i"),
        ("BAIT_II_WEIGHTS", "bait_ii"),
        ("BAIT_III_WEIGHTS", "bait_iii"),
        ("LEGENDARY_BAIT_WEIGHTS", "bait_legendary"),
    ):
        weights = _int_array(balance_path, symbol)
        total = sum(weights)
        weight_sets.append({
            "id": bait_id,
            "weights": {
                rarity["id"]: weights[index] if index < len(weights) else 0
                for index, rarity in enumerate(rarities)
            },
            "percentages": {
                rarity["id"]: round((weights[index] if index < len(weights) else 0) * 100 / total, 3) if total else 0
                for index, rarity in enumerate(rarities)
            },
            "source_symbol": symbol,
        })

    monsters: list[dict[str, Any]] = []
    materials = {"nessie": "nessie_scale", "megalodon": "megalodon_tooth", "whale": "whale_ambergris"}
    charms = {"nessie": "nessie_charm", "megalodon": "megalodon_charm", "whale": "whale_charm"}
    for constant, args in _enum_constants(monster_path, "SeaMonsterType"):
        if len(args) < 8:
            continue
        monster_id = constant.lower()
        weather_match = re.search(r"Mode\.([A-Z_]+)", args[0])
        monsters.append({
            "id": monster_id,
            "names": _translated(locales, f"sea_monster.mythicrpg.{monster_id}", constant.title()),
            "weather": weather_match.group(1).lower() if weather_match else "unknown",
            "max_health": float(_safe_number(args[1]) or 0),
            "attack_damage": float(_safe_number(args[2]) or 0),
            "attack_radius": float(_safe_number(args[3]) or 0),
            "attack_interval_ticks": int(_safe_number(args[4]) or 0),
            "attack_interval_seconds": round(float(_safe_number(args[4]) or 0) / 20.0, 2),
            "slime_size": int(_safe_number(args[5]) or 0),
            "horizontal_knockback": float(_safe_number(args[6]) or 0),
            "vertical_knockback": float(_safe_number(args[7]) or 0),
            "material_item": materials[monster_id],
            "charm_item": charms[monster_id],
            "title_id": f"special_sea_hunter_{monster_id}",
        })

    weather_path = java_root / "com/mythicrpg/fishing/FishingWeatherManager.java"
    sea_progress_path = java_root / "com/mythicrpg/fishing/SeaMonsterProgressData.java"
    sea_manager_path = java_root / "com/mythicrpg/fishing/SeaMonsterManager.java"
    return {
        "families": families,
        "rarities": rarities,
        "rarity_distributions": weight_sets,
        "rarity_rune_shifts": {"common": -5, "rare": -3, "epic": 4, "legendary": 3, "mythic": 1},
        "family_distribution": {
            "overworld_primary_percent": 75,
            "overworld_other_percent": 25,
            "nether": "infernal",
            "end": "void",
        },
        "mini_games": [
            {"rarity": "common", "game": "none"},
            {"rarity": "rare", "game": "none"},
            {"rarity": "epic", "game": "precision"},
            {"rarity": "legendary", "game": "cards"},
            {"rarity": "mythic", "game": "grid"},
        ],
        "weather": {
            "modes": ["rain", "sun", "storm"],
            "base_radius": _constant(weather_path, "BASE_RADIUS"),
            "harmonized_radius": _constant(weather_path, "HARMONIZED_RADIUS"),
            "base_duration_ticks": _constant(weather_path, "BASE_DURATION_TICKS"),
            "sealed_duration_ticks": _constant(weather_path, "SEALED_DURATION_TICKS"),
        },
        "sea_monsters": {
            "max_gauge": _constant(sea_progress_path, "MAX_GAUGE"),
            "normal_gauge_gain": _constant(sea_manager_path, "NORMAL_GAUGE_GAIN"),
            "sealed_gauge_gain": _constant(sea_manager_path, "SEALED_GAUGE_GAIN"),
            "owner_xp": 180 * 25,
            "assistant_xp": _constant(sea_manager_path, "ASSIST_XP"),
            "base_hook_damage": _constant(sea_manager_path, "BASE_HOOK_DAMAGE"),
            "sharpness_damage_per_level": _constant(sea_manager_path, "SHARPNESS_DAMAGE_PER_LEVEL"),
            "types": monsters,
        },
        "inventories": {
            "fish_net_max_slots": _constant(java_root / "com/mythicrpg/fishing/FishNetBlockEntity.java", "INVENTORY_SIZE"),
            "fishing_boat_capacity": _constant(java_root / "com/mythicrpg/fishing/FishingBoatScreenHandler.java", "CAPACITY"),
        },
        "extraction": {
            "method": "specialized_java_enum_array_and_constant_extraction",
            "files": [
                family_path.relative_to(java_root).as_posix(),
                rarity_path.relative_to(java_root).as_posix(),
                balance_path.relative_to(java_root).as_posix(),
                monster_path.relative_to(java_root).as_posix(),
                weather_path.relative_to(java_root).as_posix(),
                sea_progress_path.relative_to(java_root).as_posix(),
                sea_manager_path.relative_to(java_root).as_posix(),
            ],
        },
    }


def extract_skill_analysis(skill: dict[str, Any]) -> dict[str, Any]:
    nodes = skill["nodes"]
    node_ids = {node["id"] for node in nodes}
    child_ids: set[int] = set()
    bonus_types: set[str] = set()
    branch_counts: dict[str, int] = {}
    for node in nodes:
        child_ids.update(parent for parent in node["parent_ids"] if parent in node_ids)
        bonus_types.update(bonus["type"] for bonus in node.get("bonuses", []))
        key = f"{node['fork_id']}:{node['branch_id']}" if node["fork_id"] != -1 else "trunk"
        branch_counts[key] = branch_counts.get(key, 0) + 1
    roots = [node["id"] for node in nodes if not node["parent_ids"]]
    leaves = [node["id"] for node in nodes if node["id"] not in child_ids]
    return {
        "roots": roots,
        "leaves": leaves,
        "bonus_types": sorted(bonus_types),
        "branch_counts": branch_counts,
        "fork_count": len({node["fork_id"] for node in nodes if node["fork_id"] != -1}),
        "max_depth_hint": max((node["position"]["y"] for node in nodes), default=0),
    }
