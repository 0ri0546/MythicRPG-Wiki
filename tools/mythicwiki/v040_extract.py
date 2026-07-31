from __future__ import annotations

import re
from pathlib import Path
from typing import Any

from .systems_extract import (
    _constant,
    _enum_constants,
    _localized_identifier,
    _method_body,
    _safe_number,
    _translated,
)
from .java_extract import extract_skill_tree
from .utils import (
    find_balanced_calls,
    parse_java_string,
    read_text,
    split_top_level,
    strip_java_comments,
)


def _source(path: Path) -> str:
    return strip_java_comments(read_text(path))


def _minecraft_id(token: str) -> str:
    token = token.strip()
    for owner, namespace in (
        ("Blocks", "minecraft"),
        ("Items", "minecraft"),
        ("ModBlocks", "mythicrpg"),
        ("ModItems", "mythicrpg"),
    ):
        match = re.search(rf"\b{owner}\.([A-Z0-9_]+)", token)
        if match:
            return f"{namespace}:{match.group(1).lower()}"
    structure = re.search(r"\bStructureKeys\.([A-Z0-9_]+)", token)
    if structure:
        return f"minecraft:{structure.group(1).lower()}"
    return token.strip().lower()


def _token_list(source: str, owner: str) -> list[str]:
    namespace = "minecraft" if owner in {"Blocks", "Items"} else "mythicrpg"
    return [f"{namespace}:{value.lower()}" for value in re.findall(rf"\b{owner}\.([A-Z0-9_]+)", source)]


def _constant_value(path: Path, symbol: str, fallback: Any = None) -> Any:
    value = _constant(path, symbol)
    return fallback if value is None else value


def _seconds(ticks: int | float | None) -> float | None:
    if ticks is None:
        return None
    return round(float(ticks) / 20.0, 3)


def _millis_to_seconds(milliseconds: int | float | None) -> float | None:
    if milliseconds is None:
        return None
    return round(float(milliseconds) / 1000.0, 3)


def _translation_or_identifier(locales: dict[str, dict[str, str]], identifier: str) -> dict[str, str]:
    return _localized_identifier(locales, identifier)


def _skill_tree_nodes(path: Path) -> list[dict[str, Any]]:
    return extract_skill_tree(path, path.parent.name)


def _bonus_values(path: Path, bonus_type: str) -> list[int | float]:
    target = bonus_type.lower()
    values: list[int | float] = []
    for node in _skill_tree_nodes(path):
        for bonus in node.get("bonuses", []):
            if bonus.get("type") != target:
                continue
            numeric = float(bonus.get("value", 0))
            values.append(int(numeric) if numeric.is_integer() else numeric)
    return values


def _first_bonus_value(path: Path, bonus_type: str, fallback: Any = None) -> Any:
    values = _bonus_values(path, bonus_type)
    return values[0] if values else fallback


def _perk_number(path: Path, bonus_type: str) -> int | None:
    target = bonus_type.lower()
    for node in _skill_tree_nodes(path):
        if any(bonus.get("type") == target for bonus in node.get("bonuses", [])):
            return int(node["id"])
    return None

def _bonus_entries(path: Path, bonus_type: str) -> list[dict[str, int | float]]:
    target = bonus_type.lower()
    entries: list[dict[str, int | float]] = []
    for node in _skill_tree_nodes(path):
        for bonus in node.get("bonuses", []):
            if bonus.get("type") != target:
                continue
            numeric = float(bonus.get("value", 0))
            entries.append({
                "perk": int(node["id"]),
                "value": int(numeric) if numeric.is_integer() else numeric,
            })
    return entries

def _structure_xp(path: Path) -> list[dict[str, Any]]:
    source = _source(path)
    entries: list[dict[str, Any]] = []
    for call in find_balanced_calls(source, "put"):
        args = split_top_level(call)
        if len(args) < 3 or args[0].strip() != "values":
            continue
        xp = _safe_number(args[1])
        if xp is None:
            continue
        for token in args[2:]:
            match = re.search(r"StructureKeys\.([A-Z0-9_]+)", token)
            if match:
                entries.append({"id": match.group(1).lower(), "xp": int(xp)})
    return entries


def _structure_modules(path: Path, locales: dict[str, dict[str, str]]) -> list[dict[str, Any]]:
    source = _source(path)
    modules: list[dict[str, Any]] = []
    for call in find_balanced_calls(source, "register"):
        args = split_top_level(call)
        module_id = parse_java_string(args[0]) if args else None
        if not module_id or len(args) < 3:
            continue
        realm_match = re.search(r"Realm\.([A-Z_]+)", args[1])
        structures = [match.lower() for match in re.findall(r"StructureKeys\.([A-Z0-9_]+)", ",".join(args[2:]))]
        key = f"item.mythicrpg.structure_module.{module_id}"
        modules.append({
            "id": module_id,
            "names": _translated(locales, key, module_id.replace("_", " ").title()),
            "realm": realm_match.group(1).lower() if realm_match else "unknown",
            "structures": structures,
        })
    return modules


def _mount_healing_items(path: Path) -> dict[str, list[str]]:
    body = _method_body(_source(path), "isHealingItem")
    result: dict[str, list[str]] = {}
    for case_names, expression in re.findall(r"case\s+([A-Z0-9_,\s]+?)\s*->\s*(.*?);", body, re.S):
        items = _token_list(expression, "Items")
        tags = [f"#minecraft:{tag.lower()}" for tag in re.findall(r"ItemTags\.([A-Z0-9_]+)", expression)]
        values = list(dict.fromkeys(items + tags))
        for case_name in re.findall(r"[A-Z][A-Z0-9_]*", case_names):
            result[case_name.lower()] = values
    return result


def extract_traveling_system(java_root: Path, locales: dict[str, dict[str, str]]) -> dict[str, Any]:
    base = java_root / "com/mythicrpg/traveling"
    xp_config = base / "TravelingXpConfig.java"
    xp_manager = base / "TravelingXpManager.java"
    xp_source_path = base / "TravelingXpSource.java"
    mount_path = base / "LandMountType.java"
    skill_tree = base / "TravelingSkillTree.java"

    xp_sources = []
    for constant, args in _enum_constants(xp_source_path, "TravelingXpSource"):
        discovery = args and args[0].strip() == "true"
        xp_sources.append({"id": constant.lower(), "discovery": bool(discovery)})

    healing = _mount_healing_items(mount_path)
    mounts: list[dict[str, Any]] = []
    for constant, args in _enum_constants(mount_path, "LandMountType"):
        if len(args) < 5:
            continue
        mount_id = parse_java_string(args[0]) or constant.lower()
        saddle_key = parse_java_string(args[1]) or f"item.mythicrpg.{mount_id}_saddle"
        bonus_match = re.search(r"BonusType\.([A-Z0-9_]+)", args[2])
        perk_key = parse_java_string(args[3]) or ""
        mounts.append({
            "id": mount_id,
            "names": _translated(locales, f"mount.mythicrpg.{mount_id}", mount_id.replace("_", " ").title()),
            "saddle_identifier": f"mythicrpg:{mount_id}_saddle",
            "saddle_names": _translated(locales, saddle_key, f"Selle {mount_id.replace('_', ' ')}"),
            "required_bonus": bonus_match.group(1).lower() if bonus_match else "unknown",
            "required_perk_names": _translated(locales, perk_key, perk_key),
            "flying": args[4].strip() == "true",
            "healing_items": healing.get(constant.lower(), []),
        })

    structures = _structure_xp(xp_config)
    movement_xp = int(_constant_value(xp_config, "MOVEMENT_XP", 0))
    treasure_xp = int(_constant_value(xp_config, "TREASURE_CHEST_XP", 0))
    treasure_vanilla = int(_constant_value(xp_config, "TREASURE_VANILLA_XP", 0))
    biome_ticks = int(_constant_value(xp_config, "BIOME_SPEED_DURATION_TICKS", 0))

    double_jump_path = base / "TravelingDoubleJumpManager.java"
    grapple_path = base / "GrapplingHookConfig.java"
    compass_path = base / "TravelingCompassManager.java"
    recall_path = base / "TravelingDeathRecallManager.java"
    miniature_path = base / "TravelingMiniaturizationManager.java"
    mount_manager_path = base / "LandMountManager.java"
    flying_path = base / "FlyingMountConfig.java"
    perk_manager_path = base / "TravelingPerkManager.java"

    return {
        "xp": {
            "sources": xp_sources,
            "movement": {
                "xp": movement_xp,
                "traveled_distance_required": _constant_value(xp_config, "MOVEMENT_DISTANCE_REQUIRED"),
                "direct_distance_required": _constant_value(xp_config, "MOVEMENT_DIRECT_DISTANCE_REQUIRED"),
                "cell_size": _constant_value(xp_config, "MOVEMENT_CELL_SIZE"),
                "teleport_distance_per_tick": _constant_value(xp_manager, "TELEPORT_DISTANCE_PER_TICK"),
                "minimum_tracked_movement": _constant_value(xp_manager, "MIN_TRACKED_MOVEMENT"),
                "vehicle_exclusion_present": False,
                "vehicle_note_confidence": "static_inference_from_player_position_tracking",
            },
            "dimensions": [
                {"id": "nether", "xp": int(_constant_value(xp_config, "NETHER_FIRST_VISIT_XP", 0))},
                {"id": "end", "xp": int(_constant_value(xp_config, "END_FIRST_VISIT_XP", 0))},
                {"id": "other", "xp": int(_constant_value(xp_config, "OTHER_DIMENSION_FIRST_VISIT_XP", 0))},
                {"id": "overworld_start", "xp": 0},
            ],
            "structures": structures,
            "treasure": {
                "skill_xp": treasure_xp,
                "vanilla_xp_with_perk": treasure_vanilla,
            },
            "multipliers": {
                "travel_xp_perks": _bonus_values(skill_tree, "TRAVEL_XP_MULTIPLIER"),
                "discovery_xp_perk": _first_bonus_value(skill_tree, "TRAVEL_DISCOVERY_XP_MULTIPLIER"),
                "source": skill_tree.relative_to(java_root).as_posix(),
            },
        },
        "movement_perks": {
            "double_jump": {
                "perk": _perk_number(skill_tree, "TRAVEL_DOUBLE_JUMP"),
                "vertical_velocity": _constant_value(double_jump_path, "DOUBLE_JUMP_VERTICAL_VELOCITY"),
                "minimum_air_ticks": _constant_value(double_jump_path, "MIN_AIR_TICKS_BEFORE_DOUBLE_JUMP"),
                "server_validated": True,
                "client_input_payload": True,
            },
            "miniaturization": {
                "perk": _perk_number(skill_tree, "TRAVEL_MINIATURIZATION"),
                "scale_modifier": _constant_value(miniature_path, "SCALE_MODIFIER"),
                "validation_interval_ticks": _constant_value(miniature_path, "VALIDATION_INTERVAL_TICKS"),
            },
            "biome_speed": {
                "perk": _perk_number(skill_tree, "TRAVEL_BIOME_SPEED"),
                "duration_ticks": biome_ticks,
                "duration_seconds": _seconds(biome_ticks),
            },
            "soul_walker": {
                "perk": _perk_number(skill_tree, "TRAVEL_SOUL_WALKER"),
                "movement_bonus": _constant_value(perk_manager_path, "SOUL_SPEED_MOVEMENT_BONUS"),
                "efficiency_bonus": _constant_value(perk_manager_path, "SOUL_SPEED_EFFICIENCY_BONUS"),
            },
        },
        "mounts": {
            "types": mounts,
            "land_count": sum(not mount["flying"] for mount in mounts),
            "flying_count": sum(mount["flying"] for mount in mounts),
            "adoption": {
                "maximum_health_ratio": _constant_value(mount_manager_path, "MAX_ADOPTION_HEALTH_RATIO"),
                "adoption_heal_ratio": _constant_value(mount_manager_path, "ADOPTION_HEAL_RATIO"),
                "heal_amount": _constant_value(mount_manager_path, "HEAL_AMOUNT"),
                "unmounted_wander_radius": _constant_value(mount_manager_path, "UNMOUNTED_WANDER_RADIUS"),
                "return_to_anchor_speed": _constant_value(mount_manager_path, "RETURN_TO_ANCHOR_SPEED"),
            },
            "flying_config": {
                "phantom_cruise_speed": _constant_value(flying_path, "PHANTOM_CRUISE_SPEED"),
                "phantom_ground_speed": _constant_value(flying_path, "PHANTOM_GROUND_SPEED"),
                "phantom_ascend_speed": _constant_value(flying_path, "PHANTOM_ASCEND_SPEED"),
                "phantom_descend_speed": _constant_value(flying_path, "PHANTOM_DESCEND_SPEED"),
                "minimum_cruise_speed": _constant_value(flying_path, "MIN_CRUISE_SPEED"),
                "maximum_cruise_speed": _constant_value(flying_path, "MAX_CRUISE_SPEED"),
                "turn_degrees_per_tick": _constant_value(flying_path, "TURN_DEGREES_PER_TICK"),
                "anchor_return_speed": _constant_value(flying_path, "ANCHOR_RETURN_SPEED"),
            },
        },
        "vehicles": {
            "traveler_boat": {
                "identifier": "mythicrpg:traveler_boat",
                "speed_multiplier": _constant_value(base / "TravelerBoatEntity.java", "SPEED_MULTIPLIER"),
                "perk": _perk_number(skill_tree, "FAST_BOAT_CRAFT"),
            },
            "traveler_minecart": {
                "identifier": "mythicrpg:traveler_minecart",
                "speed_multiplier": _constant_value(base / "TravelerMinecartEntity.java", "SPEED_MULTIPLIER"),
                "perk": _perk_number(skill_tree, "FAST_MINECART_CRAFT"),
            },
            "fishing_boat": {
                "identifier": "mythicrpg:fishing_boat",
                "traveling_xp_via_movement_tracker": True,
                "confidence": "static_inference_from_no_vehicle_exclusion",
            },
        },
        "tools": {
            "grappling_hook": {
                "identifier": "mythicrpg:grappling_hook",
                "perk": _perk_number(skill_tree, "GRAPPLING_HOOK_CRAFT"),
                "max_range_blocks": _constant_value(grapple_path, "MAX_RANGE_BLOCKS"),
                "travel_speed_blocks_per_second": _constant_value(grapple_path, "TRAVEL_SPEED_BLOCKS_PER_SECOND"),
                "arrival_distance": _constant_value(grapple_path, "ARRIVAL_DISTANCE"),
                "maximum_stalled_ticks": _constant_value(grapple_path, "MAX_STALLED_TICKS"),
                "post_arrival_fall_protection_ticks": _constant_value(grapple_path, "POST_ARRIVAL_FALL_PROTECTION_TICKS"),
                "safe_position_search_distance": _constant_value(grapple_path, "SAFE_POSITION_SEARCH_DISTANCE"),
            },
            "monumental_compass": {
                "perk": _perk_number(skill_tree, "MONUMENTAL_COMPASS_CRAFT"),
                "generic_search_radius_chunks": _constant_value(compass_path, "GENERIC_SEARCH_RADIUS_CHUNKS"),
                "module_search_radius_chunks": _constant_value(compass_path, "MODULE_SEARCH_RADIUS_CHUNKS"),
                "navigation_update_interval_ticks": _constant_value(compass_path, "NAVIGATION_UPDATE_INTERVAL_TICKS"),
                "search_cooldown_millis": _constant_value(compass_path, "SEARCH_COOLDOWN_MILLIS"),
                "search_cooldown_seconds": _millis_to_seconds(_constant_value(compass_path, "SEARCH_COOLDOWN_MILLIS")),
                "arrival_radius_blocks": _constant_value(compass_path, "ARRIVAL_RADIUS_BLOCKS"),
                "modules": _structure_modules(base / "StructureModuleRegistry.java", locales),
            },
            "death_recall": {
                "identifier": "mythicrpg:death_recall_token",
                "perk": _perk_number(skill_tree, "TRAVEL_DEATH_RECALL"),
                "token_lifetime_millis": _constant_value(recall_path, "TOKEN_LIFETIME_MILLIS"),
                "token_lifetime_seconds": _millis_to_seconds(_constant_value(recall_path, "TOKEN_LIFETIME_MILLIS")),
                "use_cooldown_millis": _constant_value(recall_path, "USE_COOLDOWN_MILLIS"),
                "use_cooldown_seconds": _millis_to_seconds(_constant_value(recall_path, "USE_COOLDOWN_MILLIS")),
                "safe_horizontal_radius": _constant_value(recall_path, "SAFE_HORIZONTAL_RADIUS"),
                "safe_vertical_radius": _constant_value(recall_path, "SAFE_VERTICAL_RADIUS"),
                "owner_bound": True,
                "dimension_persisted": True,
            },
        },
        "multiplayer": {
            "xp_authority": "server",
            "movement_state_persistent_per_player": True,
            "mount_owner_data_persistent": True,
            "compass_search_state_server_side": True,
            "grappling_motion_server_side": True,
            "client_payloads_are_inputs_or_visuals": True,
        },
        "extraction": {
            "method": "specialized_java_traveling_extraction",
            "files": [
                xp_config.relative_to(java_root).as_posix(), xp_manager.relative_to(java_root).as_posix(),
                xp_source_path.relative_to(java_root).as_posix(), mount_path.relative_to(java_root).as_posix(),
                (base / "StructureModuleRegistry.java").relative_to(java_root).as_posix(),
                grapple_path.relative_to(java_root).as_posix(), compass_path.relative_to(java_root).as_posix(),
                recall_path.relative_to(java_root).as_posix(), flying_path.relative_to(java_root).as_posix(),
                skill_tree.relative_to(java_root).as_posix(),
            ],
        },
    }


def _building_xp_catalog(path: Path, locales: dict[str, dict[str, str]]) -> list[dict[str, Any]]:
    source = _source(path)
    groups: list[dict[str, Any]] = []
    for call in find_balanced_calls(source, "register"):
        args = split_top_level(call)
        if not args:
            continue
        xp = _safe_number(args[0])
        if xp is None:
            continue
        blocks = [_minecraft_id(token) for token in args[1:] if "Blocks." in token]
        groups.append({
            "xp": int(xp),
            "blocks": blocks,
            "block_names": [_translation_or_identifier(locales, block) for block in blocks],
        })
    return groups


def _vertical_slabs(path: Path, locales: dict[str, dict[str, str]]) -> list[dict[str, Any]]:
    source = _source(path)
    entries: list[dict[str, Any]] = []
    pattern = re.compile(
        r"public\s+static\s+final\s+Block\s+([A-Z0-9_]+)\s*=\s*register\(\s*\"([^\"]+)\"\s*,\s*Blocks\.([A-Z0-9_]+)\s*\)",
        re.S,
    )
    for _, slab_id, base_block in pattern.findall(source):
        identifier = f"mythicrpg:{slab_id}"
        entries.append({
            "id": slab_id,
            "identifier": identifier,
            "names": _translation_or_identifier(locales, identifier),
            "base_block": f"minecraft:{base_block.lower()}",
            "base_names": _translation_or_identifier(locales, f"minecraft:{base_block.lower()}"),
        })
    return entries


def _blank_materials(path: Path, locales: dict[str, dict[str, str]]) -> list[dict[str, Any]]:
    source = _source(path)
    blocks: list[str] = []
    for call in find_balanced_calls(source, "register"):
        args = split_top_level(call)
        for token in args:
            if "Blocks." in token:
                blocks.append(_minecraft_id(token))
    unique = list(dict.fromkeys(blocks))
    return [{"identifier": block, "names": _translation_or_identifier(locales, block)} for block in unique]


def _static_effects(path: Path, locales: dict[str, dict[str, str]]) -> list[dict[str, Any]]:
    source = _source(path)
    body = _method_body(source, "intervalTicks")
    interval_by_constant: dict[str, int] = {}
    default = 6
    default_match = re.search(r"default\s*->\s*(\d+)", body)
    if default_match:
        default = int(default_match.group(1))
    for names, ticks in re.findall(r"case\s+([A-Z0-9_,\s]+?)\s*->\s*(\d+)", body):
        for constant in re.findall(r"[A-Z][A-Z0-9_]*", names):
            interval_by_constant[constant] = int(ticks)
    effects: list[dict[str, Any]] = []
    for constant, args in _enum_constants(path, "StaticDecorationEffect"):
        effect_id = parse_java_string(args[0]) if args else constant.lower()
        effects.append({
            "id": effect_id,
            "names": _translated(locales, f"static_decoration.mythicrpg.{effect_id}", effect_id.replace("_", " ").title()),
            "interval_ticks": interval_by_constant.get(constant, default),
            "particles_per_emission": 1,
        })
    return effects


def extract_building_system(java_root: Path, locales: dict[str, dict[str, str]]) -> dict[str, Any]:
    base = java_root / "com/mythicrpg/building"
    xp_path = base / "BuildingXpManager.java"
    catalog_path = base / "BuildingBlockCatalog.java"
    skill_tree = base / "BuildingSkillTree.java"
    plan2d = base / "BuildingPlan2DManager.java"
    plan3d = base / "BuildingPlan3DManager.java"
    magnet = base / "BuildingMagnetManager.java"
    reserve = base / "BuildingReserveChestManager.java"
    compass = base / "ArchitectCompassData.java"
    vertical_path = base / "VerticalSlabRegistry.java"
    blank_path = base / "BlankBlockMaterialRegistry.java"
    static_path = base / "StaticDecorationEffect.java"
    mod_items = java_root / "com/mythicrpg/core/ModItems.java"

    xp_source = _source(xp_path)
    position_body = _method_body(xp_source, "positionMultiplier")
    reuse_values = [float(value) for value in re.findall(r"case\s+\d+\s*->\s*([0-9.]+)D", position_body)]
    default_match = re.search(r"default\s*->\s*([0-9.]+)D", position_body)
    if default_match:
        reuse_values.append(float(default_match.group(1)))
    wand_source = _source(mod_items)
    wand_damage_match = re.search(r"new\s+BuilderWandItem\(.*?\.maxDamage\((\d+)\)", wand_source, re.S)

    xp_groups = _building_xp_catalog(catalog_path, locales)
    vertical_slabs = _vertical_slabs(vertical_path, locales)
    blank_materials = _blank_materials(blank_path, locales)
    static_effects = _static_effects(static_path, locales)

    return {
        "xp": {
            "block_groups": xp_groups,
            "eligible_block_count": sum(len(group["blocks"]) for group in xp_groups),
            "custom_blocks_can_register": True,
            "anti_exploitation": {
                "maximum_position_history": _constant_value(xp_path, "MAX_POSITION_HISTORY"),
                "maximum_material_history": _constant_value(xp_path, "MAX_MATERIAL_HISTORY"),
                "position_expiry_millis": _constant_value(xp_path, "POSITION_EXPIRY_MILLIS"),
                "position_expiry_minutes": round(float(_constant_value(xp_path, "POSITION_EXPIRY_MILLIS", 0)) / 60000.0, 2),
                "position_reuse_multipliers": reuse_values,
                "material_decay": _constant_value(xp_path, "MATERIAL_DECAY"),
                "material_recovery_per_intervening_event": _constant_value(xp_path, "MATERIAL_RECOVERY"),
                "minimum_material_multiplier": _constant_value(xp_path, "MIN_MATERIAL_MULTIPLIER"),
                "persistent_per_player": True,
                "bounded_history": True,
            },
        },
        "comfort": {
            "quick_replace": {
                "perk": _perk_number(skill_tree, "BUILD_QUICK_REPLACE"),
                "requires_eligible_old_and_new_blocks": True,
                "refuses_block_entities": True,
                "recovers_old_block_outside_creative": True,
                "server_authoritative": True,
            },
            "auto_restock": {
                "perk": _perk_number(skill_tree, "BUILD_AUTO_RESTOCK"),
                "matches_item_and_component_changes": True,
                "server_authoritative": True,
            },
            "decorative_magnet": {
                "perk": _perk_number(skill_tree, "BUILD_DECORATIVE_MAGNET"),
                "interval_ticks": _constant_value(magnet, "INTERVAL_TICKS"),
                "radius_blocks": _constant_value(magnet, "RADIUS"),
                "maximum_items_per_pass": _constant_value(magnet, "MAX_ITEMS_PER_PASS"),
                "pull_strength": _constant_value(magnet, "PULL_STRENGTH"),
                "client_toggle": True,
                "server_application": True,
            },
        },
        "plans": {
            "plan_2d": {
                "base_max_size": _constant_value(plan2d, "BASE_MAX_SIZE"),
                "upgraded_max_size": _constant_value(plan2d, "UPGRADED_MAX_SIZE"),
                "blocks_per_job_tick": _constant_value(plan2d, "BLOCKS_PER_JOB_TICK"),
                "global_blocks_per_tick": _constant_value(plan2d, "GLOBAL_BLOCKS_PER_TICK"),
                "preview_lifetime_ticks": _constant_value(plan2d, "PREVIEW_LIFETIME_TICKS"),
                "preview_lifetime_seconds": _seconds(_constant_value(plan2d, "PREVIEW_LIFETIME_TICKS")),
                "perk_base": _perk_number(skill_tree, "BUILD_PLAN_2D_8"),
                "perk_upgrade": _perk_number(skill_tree, "BUILD_PLAN_2D_12"),
            },
            "plan_3d": {
                "maximum_size": _constant_value(plan3d, "MAX_SIZE"),
                "preview_lifetime_ticks": _constant_value(plan3d, "PREVIEW_LIFETIME_TICKS"),
                "preview_lifetime_seconds": _seconds(_constant_value(plan3d, "PREVIEW_LIFETIME_TICKS")),
                "perk": _perk_number(skill_tree, "BUILD_PLAN_3D"),
            },
            "server_jobs": True,
            "resource_consumption_server_side": True,
            "preview_payloads_client_side": True,
        },
        "builder_tools": {
            "reach_perks": [
                {"perk": entry["perk"], "extra_reach": entry["value"]}
                for entry in _bonus_entries(skill_tree, "BUILD_REACH")
            ],
            "scaffolding_range": {"perk": _perk_number(skill_tree, "BUILD_SCAFFOLDING_RANGE"), "blocks": _first_bonus_value(skill_tree, "BUILD_SCAFFOLDING_RANGE")},
            "architect_compass": {
                "perk": _perk_number(skill_tree, "BUILD_ARCHITECT_COMPASS"),
                "minimum_radius": _constant_value(compass, "MIN_RADIUS"),
                "maximum_radius": _constant_value(compass, "MAX_RADIUS"),
                "default_radius": _constant_value(compass, "DEFAULT_RADIUS"),
                "persistent_item_data": True,
            },
            "builder_wand": {
                "perk": _perk_number(skill_tree, "BUILD_WAND"),
                "maximum_durability": int(wand_damage_match.group(1)) if wand_damage_match else None,
                "copies_safe_block_state_properties": True,
                "dangerous_properties_excluded": True,
                "server_authoritative": True,
            },
        },
        "reserve": {
            "perk_ranges": [
                {"perk": entry["perk"], "range": entry["value"]}
                for entry in _bonus_entries(skill_tree, "BUILD_RESERVE_RANGE")
            ],
            "maximum_chests_per_player": _constant_value(reserve, "MAX_CHESTS_PER_PLAYER"),
            "request_cooldown_ticks": _constant_value(reserve, "REQUEST_COOLDOWN_TICKS"),
            "persistent_state": True,
            "server_authoritative": True,
        },
        "decorative_content": {
            "vertical_slabs": {
                "perk": _perk_number(skill_tree, "BUILD_VERTICAL_SLABS"),
                "xp_each": _constant_value(vertical_path, "BUILDING_XP"),
                "types": vertical_slabs,
            },
            "blank_block": {
                "perk": _perk_number(skill_tree, "BUILD_BLANK_BLOCK"),
                "allowed_materials": blank_materials,
                "material_count": len(blank_materials),
                "validated_on_server": True,
            },
            "static_decoration": {
                "perk": _perk_number(skill_tree, "BUILD_STATIC_DECORATION"),
                "effects": static_effects,
                "effect_count": len(static_effects),
                "owner_protected": True,
                "creative_override": True,
            },
            "miniature": {
                "perk": _perk_number(skill_tree, "BUILD_MINIATURE"),
                "owner_controlled": True,
                "wand_rotation_supported": True,
                "retrievable": True,
                "server_authoritative": True,
            },
        },
        "multiplayer": {
            "xp_authority": "server",
            "plans_authority": "server",
            "reserve_authority": "server",
            "decoration_owner_protection": True,
            "preview_and_ui_payloads_client_facing": True,
            "persistent_player_and_world_states": True,
        },
        "extraction": {
            "method": "specialized_java_building_extraction",
            "files": [
                xp_path.relative_to(java_root).as_posix(), catalog_path.relative_to(java_root).as_posix(),
                plan2d.relative_to(java_root).as_posix(), plan3d.relative_to(java_root).as_posix(),
                magnet.relative_to(java_root).as_posix(), reserve.relative_to(java_root).as_posix(),
                compass.relative_to(java_root).as_posix(), vertical_path.relative_to(java_root).as_posix(),
                blank_path.relative_to(java_root).as_posix(), static_path.relative_to(java_root).as_posix(),
                skill_tree.relative_to(java_root).as_posix(),
            ],
        },
    }


def extract_farming_system(java_root: Path, locales: dict[str, dict[str, str]]) -> dict[str, Any]:
    base = java_root / "com/mythicrpg/farming"
    events = base / "FarmingEvents.java"
    breeding = base / "FarmingBreedingXpManager.java"
    growth = base / "FarmingGrowthManager.java"
    backpack = base / "FoodBackpackItem.java"
    flower = base / "EnchantedFlowerSmeltManager.java"
    death = base / "FarmingDeathManager.java"
    skill_tree = base / "FarmingSkillTree.java"
    events_source = _source(events)
    xp_body = _method_body(events_source, "getFarmingXp")
    xp_returns = [int(value) for value in re.findall(r"return\s+(\d+)\s*;", xp_body)]
    max_harvest_match = re.search(r"int\s+maxHarvest\s*=\s*(\d+)", events_source)
    replanted_match = re.search(r"world\.getTime\(\)\s*\+\s*(\d+)\s*\)", _method_body(events_source, "protectReplantedCrop"))
    crop_items = [
        "minecraft:wheat", "minecraft:carrots", "minecraft:potatoes", "minecraft:beetroots",
        "minecraft:nether_wart", "minecraft:cocoa", "minecraft:sweet_berry_bush",
        "minecraft:melon", "minecraft:pumpkin", "minecraft:brown_mushroom_block",
        "minecraft:red_mushroom_block", "minecraft:mushroom_stem",
    ]
    living_field_source = _method_body(_source(growth), "canLivingFieldAffect")
    living_blocks = list(dict.fromkeys(_token_list(living_field_source, "Blocks")))
    plant_rewards_body = _method_body(events_source, "getRandomPlantableReward")
    plant_rewards = list(dict.fromkeys(_token_list(plant_rewards_body, "Items")))

    return {
        "xp": {
            "harvest_categories": [
                {"id": "standard_mature_crop", "xp": xp_returns[-1] if xp_returns else 2, "examples": crop_items[:4]},
                {"id": "nether_wart_or_cocoa", "xp": 3, "examples": ["minecraft:nether_wart", "minecraft:cocoa"]},
                {"id": "mushroom_block", "xp": 3, "examples": ["minecraft:brown_mushroom_block", "minecraft:red_mushroom_block", "minecraft:mushroom_stem"]},
                {"id": "melon_or_pumpkin", "xp": 4, "examples": ["minecraft:melon", "minecraft:pumpkin"]},
            ],
            "mature_only": True,
            "supported_mature_categories": crop_items,
            "breeding": {
                "xp": _constant_value(breeding, "BREEDING_XP"),
                "memory_duration_ticks": _constant_value(breeding, "MEMORY_DURATION_TICKS"),
                "memory_duration_seconds": _seconds(_constant_value(breeding, "MEMORY_DURATION_TICKS")),
                "baby_match_radius": round(float(_constant_value(breeding, "BABY_MATCH_RADIUS_SQUARED", 0)) ** 0.5, 3),
                "requires_valid_breeding_item": True,
                "matches_new_baby_type_and_world": True,
            },
            "area_harvest": {
                "maximum_blocks": int(max_harvest_match.group(1)) if max_harvest_match else None,
                "requires_hoe": True,
                "requires_mature_farming_block": True,
                "reentrant_guard": True,
            },
            "anti_automation": {
                "recent_replant_protection_ticks": int(replanted_match.group(1)) if replanted_match else None,
                "automatic_growth_does_not_directly_award_xp": True,
                "server_break_events_only": True,
            },
        },
        "perk_values": {
            "enchanted_seed_chances": _bonus_values(skill_tree, "ENCHANTED_SEED_CHANCE"),
            "double_drop_chances": _bonus_values(skill_tree, "FARMING_DOUBLE_DROP_CHANCE"),
            "compost_rare_drop_chances": _bonus_values(skill_tree, "COMPOST_RARE_DROP_CHANCE"),
            "farmer_reach_radii": _bonus_values(skill_tree, "FARMER_REACH_RADIUS"),
            "vanilla_xp_per_harvest": _first_bonus_value(skill_tree, "FARMING_VANILLA_XP"),
            "bone_meal_regeneration_ticks": 60,
            "irrigated_step_speed_ticks": 60,
            "cultivated_shield_base_ticks": 200,
            "cultivated_shield_max_amplifier": 4,
            "source": skill_tree.relative_to(java_root).as_posix(),
        },
        "growth": {
            "living_field": {
                "perk": _perk_number(skill_tree, "LIVING_FIELD"),
                "radius": _constant_value(growth, "RADIUS"),
                "extra_attempts_per_second": _constant_value(growth, "EXTRA_GROWTH_ATTEMPTS_PER_SECOND"),
                "extra_attempts_per_tick": _constant_value(growth, "EXTRA_GROWTH_ATTEMPTS_PER_TICK",
                    float(_constant_value(growth, "EXTRA_GROWTH_ATTEMPTS_PER_SECOND", 0)) / 20.0),
                "affected_blocks": living_blocks,
                "server_tick_authority": True,
            },
            "compost_rewards": {
                "roll_count": 5,
                "fixed_rewards": ["minecraft:golden_carrot", "minecraft:bone_block", "minecraft:vine"],
                "plantable_pool": plant_rewards,
                "vanilla_xp_range": [3, 7],
            },
        },
        "objects": {
            "enchanted_seed": {
                "identifier": "mythicrpg:enchanted_seed",
                "drop_chances": _bonus_values(skill_tree, "ENCHANTED_SEED_CHANCE"),
                "stored_automatically_in_food_backpack": True,
            },
            "enchanted_flower": {
                "identifier": "mythicrpg:enchanted_flower",
                "perk": _perk_number(skill_tree, "ENCHANTED_FLOWER_CRAFT"),
                "offhand_smelt_manager": True,
                "food_backpack_supported": True,
                "feedback_cooldown_ticks": _constant_value(flower, "FEEDBACK_COOLDOWN_TICKS"),
            },
            "food_backpack": {
                "identifier": "mythicrpg:food_backpack",
                "perk": _perk_number(skill_tree, "FOOD_BACKPACK_CRAFT"),
                "slots": _constant_value(backpack, "BACKPACK_SLOTS"),
                "store_feedback_cooldown_ticks": _constant_value(backpack, "STORE_FEEDBACK_COOLDOWN_TICKS"),
                "full_feedback_cooldown_ticks": _constant_value(backpack, "FULL_FEEDBACK_COOLDOWN_TICKS"),
                "accepts_food_plants_and_farming_resources": True,
                "rejects_nested_backpacks": True,
                "eating_preservation_integration": True,
                "session_inventory_server_side": True,
            },
            "growth_totem": {
                "identifier": "mythicrpg:growth_totem",
                "origin_skill": "mining_archaeology",
                "farming_interaction": "crop_growth",
                "confidence": "registered_item_and_block",
            },
        },
        "death_and_persistence": {
            "preserved_farmer_perk": _perk_number(skill_tree, "PRESERVED_FARMER"),
            "preserves_vanilla_experience": True,
            "preserves_food_backpacks": True,
            "food_backpack_death_counter": True,
            "restored_on_player_copy": True,
        },
        "multiplayer": {
            "harvest_authority": "server",
            "breeding_match_authority": "server",
            "growth_authority": "server",
            "backpack_inventory_authority": "server",
            "player_specific_perks": True,
            "pending_breeding_actions_are_runtime_only": True,
        },
        "extraction": {
            "method": "specialized_java_farming_extraction",
            "files": [
                events.relative_to(java_root).as_posix(), breeding.relative_to(java_root).as_posix(),
                growth.relative_to(java_root).as_posix(), backpack.relative_to(java_root).as_posix(),
                flower.relative_to(java_root).as_posix(), death.relative_to(java_root).as_posix(),
                skill_tree.relative_to(java_root).as_posix(),
            ],
        },
    }


def _chest_modules(mod_items: Path, locales: dict[str, dict[str, str]]) -> list[dict[str, Any]]:
    source = _source(mod_items)
    pattern = re.compile(
        r"public\s+static\s+final\s+Item\s+(CHEST_MODULE_[IVX]+)\s*=\s*registerItem\(\s*\"([^\"]+)\"\s*,\s*new\s+ChestModuleItem\(\s*(\d+)",
        re.S,
    )
    result = []
    for symbol, item_id, extra in pattern.findall(source):
        identifier = f"mythicrpg:{item_id}"
        result.append({
            "symbol": symbol,
            "id": item_id,
            "identifier": identifier,
            "names": _translation_or_identifier(locales, identifier),
            "extra_slots": int(extra),
            "single_chest_capacity": 27 + int(extra),
            "double_chest_capacity_with_two_equal_modules": 54 + 2 * int(extra),
        })
    return result


def extract_woodcutting_system(java_root: Path, locales: dict[str, dict[str, str]]) -> dict[str, Any]:
    base = java_root / "com/mythicrpg/woodcutting"
    events = base / "WoodcuttingEvents.java"
    growth = base / "TreeGrowthSneakManager.java"
    axe = base / "EnchantedAxeProjectileManager.java"
    wood_eating = base / "WoodEatingEvents.java"
    inventory = base / "chest/ModularChestInventory.java"
    storage = base / "chest/ChestModuleStorage.java"
    manager = base / "chest/ChestModuleManager.java"
    screen = base / "chest/ModularChestScreenHandler.java"
    skill_tree = base / "WoodcuttingSkillTree.java"
    mod_items = java_root / "com/mythicrpg/core/ModItems.java"
    events_source = _source(events)
    sapling_body = _method_body(events_source, "getRandomSapling")
    saplings = list(dict.fromkeys(_token_list(sapling_body, "Items")))
    axe_source = _source(axe)
    axe_damage_match = re.search(r"axe\.damage\(\s*(\d+)", axe_source)
    food_body = _method_body(_source(wood_eating), "eatWood")
    hunger_match = re.search(r"getHungerManager\(\)\.add\(\s*(\d+)\s*,\s*([0-9.]+)f", food_body)
    modules = _chest_modules(mod_items, locales)

    return {
        "xp": {
            "log_xp": _constant_value(events, "LOG_XP"),
            "accepted_block_tag": "#minecraft:logs",
            "leaves_award_skill_xp": False,
            "timber": {
                "maximum_additional_blocks": _constant_value(events, "TIMBER_MAX_BLOCKS"),
                "same_block_type_only": True,
                "requires_axe": True,
                "xp_per_additional_log": _constant_value(events, "LOG_XP"),
                "server_authoritative": True,
            },
            "vanilla_xp_per_log_with_perk": 1,
        },
        "drops": {
            "double_drop_chances": _bonus_values(skill_tree, "WOOD_DOUBLE_DROP_CHANCE"),
            "enchanted_wood_chances": _bonus_values(skill_tree, "ENCHANTED_WOOD_CHANCE"),
            "random_sapling_chance": _first_bonus_value(skill_tree, "RANDOM_SAPLING_DROP_CHANCE"),
            "random_saplings": saplings,
            "leaf_apple_guaranteed_with_perk": True,
            "golden_apple_chance": _first_bonus_value(skill_tree, "LEAF_GOLDEN_APPLE_CHANCE"),
            "silk_touch_disables_extra_leaf_drops": True,
            "source": skill_tree.relative_to(java_root).as_posix(),
        },
        "tree_growth": {
            "perk": _perk_number(skill_tree, "TREE_GROWTH"),
            "check_interval_ticks": _constant_value(growth, "CHECK_INTERVAL_TICKS"),
            "cooldown_ticks": _constant_value(growth, "COOLDOWN_TICKS"),
            "cooldown_seconds": _seconds(_constant_value(growth, "COOLDOWN_TICKS")),
            "radius": _constant_value(growth, "RADIUS"),
            "maximum_saplings_per_trigger": _constant_value(growth, "MAX_SAPLINGS_PER_TRIGGER"),
            "trigger": "sneak_transition",
            "server_authoritative": True,
        },
        "enchanted_axe": {
            "identifier": "mythicrpg:enchanted_axe",
            "perk": _perk_number(skill_tree, "ENCHANTED_AXE_CRAFT"),
            "supported_projectiles": ["minecraft:arrow", "minecraft:snowball", "minecraft:egg"],
            "split_delay_ticks": _constant_value(axe, "SPLIT_DELAY_TICKS"),
            "split_delay_seconds": _seconds(_constant_value(axe, "SPLIT_DELAY_TICKS")),
            "split_arrow_cleanup_min_age_ticks": _constant_value(axe, "SPLIT_ARROW_MIN_AGE_TICKS"),
            "offhand_durability_cost": int(axe_damage_match.group(1)) if axe_damage_match else None,
            "duplicate_pickup": "creative_only",
            "server_authoritative": True,
        },
        "wood_eater": {
            "perk": _perk_number(skill_tree, "WOOD_EATER"),
            "accepted_item_tag": "#minecraft:logs",
            "hunger": int(hunger_match.group(1)) if hunger_match else None,
            "saturation_modifier": float(hunger_match.group(2)) if hunger_match else None,
            "requires_player_can_consume": True,
            "server_authoritative": True,
        },
        "chest_modules": {
            "tiers": modules,
            "base_slots_per_chest": _constant_value(inventory, "BASE_SLOTS_PER_CHEST"),
            "maximum_slots_per_chest": _constant_value(inventory, "MAX_SLOTS_PER_CHEST"),
            "maximum_chests": _constant_value(inventory, "MAX_CHESTS"),
            "maximum_total_storage": _constant_value(inventory, "MAX_TOTAL_STORAGE",
                int(_constant_value(inventory, "MAX_SLOTS_PER_CHEST", 0)) * int(_constant_value(inventory, "MAX_CHESTS", 0))),
            "persistent_extra_storage_per_physical_chest": int(re.search(r"\bEXTRA_STORAGE_SIZE\s*=\s*(\d+)", _source(storage)).group(1))
            if re.search(r"\bEXTRA_STORAGE_SIZE\s*=\s*(\d+)", _source(storage)) else None,
            "supports_chest_and_trapped_chest": True,
            "supports_double_chests": True,
            "hoppers_use_active_capacity": True,
            "module_slots_are_not_automatable": True,
            "safe_shrink_requires_all_items_to_fit": True,
            "atomic_repack_on_shrink": True,
            "viewer_tracking_keeps_lid_state": True,
            "server_authoritative": True,
        },
        "durability": {
            "axe_no_durability_perk": _perk_number(skill_tree, "AXE_NO_DURABILITY"),
            "undoes_one_durability_after_log_or_leaf_break": True,
        },
        "multiplayer": {
            "break_events_authority": "server",
            "timber_authority": "server",
            "chest_inventory_authority": "server",
            "viewer_registry_server_only": True,
            "persistent_module_and_extra_items_per_chest": True,
            "anti_duplication_transactional_module_changes": True,
        },
        "extraction": {
            "method": "specialized_java_woodcutting_extraction",
            "files": [
                events.relative_to(java_root).as_posix(), growth.relative_to(java_root).as_posix(),
                axe.relative_to(java_root).as_posix(), wood_eating.relative_to(java_root).as_posix(),
                inventory.relative_to(java_root).as_posix(), storage.relative_to(java_root).as_posix(),
                manager.relative_to(java_root).as_posix(), screen.relative_to(java_root).as_posix(),
                skill_tree.relative_to(java_root).as_posix(), mod_items.relative_to(java_root).as_posix(),
            ],
        },
    }
