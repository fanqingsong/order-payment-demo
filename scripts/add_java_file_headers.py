#!/usr/bin/env python3
"""Prepend standardized Chinese file headers (what/why/how + ASCII diagram) to all Java sources."""
from __future__ import annotations

import json
import re
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
META_PATH = Path(__file__).with_name("java_file_header_meta.json")
MARKER = "FILE_GUIDE_HEADER"

HEADER_TEMPLATE = """\
/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · {short_name:<58} ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】{what}
 * 【为什么】{why}
 * 【怎么做】{how}
 *
 * 示意图（{service_label}）：
{diagram_block}
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * ({marker})
 */
"""


def service_label(rel: str) -> str:
    if rel.startswith("order-service"):
        return "order-service · 8080 · order_db"
    if rel.startswith("inventory-service"):
        return "inventory-service · 8083 · inventory_db"
    if rel.startswith("payment-service"):
        return "payment-service · 8081 · payment_db"
    if rel.startswith("notification-service"):
        return "notification-service · 8082 · 无库"
    return "demo 微服务"


def strip_existing_header(content: str) -> str:
    if MARKER not in content:
        return content
    # Remove block comment from start through marker line
    pattern = re.compile(
        r"^/\*\*?[\s\S]*?\(" + re.escape(MARKER) + r"\)[\s\S]*?\*/\s*",
        re.MULTILINE,
    )
    return pattern.sub("", content, count=1)


def format_diagram(lines: list[str]) -> str:
    out = []
    for line in lines:
        out.append(f" * {line}")
    return "\n".join(out)


def build_header(rel: str, meta: dict) -> str:
    short = Path(rel).name
    diagram = meta.get("diagram") or ["(无额外流程图)"]
    return HEADER_TEMPLATE.format(
        short_name=short[:58],
        what=meta["what"],
        why=meta["why"],
        how=meta["how"],
        diagram_block=format_diagram(diagram),
        service_label=service_label(rel),
        marker=MARKER,
    )


def main() -> None:
    meta_all: dict[str, dict] = json.loads(META_PATH.read_text(encoding="utf-8"))
    java_files = sorted(REPO.rglob("*.java"))
    updated = 0
    missing = []
    for path in java_files:
        rel = path.relative_to(REPO).as_posix()
        if rel not in meta_all:
            missing.append(rel)
            continue
        content = path.read_text(encoding="utf-8")
        content = strip_existing_header(content)
        if not content.lstrip().startswith("package "):
            # insert before first package line
            idx = content.find("package ")
            if idx == -1:
                missing.append(rel + " (no package)")
                continue
            before, after = content[:idx], content[idx:]
            content = after
        header = build_header(rel, meta_all[rel])
        new_content = header + content
        if new_content != path.read_text(encoding="utf-8"):
            path.write_text(new_content, encoding="utf-8")
            updated += 1
    print(f"Updated {updated} files.")
    if missing:
        print("Missing meta for:", missing)
        raise SystemExit(1)


if __name__ == "__main__":
    main()
