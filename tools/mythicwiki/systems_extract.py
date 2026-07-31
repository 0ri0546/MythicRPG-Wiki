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



def _method_body(source: str, method_name: str) -> str:
    match = re.search(rf"\b{re.escape(method_name)}\s*\([^)]*\)\s*\{{", source)
    if not match:
        return ""
    start = match.end()
    depth = 1
    state = "code"
    index = start
    while index < len(source):
        char = source[index]
        next_char = source[index + 1] if index + 1 < len(source) else ""
        if state == "code":
            if char == '"':
                state = "string"
            elif char == "'":
                state = "char"
            elif char == "{":
                depth += 1
            elif char == "}":
                depth -= 1
                if depth == 0:
                    return source[start:index]
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
    return ""


def _initializer_source(path: Path, symbol: str) -> str:
    source = strip_java_comments(read_text(path))
    match = re.search(
        rf"\b{re.escape(symbol)}\s*=\s*(.*?);",
        source,
        re.S,
    )
    return match.group(1).strip() if match else ""


def _primitive_constants(path: Path) -> list[dict[str, Any]]:
    source = strip_java_comments(read_text(path))
    values: list[dict[str, Any]] = []
    pattern = re.compile(
        r"\bstatic\s+final\s+[\w<>?,.\[\]]+\s+([A-Z][A-Z0-9_]*)\s*=\s*([^;]+);",
        re.S,
    )
    for match in pattern.finditer(source):
        symbol = match.group(1)
        expression = match.group(2).strip()
        if symbol.startswith("COOLDOWN_") or symbol.endswith("_ID") or symbol.endswith("_TAG"):
            continue
        value: Any = _safe_number(expression)
        if value is None and expression in {"true", "false"}:
            value = expression == "true"
        if value is None and expression.startswith("{") and expression.endswith("}"):
            parsed = [_safe_number(token) for token in split_top_level(expression[1:-1])]
            if parsed and all(item is not None for item in parsed):
                value = parsed
        if value is None:
            continue
        unit = "value"
        if "CHANCE" in symbol or "RATIO" in symbol:
            unit = "ratio"
        elif symbol.endswith("_TICKS") or "INTERVAL_TICKS" in symbol or "DURATION_TICKS" in symbol:
            unit = "ticks"
        elif "RANGE" in symbol or "RADIUS" in symbol or "DISTANCE" in symbol:
            unit = "blocks"
        elif "DAMAGE" in symbol:
            unit = "damage"
        elif "SPEED" in symbol:
            unit = "speed"
        elif "HEALTH" in symbol:
            unit = "health"
        elif "COUNT" in symbol or symbol.startswith("MAX_") or symbol.startswith("MIN_"):
            unit = "count"
        entry: dict[str, Any] = {
            "symbol": symbol,
            "value": value,
            "unit": unit,
            "source": path.name,
        }
        if unit == "ticks" and isinstance(value, (int, float)):
            entry["seconds"] = round(float(value) / 20.0, 3)
        values.append(entry)
    return values


def _item_identifier(token: str) -> str:
    token = token.strip()
    match = re.search(r"(Items|ModItems|ModBlocks)\.([A-Z0-9_]+)", token)
    if not match:
        return token.lower()
    namespace = "minecraft" if match.group(1) == "Items" else "mythicrpg"
    return f"{namespace}:{match.group(2).lower()}"


def _localized_identifier(locales: dict[str, dict[str, str]], identifier: str) -> dict[str, str]:
    if ":" not in identifier:
        fallback = identifier.replace("_", " ").title()
        return {"fr": fallback, "en": fallback}
    namespace, local_id = identifier.split(":", 1)
    key_prefix = "block" if local_id in {"lucky_block", "infinite_crafting_table"} else "item"
    key = f"{key_prefix}.{namespace}.{local_id}"
    fallback = local_id.replace("_", " ").title()
    return _translated(locales, key, fallback)


def _list_enum_members(path: Path, symbol: str, enum_name: str) -> list[str]:
    expression = _initializer_source(path, symbol)
    return [value.lower() for value in re.findall(rf"{re.escape(enum_name)}\.([A-Z0-9_]+)", expression)]


def _piecewise_baron_chances(path: Path) -> list[dict[str, Any]]:
    source = strip_java_comments(read_text(path))
    body = _method_body(source, "getBaronChance")
    minimum_level = _constant(path, "MIN_FIGHTING_LEVEL")
    if isinstance(minimum_level, (int, float)):
        body = body.replace("MIN_FIGHTING_LEVEL", str(int(minimum_level)))
    tiers: list[dict[str, Any]] = []
    lower = 0
    for threshold, chance in re.findall(r"if\s*\(fightingLevel\s*<\s*(\d+)\)\s*\{\s*return\s+([0-9.]+);", body, re.S):
        upper = int(threshold) - 1
        tiers.append({"min_level": lower, "max_level": upper, "chance": float(chance)})
        lower = int(threshold)
    final = re.findall(r"return\s+([0-9.]+);", body)
    if final:
        tiers.append({"min_level": lower, "max_level": 100, "chance": float(final[-1])})
    return tiers


def _baron_entity_mapping(path: Path) -> tuple[dict[str, list[str]], list[str]]:
    source = strip_java_comments(read_text(path))
    lists: dict[str, list[str]] = {}
    for match in re.finditer(
        r"private\s+static\s+final\s+List<BaronType>\s+([A-Z0-9_]+)\s*=\s*List\.of\((.*?)\);",
        source,
        re.S,
    ):
        lists[match.group(1)] = [value.lower() for value in re.findall(r"BaronType\.([A-Z0-9_]+)", match.group(2))]

    type_to_entities: dict[str, list[str]] = {"normal": ["any_non_player_living_entity"]}
    body = _method_body(source, "getAvailableSpecialTypes")
    for match in re.finditer(r"if\s*\((.*?)\)\s*\{\s*return\s+([A-Z0-9_]+);\s*\}", body, re.S):
        classes = [name.replace("Entity", "").replace(" ", "").lower() for name in re.findall(r"instanceof\s+([A-Za-z0-9_]+)", match.group(1))]
        for baron_type in lists.get(match.group(2), []):
            type_to_entities.setdefault(baron_type, []).extend(classes)

    lucky_expression = _initializer_source(path, "LUCKY_BLOCK_BARON_ENTITY_TYPES")
    lucky_entities = [name.lower() for name in re.findall(r"EntityType\.([A-Z0-9_]+)", lucky_expression)]
    return type_to_entities, lucky_entities


def _reward_chance(source: str, token: str | None = None, method: str | None = None) -> float | None:
    target = re.escape(token) if token else rf"BaronRewardRegistry::{re.escape(method or '')}"
    patterns = [
        rf"roll\(world, entity,\s*([0-9.]+),\s*\(\)\s*->\s*new ItemStack\({target}",
        rf"roll\(world, entity,\s*([0-9.]+),\s*{target}\)",
        rf"roll\(world, entity,\s*([0-9.]+),\s*\(\)\s*->\s*{re.escape(method or '')}\(",
    ]
    for pattern in patterns:
        match = re.search(pattern, source)
        if match:
            return float(match.group(1))
    if token and re.search(rf"drop\(world, entity,\s*(?:\(\)\s*->\s*)?new ItemStack\({re.escape(token)}", source):
        return 1.0
    return None


def _baron_rewards(locales: dict[str, dict[str, str]], path: Path) -> dict[str, list[dict[str, Any]]]:
    source = strip_java_comments(read_text(path))

    def entry(identifier: str, chance: float | None, count: int = 1, note_fr: str = "", note_en: str = "") -> dict[str, Any]:
        return {
            "identifier": identifier,
            "names": _localized_identifier(locales, identifier),
            "chance": chance,
            "count": count,
            "note": {"fr": note_fr, "en": note_en},
        }

    specs: dict[str, list[dict[str, Any]]] = {
        "normal": [entry("mythicrpg:barons_doll", _reward_chance(source, "ModItems.BARONS_DOLL"))],
        "druid": [entry("minecraft:tipped_arrow", 1.0, 1, "Flèche de soin.", "Healing tipped arrow.")],
        "barrage": [entry("minecraft:arrow", 1.0, 8)],
        "nuke": [
            entry("minecraft:gunpowder", 1.0),
            entry("minecraft:fermented_spider_eye", _reward_chance(source, "Items.FERMENTED_SPIDER_EYE")),
        ],
        "survivor": [
            entry("minecraft:iron_ingot", _reward_chance(source, "Items.IRON_INGOT")),
            entry("minecraft:leather", _reward_chance(source, "Items.LEATHER")),
        ],
        "fugitive": [
            entry("minecraft:sugar", 1.0),
            entry("minecraft:rabbit_foot", _reward_chance(source, "Items.RABBIT_FOOT")),
        ],
        "golden": [entry("minecraft:golden_apple_or_emerald", 1.0, 1, "Une récompense garantie, puis une seconde selon le niveau Fighting de naissance.", "One guaranteed reward, then a second based on spawn Fighting level.")],
        "panic": [entry("minecraft:nausea_potion", _reward_chance(source, method="createNauseaPotion"))],
        "hothead": [
            entry("minecraft:fire_charge", _reward_chance(source, "Items.FIRE_CHARGE")),
            entry("mythicrpg:fire_wand", _reward_chance(source, "ModItems.FIRE_WAND")),
        ],
        "alchemist": [
            entry("minecraft:random_useful_potion", _reward_chance(source, method="createRandomUsefulPotion")),
            entry("minecraft:nether_wart", _reward_chance(source, "Items.NETHER_WART")),
        ],
        "giant": [entry("minecraft:slime_or_magma_resources", 1.0, 1, "La récompense dépend du type de Slime.", "Reward depends on the Slime type.")],
        "darknight": [entry("minecraft:night_vision_potion", 1.0)],
        "swimming": [
            entry("minecraft:prismarine_shard", _reward_chance(source, "Items.PRISMARINE_SHARD")),
            entry("minecraft:nautilus_shell", _reward_chance(source, "Items.NAUTILUS_SHELL")),
        ],
        "drowned_king": [entry("minecraft:riptide_trident", _reward_chance(source, method="createRiptideTrident"), 1, "Trident très endommagé avec Riptide I.", "Heavily damaged trident with Riptide I.")],
        "charging": [entry("minecraft:red_carpet", 1.0)],
        "balloon": [entry("minecraft:dragon_breath", _reward_chance(source, "Items.DRAGON_BREATH"))],
        "diamond": [
            entry("minecraft:phantom_membrane", _reward_chance(source, "Items.PHANTOM_MEMBRANE")),
            entry("minecraft:diamond", _reward_chance(source, "Items.DIAMOND")),
        ],
        "stalker": [
            entry("minecraft:wither_rose", _reward_chance(source, "Items.WITHER_ROSE")),
            entry("mythicrpg:wither_shield", _reward_chance(source, "ModItems.WITHER_SHIELD")),
        ],
        "heavy": [entry("minecraft:blast_protection_book", _reward_chance(source, method="createEnchantedBook"), 1, "Livre Solidité explosive I.", "Blast Protection I book.")],
        "molten": [entry("minecraft:magma_cream", _reward_chance(source, "Items.MAGMA_CREAM"))],
        "runner": [],
        "ink": [
            entry("minecraft:ink_sac", 1.0, 2),
            entry("minecraft:glow_ink_sac", _reward_chance(source, "Items.GLOW_INK_SAC")),
        ],
        "undying_wolf": [entry("special:taming", 1.0, 1, "La récompense est le Baron apprivoisé vivant.", "The reward is the living tamed Baron.")],
        "inferno": [entry("mythicrpg:heart_of_the_beam", _reward_chance(source, "ModItems.HEART_OF_THE_BEAM"))],
        "thrower": [entry("mythicrpg:spider_wand", _reward_chance(source, "ModItems.SPIDER_WAND"))],
    }
    return specs


_BARON_BEHAVIOR_TEXT: dict[str, dict[str, str]] = {
    "normal": {"fr": "Version renforcée sans capacité spéciale supplémentaire.", "en": "Strengthened version without an extra special ability."},
    "druid": {"fr": "Un squelette qui se soigne lorsqu’il touche directement un joueur.", "en": "A skeleton that heals itself when it directly hits a player."},
    "barrage": {"fr": "Un squelette qui déclenche régulièrement une attaque à distance supplémentaire.", "en": "A skeleton that regularly triggers an additional ranged attack."},
    "nuke": {"fr": "Un zombie qui libère à sa mort un nuage d’effet négatif choisi aléatoirement.", "en": "A zombie that releases a random harmful effect cloud on death."},
    "survivor": {"fr": "Un zombie immunisé aux dégâts qui ne proviennent pas d’un coup de mêlée direct d’un joueur.", "en": "A zombie immune to damage that is not direct player melee damage."},
    "fugitive": {"fr": "Une créature passive extrêmement mobile qui reçoit un bonus de vitesse permanent.", "en": "A highly mobile passive creature with a permanent speed bonus."},
    "golden": {"fr": "Une créature passive lumineuse avec une récompense garantie et une chance de seconde récompense.", "en": "A glowing passive creature with one guaranteed reward and a chance for a second."},
    "panic": {"fr": "Une créature passive qui accélère et saute davantage lorsqu’elle est frappée.", "en": "A passive creature that gains speed and jump boost when hit."},
    "giant": {"fr": "Un Slime dont la taille est doublée à la promotion, dans la limite prévue par le code.", "en": "A Slime whose size is doubled on promotion up to the coded limit."},
    "darknight": {"fr": "Une araignée qui recherche activement le joueur valide le plus proche lorsqu’elle n’a plus de cible.", "en": "A spider that actively acquires the nearest valid player when it has no target."},
    "alchemist": {"fr": "Une sorcière qui applique périodiquement un effet négatif aléatoire à sa cible visible.", "en": "A witch that periodically applies a random harmful effect to a visible target."},
    "hothead": {"fr": "Un Blaze qui projette des salves circulaires de projectiles enflammés.", "en": "A Blaze that fires circular bursts of flaming projectiles."},
    "swimming": {"fr": "Un Enderman adapté à l’eau, immunisé aux dégâts liés à l’eau et capable de poursuivre une cible immergée.", "en": "A water-adapted Enderman immune to water damage and able to chase submerged targets."},
    "drowned_king": {"fr": "Un Noyé équipé qui charge sa cible dans l’eau et inflige un impact avec recul.", "en": "An equipped Drowned that charges targets in water and deals a knockback impact."},
    "balloon": {"fr": "Un Ghast dont les boules de feu créent des nuages persistants infligeant des dégâts de zone.", "en": "A Ghast whose fireballs create persistent area-damage clouds."},
    "charging": {"fr": "Un Ravageur ou Hoglin qui prépare puis exécute une charge rapide avec dégâts d’impact.", "en": "A Ravager or Hoglin that winds up and performs a fast damaging charge."},
    "diamond": {"fr": "Un Vex équipé d’une épée en diamant non récupérable.", "en": "A Vex equipped with a non-droppable diamond sword."},
    "stalker": {"fr": "Un Wither Skeleton dont la taille diminue avec le niveau Fighting ayant servi à sa création.", "en": "A Wither Skeleton whose size decreases with the Fighting level used at spawn."},
    "heavy": {"fr": "Un Creeper possédant une résistance totale au recul ajoutée lors de la promotion.", "en": "A Creeper granted complete knockback resistance on promotion."},
    "molten": {"fr": "Un Golem de fer immunisé au feu, à la lave, à la noyade et à plusieurs dégâts environnementaux.", "en": "An Iron Golem immune to fire, lava, drowning and several environmental damage sources."},
    "runner": {"fr": "Un Creeper doté d’un effet de vitesse permanent et d’un multiplicateur de déplacement renforcé.", "en": "A Creeper with permanent speed and an increased movement multiplier."},
    "ink": {"fr": "Un Calmar qui aveugle l’attaquant lorsqu’il subit un coup, selon son temps de recharge.", "en": "A Squid that blinds its attacker when hit, subject to a cooldown."},
    "undying_wolf": {"fr": "Un loup apprivoisable qui ne peut pas mourir une fois domestiqué et doit récupérer avant de reprendre le combat.", "en": "A tameable wolf that cannot die once tamed and must recover before fighting again."},
    "inferno": {"fr": "Un Guardian qui verrouille un rayon sur un joueur et augmente progressivement les dégâts répétés.", "en": "A Guardian that locks a beam onto a player and progressively increases repeated damage."},
    "thrower": {"fr": "Une araignée qui recherche une autre créature, la saisit après une préparation puis la projette vers le joueur.", "en": "A spider that finds another mob, winds up, then throws it toward the player."},
}


def extract_fighting_system(java_root: Path, locales: dict[str, dict[str, str]]) -> dict[str, Any]:
    type_path = java_root / "com/mythicrpg/fighting/BaronType.java"
    manager_path = java_root / "com/mythicrpg/fighting/BaronMobManager.java"
    events_path = java_root / "com/mythicrpg/fighting/FightingEvents.java"
    scaling_path = java_root / "com/mythicrpg/fighting/barons/BaronScaling.java"
    rewards_path = java_root / "com/mythicrpg/fighting/barons/BaronRewardRegistry.java"
    type_to_entities, lucky_entities = _baron_entity_mapping(manager_path)
    rewards = _baron_rewards(locales, rewards_path)

    events_source = strip_java_comments(read_text(events_path))
    grant_xp_body = _method_body(events_source, "grantSkillXp")
    normal_xp_match = re.search(
        r"Math\.max\((\d+),\s*Math\.min\((\d+),\s*Math\.round\(xpHealthBasis\s*/\s*([0-9.]+)f?\)\)\)",
        grant_xp_body,
    )
    baron_xp_match = re.search(
        r"Math\.round\(xpGained\s*\*\s*([0-9.]+)\)\s*\+\s*(\d+)",
        grant_xp_body,
    )
    if not normal_xp_match or not baron_xp_match:
        raise ValueError("Unable to extract Fighting XP formulas")
    normal_min, normal_max = int(normal_xp_match.group(1)), int(normal_xp_match.group(2))
    health_divisor = float(normal_xp_match.group(3))
    baron_base_multiplier, baron_flat_bonus = float(baron_xp_match.group(1)), int(baron_xp_match.group(2))

    baseline_body = _method_body(events_source, "baselineXpForMob")
    baseline_returns = [int(value) for value in re.findall(r"return\s+(\d+);", baseline_body)]
    positive_baselines = [value for value in baseline_returns if value > 0]

    manager_source = strip_java_comments(read_text(manager_path))
    special_type_chance = _require_regex_number(manager_source, r"random\.nextDouble\(\)\s*<\s*([0-9.]+)", "Baron normal-vs-special chance")
    maximum_entity_age = int(_require_regex_number(manager_source, r"livingEntity\.age\s*>\s*(\d+)", "Baron maximum entity age"))

    behavior_class_by_type = {
        "druid": "DruidBaronBehavior.java", "barrage": "BarrageBaronBehavior.java",
        "nuke": "NukeBaronBehavior.java", "survivor": "SurvivorBaronBehavior.java",
        "panic": "PanicBaronBehavior.java", "giant": "GiantBaronBehavior.java",
        "darknight": "DarknightBaronBehavior.java", "alchemist": "AlchemistBaronBehavior.java",
        "hothead": "HotheadBaronBehavior.java", "swimming": "SwimmingBaronBehavior.java",
        "drowned_king": "DrownedKingBaronBehavior.java", "balloon": "BalloonBaronBehavior.java",
        "charging": "ChargingBaronBehavior.java", "diamond": "DiamondBaronBehavior.java",
        "stalker": "StalkerBaronBehavior.java", "heavy": "HeavyBaronBehavior.java",
        "molten": "MoltenBaronBehavior.java", "runner": "RunnerBaronBehavior.java",
        "ink": "InkBaronBehavior.java", "undying_wolf": "UndyingWolfBaronBehavior.java",
        "inferno": "InfernoBaronBehavior.java", "thrower": "ThrowerBaronBehavior.java",
    }

    baron_types: list[dict[str, Any]] = []
    for constant, args in _enum_constants(type_path, "BaronType"):
        baron_id = constant.lower()
        tag = parse_java_string(args[0]) if args else f"mythicrpg_baron_{baron_id}"
        behavior_file = behavior_class_by_type.get(baron_id)
        behavior_path = java_root / "com/mythicrpg/fighting/barons" / behavior_file if behavior_file else None
        baron_types.append({
            "id": baron_id,
            "names": _translated(locales, f"baron.mythicrpg.{baron_id}", baron_id.replace("_", " ").title()),
            "tag": tag,
            "base_entities": sorted(set(type_to_entities.get(baron_id, []))),
            "behavior": {
                "summary": _BARON_BEHAVIOR_TEXT.get(baron_id, {"fr": "Comportement géré par les règles communes des Barons.", "en": "Behavior handled by the common Baron rules."}),
                "file": behavior_path.relative_to(java_root).as_posix() if behavior_path and behavior_path.is_file() else manager_path.relative_to(java_root).as_posix(),
                "constants": _primitive_constants(behavior_path) if behavior_path and behavior_path.is_file() else [],
            },
            "rewards": rewards.get(baron_id, []),
        })

    health_per_level = float(_constant(scaling_path, "HEALTH_PER_LEVEL") or 0)
    damage_per_level = float(_constant(scaling_path, "DAMAGE_PER_LEVEL") or 0)
    xp_per_level = float(_constant(scaling_path, "XP_REWARD_PER_LEVEL") or 0)
    scaling_samples = [
        {
            "level": level,
            "health_multiplier": round(1 + level * health_per_level, 3),
            "damage_multiplier": round(1 + level * damage_per_level, 3),
            "xp_multiplier": round(1 + level * xp_per_level, 3),
        }
        for level in (0, 10, 25, 50, 75, 100)
    ]

    legendary_specs = [
        ("fire_wand", "FireWandItem.java", "Baguette ciblée qui enflamme une créature.", "Targeted wand that sets a creature on fire."),
        ("wither_shield", "LegendaryShieldItem.java", "Bouclier légendaire dont le blocage en mêlée applique Wither via les effets serveur.", "Legendary shield whose melee block applies Wither through server effects."),
        ("heart_of_the_beam", "HeartOfTheBeamItem.java", "Déclenche un rayon en trois étapes sur une cible visible.", "Starts a three-stage beam on a visible target."),
        ("spider_wand", "SpiderWandItem.java", "Place une toile à distance et consomme une ficelle hors mode créatif.", "Places a cobweb at range and consumes string outside creative mode."),
        ("barons_doll", "BaronsDollItem.java", "Crée un leurre temporaire reprenant l’armure du joueur et attirant ses agresseurs.", "Creates a temporary decoy using the player's armor values and retargeting attackers."),
    ]
    legendary_items = []
    for item_id, filename, fr_summary, en_summary in legendary_specs:
        item_path = java_root / "com/mythicrpg/fighting/items" / filename
        constants = _primitive_constants(item_path)
        if item_id in {"wither_shield", "heart_of_the_beam", "barons_doll"}:
            constants += _primitive_constants(java_root / "com/mythicrpg/fighting/items/BaronLegendaryItemEffects.java")
        legendary_items.append({
            "id": item_id,
            "identifier": f"mythicrpg:{item_id}",
            "names": _localized_identifier(locales, f"mythicrpg:{item_id}"),
            "summary": {"fr": fr_summary, "en": en_summary},
            "constants": constants,
            "source_file": item_path.relative_to(java_root).as_posix(),
        })

    return {
        "xp": {
            "normal_formula": f"clamp(round(max_health / {health_divisor:g}), {normal_min}, {normal_max})",
            "baron_formula": f"round((round(base_xp × {baron_base_multiplier:g}) + {baron_flat_bonus}) × baron_xp_multiplier)",
            "health_divisor": health_divisor,
            "normal_min": normal_min,
            "normal_max": normal_max,
            "baron_base_multiplier": baron_base_multiplier,
            "baron_flat_bonus": baron_flat_bonus,
            "vanilla_mob_xp_estimates": [
                {"group": "common_hostile", "xp": min(positive_baselines), "approximate": True},
                {"group": "blaze_or_guardian", "xp": max(positive_baselines), "approximate": True},
            ] if positive_baselines else [],
            "source_file": events_path.relative_to(java_root).as_posix(),
        },
        "barons": {
            "promotion": {
                "minimum_fighting_level_for_multiplayer_protection": _constant(manager_path, "MIN_FIGHTING_LEVEL"),
                "player_search_radius": _constant(manager_path, "PLAYER_SEARCH_RADIUS"),
                "low_level_protection_radius": _constant(manager_path, "LOW_LEVEL_PROTECTION_RADIUS"),
                "generic_scale_bonus": _constant(manager_path, "BARON_SCALE_BONUS"),
                "special_type_chance": special_type_chance,
                "chance_tiers": _piecewise_baron_chances(manager_path),
                "checked_once_per_entity": True,
                "maximum_entity_age_ticks": maximum_entity_age,
                "lucky_block_forced_entity_types": lucky_entities,
            },
            "scaling": {
                "min_level": _constant(scaling_path, "MIN_LEVEL"),
                "max_level": _constant(scaling_path, "MAX_LEVEL"),
                "health_per_level": health_per_level,
                "damage_per_level": damage_per_level,
                "xp_reward_per_level": xp_per_level,
                "samples": scaling_samples,
            },
            "types": baron_types,
        },
        "legendary_items": legendary_items,
        "multiplayer": {
            "promotion_authority": "server",
            "damage_authority": "server",
            "reward_authority": "server",
            "nearest_player_sets_spawn_level": True,
            "low_level_nearby_player_can_block_promotion": True,
        },
        "extraction": {
            "method": "specialized_java_baron_and_reward_extraction",
            "files": [
                type_path.relative_to(java_root).as_posix(),
                manager_path.relative_to(java_root).as_posix(),
                events_path.relative_to(java_root).as_posix(),
                scaling_path.relative_to(java_root).as_posix(),
                rewards_path.relative_to(java_root).as_posix(),
            ],
        },
    }


def _craft_score_values(path: Path) -> list[dict[str, Any]]:
    source = strip_java_comments(read_text(path))
    entries = []
    for item, points in re.findall(r"value\(Items\.([A-Z0-9_]+),\s*(-?\d+)\)", source):
        entries.append({"identifier": f"minecraft:{item.lower()}", "points": int(points)})
    return entries


def _map_item_pairs(path: Path) -> list[dict[str, str]]:
    source = strip_java_comments(read_text(path))
    return [
        {"input": f"minecraft:{source_item.lower()}", "output": f"minecraft:{output_item.lower()}"}
        for source_item, output_item in re.findall(r"map\.put\(Items\.([A-Z0-9_]+),\s*Items\.([A-Z0-9_]+)\)", source)
    ]


def _recycle_groups(path: Path) -> list[dict[str, Any]]:
    source = strip_java_comments(read_text(path))
    groups: list[dict[str, Any]] = []
    for method_name, material in (
        ("isWoodenTool", "wood"), ("isStoneTool", "stone"),
        ("isIronToolOrArmor", "iron"), ("isGoldToolOrArmor", "gold"),
    ):
        body = _method_body(source, method_name)
        result_match = re.search(r"resultItem\s*==\s*Items\.([A-Z0-9_]+)", body)
        inputs = [f"minecraft:{item.lower()}" for item in re.findall(r"inputItem\s*==\s*Items\.([A-Z0-9_]+)", body)]
        groups.append({
            "id": material,
            "result": f"minecraft:{result_match.group(1).lower()}" if result_match else "unknown",
            "inputs": inputs,
        })
    return groups


def _weighted_lucky_events(path: Path, symbol: str) -> list[dict[str, Any]]:
    expression = _initializer_source(path, symbol)
    return [
        {"id": method, "weight": int(weight)}
        for weight, method in re.findall(r"new\s+WeightedLuckyEvent\(\s*(\d+)\s*,\s*LuckyBlockEventManager::([A-Za-z0-9_]+)\s*\)", expression)
    ]


def _lucky_event_names(event_id: str) -> dict[str, str]:
    labels = {
        "randomOreDrop": ("Minerai aléatoire", "Random ore"),
        "mythicResourceDrop": ("Ressource MythicRPG", "MythicRPG resource"),
        "randomSkillSpark": ("Étincelle de skill", "Skill spark"),
        "boundDiamondArmor": ("Armure en diamant maudite", "Bound diamond armor"),
        "redstoneEngineerPack": ("Pack d’ingénieur redstone", "Redstone engineer pack"),
        "coinTossBlessed": ("Pile ou face bénéfique", "Blessed coin toss"),
        "templeVariant": ("Temple aléatoire", "Random temple"),
        "nothingHappens": ("Rien ne se passe", "Nothing happens"),
        "luckyBlockBlink": ("Téléportation du Bloc chanceux", "Lucky Block blink"),
        "visitAFriend": ("Visite chez un joueur", "Visit a friend"),
        "impossibleChoice": ("Choix impossible", "Impossible choice"),
        "uselessBlastProtectionStick": ("Bâton Protection contre les explosions", "Blast Protection stick"),
        "rainbowSheep": ("Mouton arc-en-ciel", "Rainbow sheep"),
        "minorCurse": ("Malédiction mineure", "Minor curse"),
        "chickenJockeySquad": ("Escouade de jockeys", "Chicken jockey squad"),
        "fallingAnvil": ("Enclume tombante", "Falling anvil"),
        "safeTnt": ("TNT sécurisée", "Safe TNT"),
        "coinTossCursed": ("Pile ou face maudit", "Cursed coin toss"),
        "skyTrial": ("Épreuve aérienne", "Sky trial"),
        "baronRitual": ("Rituel de Baron", "Baron ritual"),
        "shuffleInventory": ("Inventaire mélangé", "Shuffled inventory"),
    }
    fr, en = labels.get(event_id, (event_id, event_id))
    return {"fr": fr, "en": en}


def _lucky_infusion_rules(path: Path) -> list[dict[str, Any]]:
    source = strip_java_comments(read_text(path))
    body = _method_body(source, "getDeltaForItem")
    rules: list[dict[str, Any]] = []
    for condition, delta in re.findall(r"if\s*\((.*?)\)\s*\{\s*return\s+(-?\d+);\s*\}", body, re.S):
        items = [f"minecraft:{item.lower()}" for item in re.findall(r"item\s*==\s*Items\.([A-Z0-9_]+)", condition)]
        if items:
            rules.append({"delta": int(delta), "items": items})
    return rules



def _perk_id_for_bonus(path: Path, bonus_type: str) -> int:
    source = strip_java_comments(read_text(path))
    pattern = re.compile(
        r"nodes\.put\((\d+),\s*new\s+SkillTreeNode\(.*?bonus\(BonusType\.([A-Z0-9_]+),",
        re.S,
    )
    matches = {bonus: int(node_id) for node_id, bonus in pattern.findall(source)}
    if bonus_type not in matches:
        raise ValueError(f"Unable to find perk for BonusType.{bonus_type} in {path.name}")
    return matches[bonus_type]


def _require_regex_number(source: str, pattern: str, label: str, *, group: int = 1) -> float:
    match = re.search(pattern, source, re.S)
    if not match:
        raise ValueError(f"Unable to extract {label}")
    return float(match.group(group))


def extract_crafting_system(java_root: Path, locales: dict[str, dict[str, str]]) -> dict[str, Any]:
    base = java_root / "com/mythicrpg/crafting"
    xp_path = base / "CraftXpManager.java"
    score_path = base / "CraftScoreManager.java"
    charge_path = base / "CraftChargeManager.java"
    mastery_path = base / "CraftMasteryManager.java"
    portable_state_path = base / "PortableCraftingState.java"
    table_state_path = base / "station/CraftingTableDurabilityState.java"
    station_path = base / "station/CraftingStationType.java"
    screen_path = base / "MythicCraftingScreenHandler.java"
    recycle_path = base / "RecycleCraftManager.java"
    transformation_path = base / "TransformationSlotManager.java"
    lucky_path = base / "LuckyBlockEventManager.java"
    luck_manager_path = base / "LuckyBlockLuckManager.java"
    lucky_block_path = base / "LuckyBlock.java"
    infusion_path = base / "LuckyInfusionManager.java"
    skill_tree_path = base / "CraftingSkillTree.java"
    reinforced_path = base / "ReinforcedCraftManager.java"

    xp_source = strip_java_comments(read_text(xp_path))
    night_match = re.search(r"time\s*>=\s*(\d+)L?\s*&&\s*time\s*<=\s*(\d+)L?", xp_source)
    if not night_match:
        raise ValueError("Unable to extract Crafting night window")
    night_start, night_end = (int(night_match.group(1)), int(night_match.group(2)))

    charge_source = strip_java_comments(read_text(charge_path))
    charge_threshold = int(_require_regex_number(charge_source, r"newCharge\s*>=\s*([0-9.]+)", "Craft Charge completion threshold"))

    reinforced_source = strip_java_comments(read_text(reinforced_path))
    unbreaking_level = int(_require_regex_number(reinforced_source, r"addEnchantment\(unbreaking\.get\(\),\s*(\d+)\)", "reinforced craft enchantment level"))

    infusion_source = strip_java_comments(read_text(infusion_path))
    infusion_count = int(_require_regex_number(infusion_source, r"infusionSlots\s*!=\s*(\d+)", "Lucky infusion ingredient count"))

    mastery_source = strip_java_comments(read_text(mastery_path))
    mastery_targets = [skill.lower() for skill in dict.fromkeys(re.findall(r"Optional\.of\(SkillType\.([A-Z_]+)\)", mastery_source)) if skill != "CRAFTING"]

    lucky_source = strip_java_comments(read_text(lucky_path))
    neutral_match = re.search(r"Math\.abs\(clampedLuck\)\s*>=\s*(\d+)\s*\?\s*(\d+)\s*:\s*(\d+)", lucky_source)
    positive_match = re.search(r"positiveChance\s*=\s*(\d+)\s*\+\s*\(clampedLuck\s*\*\s*(\d+)\)", lucky_source)
    if not neutral_match or not positive_match:
        raise ValueError("Unable to extract Lucky Block category formula")
    neutral_threshold, neutral_extreme, neutral_standard = map(int, neutral_match.groups())
    positive_base, positive_step = map(int, positive_match.groups())

    stations = []
    for constant, args in _enum_constants(station_path, "CraftingStationType"):
        station_id = constant.lower()
        station_names = {
            "portable": {"fr": "Fabrication portable", "en": "Portable crafting"},
            "vanilla_table": {"fr": "Table de fabrication", "en": "Crafting table"},
            "infinite_table": {"fr": "Table de fabrication infinie", "en": "Infinite crafting table"},
        }
        stations.append({
            "id": station_id,
            "names": station_names.get(station_id, {"fr": station_id, "en": station_id}),
            "numeric_id": int(_safe_number(args[0]) or 0) if args else 0,
            "finite_durability": args[1].strip() == "true" if len(args) > 1 else True,
            "max_durability": (
                int(_constant(portable_state_path, "MAX_DURABILITY") or 0) if station_id == "portable"
                else int(_constant(table_state_path, "MAX_DURABILITY") or 0) if station_id == "vanilla_table"
                else None
            ),
        })

    score_entries = _craft_score_values(score_path)
    score_source = strip_java_comments(read_text(score_path))
    mythic_default_match = re.search(r'if\s*\("mythicrpg"\.equals\(id\.getNamespace\(\)\)\)\s*\{\s*return\s+(\d+);', score_source, re.S)
    fallback_returns = re.findall(r"return\s+(\d+);", score_source)
    non_mythic_default = int(fallback_returns[-1]) if fallback_returns else 1

    lucky_categories = []
    category_symbols = (("positive", "POSITIVE_EVENTS"), ("neutral", "NEUTRAL_EVENTS"), ("negative", "NEGATIVE_EVENTS"))
    for category, symbol in category_symbols:
        events = _weighted_lucky_events(lucky_path, symbol)
        total = sum(event["weight"] for event in events)
        for event in events:
            event["names"] = _lucky_event_names(event["id"])
            event["within_category_percent"] = round(event["weight"] * 100 / total, 3) if total else 0
        lucky_categories.append({"id": category, "events": events, "total_weight": total})

    chance_samples = []
    for luck in (-10, -9, -5, 0, 5, 9, 10):
        neutral = neutral_extreme if abs(luck) >= neutral_threshold else neutral_standard
        positive = max(0, positive_base + luck * positive_step)
        negative = max(0, 100 - neutral - positive)
        total = positive + neutral + negative
        chance_samples.append({
            "luck": luck,
            "positive": round(positive * 100 / total, 3) if total else 0,
            "neutral": round(neutral * 100 / total, 3) if total else 0,
            "negative": round(negative * 100 / total, 3) if total else 0,
        })

    ore_expression = _initializer_source(lucky_path, "ORE_POOL")
    ore_pool = [
        {"identifier": f"minecraft:{item.lower()}", "weight": int(weight)}
        for item, weight in re.findall(r"new\s+WeightedItem\(Items\.([A-Z0-9_]+),\s*(\d+)\)", ore_expression)
    ]

    transformation_pairs = _map_item_pairs(transformation_path)
    for pair in transformation_pairs:
        pair["input_names"] = _localized_identifier(locales, pair["input"])
        pair["output_names"] = _localized_identifier(locales, pair["output"])

    recycle_groups = _recycle_groups(recycle_path)
    for group in recycle_groups:
        group["result_names"] = _localized_identifier(locales, group["result"])
        group["input_names"] = [_localized_identifier(locales, item) for item in group["inputs"]]

    screen_constants = {
        entry["symbol"].lower(): entry["value"]
        for entry in _primitive_constants(screen_path)
        if isinstance(entry["value"], (int, float))
    }

    return {
        "xp": {
            "score_multiplier": _constant(xp_path, "CRAFT_XP_MULTIPLIER"),
            "max_per_action": _constant(xp_path, "MAX_XP_PER_CRAFT_ACTION"),
            "formula": "clamp(floor(total_craft_score × multiplier), 1, max_per_action)",
            "processing_order": ["base_score", "midnight_workshop", "mythic_inspiration", "first_craft_bonus"],
            "night_window": {"start_tick": night_start, "end_tick": night_end},
            "midnight_multiplier": _constant(xp_path, "MIDNIGHT_WORKSHOP_MULTIPLIER"),
            "green_vanilla_xp_ratio": _constant(xp_path, "GREEN_CRAFTING_VANILLA_XP_RATIO"),
            "green_vanilla_xp_cap": _constant(xp_path, "MAX_GREEN_CRAFTING_VANILLA_XP"),
        },
        "craft_score": {
            "item_points": score_entries,
            "mythicrpg_item_default": int(mythic_default_match.group(1)) if mythic_default_match else 100,
            "other_item_default": non_mythic_default,
            "excluded_systems": ["recycling", "lucky_infusion", "blocked_conversion_loops", "decorative_spam", "storage_or_building_loops"],
        },
        "stations": stations,
        "interface": {
            "property_count": screen_constants.get("property_count"),
            "craft_result_slot": screen_constants.get("craft_result_slot"),
            "craft_input_start": screen_constants.get("craft_input_start"),
            "craft_input_end": screen_constants.get("craft_input_end"),
            "transformation_input_slot": screen_constants.get("transformation_input_slot"),
            "transformation_output_slot": screen_constants.get("transformation_output_slot"),
        },
        "bonuses": {
            "portable_durability": _constant(portable_state_path, "MAX_DURABILITY"),
            "table_durability": _constant(table_state_path, "MAX_DURABILITY"),
            "repair_kit_base_power": _constant(base / "RepairKitItem.java", "BASE_REPAIR_POWER"),
            "resource_save": {"handled_server_side": True},
            "reinforced_craft": {"applies_unbreaking_level": unbreaking_level, "handled_server_side": True},
            "craft_charge": {
                "charge_per_craft_xp": _constant(charge_path, "CHARGE_PER_CRAFT_XP"),
                "max_charge_per_craft": _constant(charge_path, "MAX_CHARGE_GAIN_PER_CRAFT"),
                "completion_threshold": charge_threshold,
                "bonus_next_level_ratio": _constant(charge_path, "BONUS_NEXT_LEVEL_RATIO"),
            },
            "mythic_inspiration_multiplier": _constant(base / "MythicInspirationManager.java", "MYTHIC_INSPIRATION_MULTIPLIER"),
            "first_craft_multiplier": _constant(base / "FirstCraftBonusManager.java", "FIRST_CRAFT_MULTIPLIER"),
            "experience_charm_ratio": _constant(base / "ExpCharmBonusManager.java", "BONUS_RATIO"),
            "craft_mastery": {
                "transfer_ratio": _constant(mastery_path, "CRAFT_XP_TRANSFER_RATIO"),
                "target_level_cap_ratio": _constant(mastery_path, "TARGET_SKILL_LEVEL_CAP_RATIO"),
                "target_skills": mastery_targets,
            },
        },
        "recycling": {
            "groups": recycle_groups,
            "requires_perk": _perk_id_for_bonus(skill_tree_path, "RECYCLE_CRAFTS"),
            "grants_crafting_xp": False,
        },
        "transformations": {
            "pairs": transformation_pairs,
            "requires_perk": _perk_id_for_bonus(skill_tree_path, "TRANSFORMATION_SLOT"),
            "charge_per_item": 1,
            "charge_source_expression": "tryConsumeCharges(player, transformAmount)",
        },
        "lucky_blocks": {
            "luck_min": _constant(luck_manager_path, "MIN_LUCK"),
            "luck_max": _constant(luck_manager_path, "MAX_LUCK"),
            "block_state_offset": _constant(lucky_block_path, "LUCK_OFFSET"),
            "category_formula": {
                "positive": f"max(0, {positive_base} + luck × {positive_step})",
                "neutral": f"{neutral_extreme} if |luck| >= {neutral_threshold} else {neutral_standard}",
                "negative": "max(0, 100 - neutral - positive)",
            },
            "category_parameters": {
                "positive_base": positive_base,
                "positive_step": positive_step,
                "neutral_threshold": neutral_threshold,
                "neutral_extreme": neutral_extreme,
                "neutral_standard": neutral_standard,
            },
            "chance_samples": chance_samples,
            "categories": lucky_categories,
            "ore_pool": ore_pool,
            "infusion_rules": _lucky_infusion_rules(infusion_path),
            "infusion_ingredient_count": infusion_count,
            "craft_perk": _perk_id_for_bonus(skill_tree_path, "LUCKY_BLOCK_CRAFT"),
            "infusion_perk": _perk_id_for_bonus(skill_tree_path, "LUCKY_INFUSION"),
            "break_event_authority": "server",
        },
        "multiplayer": {
            "craft_result_authority": "server",
            "persistent_player_states": ["portable_durability", "craft_charge", "first_crafts"],
            "lucky_block_authority": "server",
            "portable_interface_is_client_view": True,
        },
        "extraction": {
            "method": "specialized_java_crafting_extraction",
            "files": [
                xp_path.relative_to(java_root).as_posix(), score_path.relative_to(java_root).as_posix(),
                charge_path.relative_to(java_root).as_posix(), mastery_path.relative_to(java_root).as_posix(),
                station_path.relative_to(java_root).as_posix(), recycle_path.relative_to(java_root).as_posix(),
                transformation_path.relative_to(java_root).as_posix(), lucky_path.relative_to(java_root).as_posix(),
                infusion_path.relative_to(java_root).as_posix(),
                skill_tree_path.relative_to(java_root).as_posix(), reinforced_path.relative_to(java_root).as_posix(),
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
