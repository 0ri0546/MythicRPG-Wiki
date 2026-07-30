from __future__ import annotations

import hashlib
import json
import re
import shutil
from pathlib import Path
from typing import Any, Iterable


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8-sig")


def write_json(path: Path, data: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def tree_sha256(root: Path) -> str:
    digest = hashlib.sha256()
    files = (path for path in root.rglob("*") if path.is_file())

    for path in sorted(
            files,
            key=lambda candidate: candidate.relative_to(root).as_posix()
    ):
        relative = path.relative_to(root).as_posix().encode("utf-8")
        digest.update(len(relative).to_bytes(4, "big"))
        digest.update(relative)
        content = path.read_bytes()
        digest.update(len(content).to_bytes(8, "big"))
        digest.update(content)
    return digest.hexdigest()


def strip_java_comments(source: str) -> str:
    out: list[str] = []
    i = 0
    state = "code"
    while i < len(source):
        ch = source[i]
        nxt = source[i + 1] if i + 1 < len(source) else ""
        if state == "code":
            if ch == '"':
                state = "string"
                out.append(ch)
            elif ch == "'":
                state = "char"
                out.append(ch)
            elif ch == "/" and nxt == "/":
                state = "line_comment"
                out.extend("  ")
                i += 1
            elif ch == "/" and nxt == "*":
                state = "block_comment"
                out.extend("  ")
                i += 1
            else:
                out.append(ch)
        elif state == "string":
            out.append(ch)
            if ch == "\\" and nxt:
                out.append(nxt)
                i += 1
            elif ch == '"':
                state = "code"
        elif state == "char":
            out.append(ch)
            if ch == "\\" and nxt:
                out.append(nxt)
                i += 1
            elif ch == "'":
                state = "code"
        elif state == "line_comment":
            if ch == "\n":
                out.append(ch)
                state = "code"
            else:
                out.append(" ")
        else:
            if ch == "*" and nxt == "/":
                out.extend("  ")
                i += 1
                state = "code"
            elif ch == "\n":
                out.append(ch)
            else:
                out.append(" ")
        i += 1
    return "".join(out)


def find_balanced_calls(source: str, call_name: str) -> list[str]:
    calls: list[str] = []
    pattern = re.compile(rf"\b{re.escape(call_name)}\s*\(")
    for match in pattern.finditer(source):
        start = source.find("(", match.start())
        depth = 0
        state = "code"
        i = start
        while i < len(source):
            ch = source[i]
            nxt = source[i + 1] if i + 1 < len(source) else ""
            if state == "code":
                if ch == '"':
                    state = "string"
                elif ch == "'":
                    state = "char"
                elif ch == "(":
                    depth += 1
                elif ch == ")":
                    depth -= 1
                    if depth == 0:
                        calls.append(source[start + 1 : i])
                        break
            elif state == "string":
                if ch == "\\" and nxt:
                    i += 1
                elif ch == '"':
                    state = "code"
            else:
                if ch == "\\" and nxt:
                    i += 1
                elif ch == "'":
                    state = "code"
            i += 1
    return calls


def split_top_level(value: str, delimiter: str = ",") -> list[str]:
    parts: list[str] = []
    current: list[str] = []
    depths = {"(": 0, "[": 0, "{": 0}
    closing = {")": "(", "]": "[", "}": "{"}
    state = "code"
    i = 0
    while i < len(value):
        ch = value[i]
        nxt = value[i + 1] if i + 1 < len(value) else ""
        if state == "code":
            if ch == '"':
                state = "string"
                current.append(ch)
            elif ch == "'":
                state = "char"
                current.append(ch)
            elif ch in depths:
                depths[ch] += 1
                current.append(ch)
            elif ch in closing:
                depths[closing[ch]] -= 1
                current.append(ch)
            elif ch == delimiter and all(v == 0 for v in depths.values()):
                parts.append("".join(current).strip())
                current = []
            else:
                current.append(ch)
        else:
            current.append(ch)
            if ch == "\\" and nxt:
                current.append(nxt)
                i += 1
            elif (state == "string" and ch == '"') or (state == "char" and ch == "'"):
                state = "code"
        i += 1
    if current or value.strip():
        parts.append("".join(current).strip())
    return parts


def parse_java_string(value: str) -> str | None:
    value = value.strip()
    if len(value) >= 2 and value[0] == value[-1] == '"':
        try:
            return json.loads(value)
        except json.JSONDecodeError:
            return value[1:-1]
    return None


def parse_java_number(value: str) -> int | float | None:
    cleaned = value.strip().replace("_", "")
    cleaned = re.sub(r"[fFdDlL]$", "", cleaned)
    if re.fullmatch(r"[-+]?\d+", cleaned):
        return int(cleaned)
    if re.fullmatch(r"[-+]?(?:\d+\.\d*|\d*\.\d+)(?:[eE][-+]?\d+)?", cleaned):
        return float(cleaned)
    return None


def copy_if_exists(source: Path, destination: Path) -> bool:
    if not source.is_file():
        return False
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, destination)
    return True


def unique_sorted(values: Iterable[str]) -> list[str]:
    return sorted(set(values))
