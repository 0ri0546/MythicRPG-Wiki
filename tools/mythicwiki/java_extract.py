from __future__ import annotations

import re
from pathlib import Path
from typing import Any

import yaml

from .utils import (
    find_balanced_calls,
    parse_java_number,
    parse_java_string,
    read_text,
    split_top_level,
    strip_java_comments,
)


def extract_skill_ids(skill_type_file: Path) -> list[str]:
    source = strip_java_comments(read_text(skill_type_file))
    match = re.search(r"enum\s+SkillType\s*\{(.*?);", source, re.S)
    if not match:
        raise ValueError("Enum SkillType introuvable")
    return [token.strip().lower() for token in match.group(1).split(",") if token.strip()]


def _parse_parents(value: str) -> list[int]:
    match = re.search(r"List\.of\((.*?)\)", value, re.S)
    if not match or not match.group(1).strip():
        return []
    result: list[int] = []
    for item in split_top_level(match.group(1)):
        number = parse_java_number(item)
        if isinstance(number, int):
            result.append(number)
    return result


def _inner_call_args(expression: str) -> tuple[str, list[str]]:
    expression = expression.strip()
    if expression.startswith("new SkillTreeNode"):
        calls = find_balanced_calls(expression, "SkillTreeNode")
        return "SkillTreeNode", split_top_level(calls[0]) if calls else []
    if expression.startswith("node"):
        calls = find_balanced_calls(expression, "node")
        return "node", split_top_level(calls[0]) if calls else []
    return "unknown", []


def extract_skill_tree(path: Path, skill_id: str) -> list[dict[str, Any]]:
    source = strip_java_comments(read_text(path))
    nodes: list[dict[str, Any]] = []
    for call in find_balanced_calls(source, "nodes.put"):
        outer = split_top_level(call)
        if len(outer) < 2:
            continue
        kind, args = _inner_call_args(outer[1])
        if len(args) < 8:
            continue
        node_id = parse_java_number(args[0])
        name_key = parse_java_string(args[1])
        description_key = parse_java_string(args[2])
        x = parse_java_number(args[3])
        y = parse_java_number(args[4])
        fork_id = parse_java_number(args[6])
        branch_id = parse_java_number(args[7])
        if not all((isinstance(node_id, int), name_key, description_key, isinstance(x, int), isinstance(y, int))):
            continue
        raw_tail = ", ".join(args[8:])
        bonuses: list[dict[str, Any]] = []
        bonus_matches = list(re.finditer(r"BonusType\.([A-Z0-9_]+)", raw_tail))
        for index, match in enumerate(bonus_matches):
            segment_end = bonus_matches[index + 1].start() if index + 1 < len(bonus_matches) else len(raw_tail)
            segment = raw_tail[match.end() : segment_end]
            number_match = re.search(r",\s*([-+]?\d+(?:\.\d+)?(?:[fFdD])?)", segment)
            bonuses.append({
                "type": match.group(1).lower(),
                "value": parse_java_number(number_match.group(1)) if number_match else None,
            })
        effects = [
            {"effect": effect.lower(), "amplifier": int(amplifier)}
            for effect, amplifier in re.findall(r"StatusEffects\.([A-Z0-9_]+)\s*,\s*(-?\d+)", raw_tail)
        ]
        poison = None
        poison_match = re.search(r"new\s+PoisonOnHit\(\s*(\d+)\s*,\s*(\d+)\s*\)", raw_tail)
        if poison_match:
            poison = {"amplifier": int(poison_match.group(1)), "duration_ticks": int(poison_match.group(2))}
        nodes.append({
            "id": node_id,
            "skill_id": skill_id,
            "slug": f"{skill_id}-{node_id}",
            "name_key": name_key,
            "description_key": description_key,
            "position": {"x": x, "y": y},
            "parent_ids": _parse_parents(args[5]),
            "fork_id": fork_id if isinstance(fork_id, int) else -1,
            "branch_id": branch_id if isinstance(branch_id, int) else -1,
            "bonuses": bonuses,
            "effects": effects,
            "poison_on_hit": poison,
            "extraction": {"method": f"java_{kind}", "file": path.as_posix()},
        })
    return sorted(nodes, key=lambda node: node["id"])


def extract_literal_registrations(java_root: Path) -> dict[str, set[str]]:
    result = {"item": set(), "block": set(), "entity": set()}
    patterns = {
        "item": re.compile(r"registerItem\(\s*\"([a-z0-9_./-]+)\""),
        "block": re.compile(r"register(?:Block|LuckyBlock|BlankBlock|StaticDecoration|BareBlock)\(\s*\"([a-z0-9_./-]+)\""),
    }
    for path in java_root.rglob("*.java"):
        source = strip_java_comments(read_text(path))
        for kind, pattern in patterns.items():
            result[kind].update(pattern.findall(source))
        for identifier in re.findall(r"Identifier\.of\(\s*MythicRPG\.MOD_ID\s*,\s*\"([a-z0-9_./-]+)\"\s*\)", source):
            if "Entity" in path.name or "Entities" in path.name:
                result["entity"].add(identifier)
    return result


def load_documented_values(config_file: Path, java_root: Path) -> tuple[list[dict[str, Any]], list[str]]:
    config = yaml.safe_load(read_text(config_file)) or {}
    entries = config.get("values", [])
    values: list[dict[str, Any]] = []
    errors: list[str] = []
    for entry in entries:
        relative_file = Path(entry["file"])
        path = java_root / relative_file
        if not path.is_file():
            errors.append(f"Fichier de valeur introuvable: {relative_file}")
            continue
        source = strip_java_comments(read_text(path))
        symbol = str(entry["symbol"])
        match = re.search(
            rf"\bstatic\s+final\s+[\w<>?,.]+\s+{re.escape(symbol)}\s*=\s*([^;]+);",
            source,
        )
        if not match:
            errors.append(f"Constante introuvable: {relative_file}:{symbol}")
            continue
        expression = match.group(1).strip()
        value: Any = parse_java_number(expression)
        if value is None:
            value = parse_java_string(expression)
        if value is None and expression in {"true", "false"}:
            value = expression == "true"
        if value is None:
            errors.append(f"Expression non littérale non extraite: {relative_file}:{symbol} = {expression}")
            continue
        values.append({
            "id": entry["id"],
            "value": value,
            "unit": entry.get("unit"),
            "status": entry.get("status", "unknown"),
            "label": entry.get("label", {}),
            "audiences": entry.get("audiences", ["website"]),
            "formula_group": entry.get("formula_group"),
            "source": {"file": relative_file.as_posix(), "symbol": symbol, "expression": expression},
        })
    return values, errors
