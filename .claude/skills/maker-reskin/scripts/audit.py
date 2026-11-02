#!/usr/bin/env python3
"""Kiểm tra app được tạo bằng new_app.py: còn sót gì của app cũ, đã thay bao nhiêu thiết kế.

    python3 .claude/skills/maker-reskin/scripts/audit.py            # khi đang làm
    python3 .claude/skills/maker-reskin/scripts/audit.py --release  # trước khi phát hành

Mức LỖI làm exit code = 1. --release nâng các mục phát hành từ CẢNH BÁO thành LỖI.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

SKILL_DIR = Path(__file__).resolve().parents[1]
ROOT = SKILL_DIR.parents[2]
APP = ROOT / "app/src"
RES = APP / "main/res"
MAX_LISTED = 12

errors: list[str] = []
warnings: list[str] = []


def text_of(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="ignore")


def source_files():
    for path in APP.rglob("*"):
        if path.is_file() and path.suffix in {".kt", ".xml", ".kts"}:
            yield path
    yield ROOT / "app/build.gradle.kts"
    yield ROOT / "settings.gradle.kts"


def find_term(term: str, files: list[Path], case_sensitive: bool = True) -> list[str]:
    flags = 0 if case_sensitive else re.IGNORECASE
    pattern = re.compile(re.escape(term), flags)
    return sorted({path.relative_to(ROOT).as_posix() for path in files
                   if pattern.search(text_of(path)) or pattern.search(path.name)})


def shorten(items: list[str]) -> str:
    extra = len(items) - MAX_LISTED
    return ", ".join(items[:MAX_LISTED]) + (f" ... và {extra} mục khác" if extra > 0 else "")


def check_identity(origin: dict, files: list[Path]) -> None:
    source = origin["source"]
    for label, term in (("package cũ", source["package"]), ("tiền tố cũ", source["class_prefix"]),
                        ("tên database cũ", source["db_name"]), ("tên app cũ", source["app_name"])):
        hits = find_term(term, files)
        if hits:
            errors.append(f"Còn {label} `{term}`: {shorten(hits)}")
    for term in origin["legacy_terms"]:
        hits = find_term(term, files, case_sensitive=False)
        if hits:
            warnings.append(f"Còn từ của chủ đề cũ `{term}`: {shorten(hits)}")


def string_keys(path: Path) -> set[str]:
    return {node.get("name") for node in ET.parse(path).getroot() if node.get("translatable") != "false"}


def check_translations() -> None:
    default = RES / "values/strings.xml"
    keys = string_keys(default)
    for path in sorted(RES.glob("values-*/strings.xml")):
        locale = path.parent.name
        current = string_keys(path)
        if keys - current:
            errors.append(f"{locale} thiếu chuỗi: {shorten(sorted(keys - current))}")
        if current - keys:
            warnings.append(f"{locale} thừa chuỗi: {shorten(sorted(current - keys))}")
    default_text = text_of(default)
    for path in sorted(RES.glob("values-*/strings.xml")):
        if text_of(path) == default_text:
            errors.append(f"{path.parent.name}/strings.xml giống hệt bản tiếng Anh (chưa dịch)")


def check_layouts() -> None:
    hex_colors, literals = [], []
    for path in sorted((RES / "layout").glob("*.xml")):
        content = text_of(path)
        if re.search(r'android:[A-Za-z]*(?:[Cc]olor|[Tt]int|background)="#', content):
            hex_colors.append(path.name)
        if re.search(r'(?:android:text|android:hint|android:contentDescription|app:dialogTitle)="(?!@)[^"]+"', content):
            literals.append(path.name)
    if hex_colors:
        errors.append(f"Layout dùng mã màu trực tiếp thay vì @color: {shorten(hex_colors)}")
    if literals:
        errors.append(f"Layout dùng chữ trực tiếp thay vì @string: {shorten(literals)}")


def check_config(files: list[Path], release: bool) -> None:
    gradle = text_of(ROOT / "app/build.gradle.kts")
    if re.search(r'"USE_MOCK_DATA",\s*"true"', gradle):
        (errors if release else warnings).append("USE_MOCK_DATA đang bật: app dùng dữ liệu giả")
    temp_urls = [p.relative_to(ROOT).as_posix() for p in files
                 if re.search(r"figma\.com/api|figma-alpha-api|s3-[a-z0-9-]+\.amazonaws\.com/figma", text_of(p))]
    if temp_urls:
        errors.append(f"Còn URL ảnh Figma tạm thời: {shorten(temp_urls)}")

    release_items = []
    if find_term("example.com", files):
        release_items.append("URL / email placeholder example.com trong AppConfig")
    pending = [tag for tag in ("TODO(ADS)", "TODO(STICKER_EXPORT)") if find_term(tag, files)]
    if pending:
        release_items.append(f"Tính năng còn là stub: {', '.join(pending)}")
    if re.search(r"optimization\s*\{\s*enable\s*=\s*false", gradle):
        release_items.append("Bản release đang tắt tối ưu (R8)")
    for item in release_items:
        (errors if release else warnings).append(f"[Phát hành] {item}")


def report_design_progress(origin: dict) -> None:
    unchanged: dict[str, list[str]] = defaultdict(list)
    total = kept = 0
    for relative, digest in origin["resource_hashes"].items():
        path = RES / relative
        if not path.exists():
            continue
        total += 1
        if hashlib.sha256(path.read_bytes()).hexdigest() == digest:
            kept += 1
            unchanged[path.parent.name].append(path.name)

    current = dict(re.findall(r'<color name="(\w+)">([^<]+)</color>', text_of(RES / "values/colors.xml")))
    same_colors = sorted(name for name, value in origin["colors"].items() if current.get(name) == value)

    print("\n== Tiến độ thiết kế ==")
    print(f"Màu giữ nguyên từ app cũ: {len(same_colors)}/{len(origin['colors'])}")
    print(f"File ảnh / font giữ nguyên: {kept}/{total}")
    for folder, names in sorted(unchanged.items()):
        print(f"  {folder} ({len(names)}): {shorten(names)}")
    print("Được phép giữ: cờ quốc gia, bánh xe màu, icon hệ thống dùng chung. Còn lại cần đối chiếu với Figma mới.")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--release", action="store_true", help="coi các mục phát hành là lỗi")
    args = parser.parse_args()

    origin_path = SKILL_DIR / "origin.json"
    if not origin_path.exists():
        print("Không có origin.json: script này chỉ chạy trong project do new_app.py tạo.", file=sys.stderr)
        return 2
    origin = json.loads(origin_path.read_text(encoding="utf-8"))

    files = list(source_files())
    check_identity(origin, files)
    check_translations()
    check_layouts()
    check_config(files, args.release)
    report_design_progress(origin)

    print(f"\n== Lỗi ({len(errors)}) ==")
    print("\n".join(f"- {item}" for item in errors) or "Không có")
    print(f"\n== Cảnh báo ({len(warnings)}) ==")
    print("\n".join(f"- {item}" for item in warnings) or "Không có")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
