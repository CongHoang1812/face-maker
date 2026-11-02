#!/usr/bin/env python3
"""Tạo project app maker mới từ project hiện tại: sao chép sang thư mục mới rồi đổi định danh.

Không bao giờ sửa project nguồn. Tên cũ được đọc từ code nên app nào cũng dùng làm mẫu được.

    python3 .claude/skills/maker-reskin/scripts/new_app.py \
        --dest ../anime-oc-maker \
        --app-name "Anime OC Maker" \
        --app-id com.conght.animeocmaker \
        --app-key ST279_AnimeOCMaker \
        --project-code ST279 \
        --base-url https://lvtglobal.tech/ \
        --backup-url https://lvt-api-tech.io.vn/
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import shutil
import sys
from pathlib import Path

SKILL_DIR = Path(__file__).resolve().parents[1]
SOURCE_ROOT = SKILL_DIR.parents[2]
ORIGIN_FILE = SKILL_DIR / "origin.json"

# Không sao chép: đồ build, cấu hình IDE, tài liệu riêng của app nguồn (Figma node, kế hoạch...).
SKIP_NAMES = {".git", ".gradle", ".kotlin", ".idea", "build", "captures", ".externalNativeBuild", ".cxx",
              "settings.local.json", "origin.json", "__pycache__"}
SKIP_RELATIVE = {"docs/prompts", "docs/plan.xlsx", "docs/assets.md", "docs/APP_BRIEF.md"}

TEXT_SUFFIXES = {".kt", ".kts", ".java", ".xml", ".properties", ".toml", ".pro", ".md"}
TRACKED_RES_DIRS = ("drawable", "mipmap", "font")
TEMPLATE_TERMS = ["D&amp;D", "D&D", "tavern"]


def fail(message: str) -> None:
    sys.exit(f"LỖI: {message}")


def read_required(pattern: str, path: Path, label: str) -> str:
    match = re.search(pattern, path.read_text(encoding="utf-8"))
    if not match:
        fail(f"không đọc được {label} trong {path}")
    return match.group(1)


def detect_identity(root: Path) -> dict:
    strings = root / "app/src/main/res/values/strings.xml"
    app_class = read_required(r'android:name="\.(\w+)App"', root / "app/src/main/AndroidManifest.xml", "Application class")
    package = read_required(r'namespace\s*=\s*"([^"]+)"', root / "app/build.gradle.kts", "namespace")
    config = root / "app/src/main/java" / package.replace(".", "/") / "core/common/AppConfig.kt"
    database = root / "app/src/main/java" / package.replace(".", "/") / "data/local/db/AppDatabase.kt"
    return {
        "package": package,
        "class_prefix": app_class,
        "db_name": read_required(r'NAME\s*=\s*"([^"]+)"', database, "tên database"),
        "app_key": read_required(r'APP_KEY: String = "([^"]+)"', config, "APP_KEY"),
        "app_name": read_required(r'<string name="app_name">(.*?)</string>', strings, "app_name"),
    }


def default_prefix(app_name: str) -> str:
    words = re.findall(r"[A-Za-z0-9]+", app_name.replace("'", ""))
    prefix = "".join(word[:1].upper() + word[1:] for word in words)
    # Tiền tố đứng đầu tên class Kotlin nên không được bắt đầu bằng số.
    if not prefix or prefix[0].isdigit():
        fail("không suy ra được --class-prefix từ tên app, hãy truyền --class-prefix")
    return prefix


def snake_case(name: str) -> str:
    return re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", name).lower()


def android_string(value: str) -> str:
    # Giữ nguyên \n vì đó là cách xuống dòng trong string resource (dùng ở --app-title).
    escaped = value.replace("\\n", "\n").replace("\\", "\\\\").replace("\n", "\\n")
    return escaped.replace("&", "&amp;").replace("<", "&lt;").replace("'", "\\'").replace('"', '\\"')


def replacement_literal(text: str):
    """Trả giá trị thay thế cho re.sub mà không diễn giải dấu \\ trong [text] (vd. \\n của Android)."""
    return lambda match: match.group(1) + text + match.group(3)


def copy_project(source: Path, dest: Path) -> None:
    def ignore(directory: str, names: list[str]) -> set[str]:
        relative = Path(directory).relative_to(source)
        return {name for name in names
                if name in SKIP_NAMES or (relative / name).as_posix() in SKIP_RELATIVE or name.endswith(".iml")}

    shutil.copytree(source, dest, ignore=ignore)


def move_packages(dest: Path, old: str, new: str) -> None:
    old_path, new_path = old.replace(".", "/"), new.replace(".", "/")
    for source_set in (dest / "app/src").iterdir():
        for language in ("java", "kotlin"):
            current = source_set / language / old_path
            if not current.is_dir():
                continue
            target = source_set / language / new_path
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.move(str(current), str(target))
            # Xoá các thư mục cha rỗng còn lại của package cũ.
            parent = current.parent
            while parent != source_set / language and not any(parent.iterdir()):
                parent.rmdir()
                parent = parent.parent


def project_text_files(dest: Path):
    for path in dest.rglob("*"):
        # Skill giữ tên chung, không thay chữ trong .claude để script vẫn đúng ở mọi đời app.
        if path.is_file() and path.suffix in TEXT_SUFFIXES and ".claude" not in path.relative_to(dest).parts:
            yield path


def replace_identifiers(dest: Path, old: dict, new: dict) -> None:
    pairs = [
        (old["package"], new["package"]),
        (old["class_prefix"], new["class_prefix"]),
        (old["db_name"], new["db_name"]),
        (old["app_key"], new["app_key"]),
    ]
    for path in project_text_files(dest):
        original = path.read_text(encoding="utf-8")
        updated = original
        for before, after in pairs:
            updated = updated.replace(before, after)
        if updated != original:
            path.write_text(updated, encoding="utf-8")

    for path in sorted(dest.rglob(f"*{old['class_prefix']}*"), key=lambda p: len(p.parts), reverse=True):
        if ".claude" not in path.relative_to(dest).parts:
            path.rename(path.with_name(path.name.replace(old["class_prefix"], new["class_prefix"])))


def set_build_config(dest: Path, base_url: str | None, backup_url: str | None) -> None:
    gradle = dest / "app/build.gradle.kts"
    text = gradle.read_text(encoding="utf-8")
    for field, url in (("BASE_URL", base_url), ("BACKUP_BASE_URL", backup_url)):
        if url:
            text, count = re.subn(rf'("{field}", "\\")[^"\\]*(\\"")', lambda m: f"{m.group(1)}{url}{m.group(2)}", text)
            if count != 1:
                fail(f"không tìm thấy {field} trong app/build.gradle.kts")
    gradle.write_text(text, encoding="utf-8")


def set_app_strings(dest: Path, name: str, title: str) -> None:
    files = list((dest / "app/src/main/res").glob("values*/strings.xml"))
    for path in files:
        text = path.read_text(encoding="utf-8")
        for key, value in (("app_name", name), ("app_title", title)):
            text = re.sub(rf'(<string name="{key}">)(.*?)(</string>)', replacement_literal(android_string(value)), text)
        path.write_text(text, encoding="utf-8")


def set_root_project(dest: Path, project_name: str) -> None:
    settings = dest / "settings.gradle.kts"
    text = re.sub(r'rootProject\.name = ".*"', lambda _: f'rootProject.name = "{project_name}"',
                  settings.read_text(encoding="utf-8"))
    settings.write_text(text, encoding="utf-8")


def snapshot_resources(dest: Path) -> dict:
    res = dest / "app/src/main/res"
    hashes = {}
    for path in sorted(res.rglob("*")):
        if path.is_file() and path.parent.name.startswith(TRACKED_RES_DIRS):
            hashes[path.relative_to(res).as_posix()] = hashlib.sha256(path.read_bytes()).hexdigest()
    colors = dict(re.findall(r'<color name="(\w+)">([^<]+)</color>', (res / "values/colors.xml").read_text(encoding="utf-8")))
    return {"resource_hashes": hashes, "colors": colors}


def write_brief(dest: Path, args: argparse.Namespace, new: dict) -> None:
    template = (SKILL_DIR / "assets/app-brief.template.md").read_text(encoding="utf-8")
    values = {"app_name": args.app_name, "project_code": args.project_code or "(chưa có)", "app_id": new["package"],
              "class_prefix": new["class_prefix"], "app_key": new["app_key"],
              "base_url": args.base_url or "(giữ như app nguồn)", "backup_url": args.backup_url or "(giữ như app nguồn)"}
    for key, value in values.items():
        template = template.replace(f"{{{{{key}}}}}", value)
    (dest / "docs").mkdir(exist_ok=True)
    (dest / "docs/APP_BRIEF.md").write_text(template, encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Tạo app maker mới từ project hiện tại.")
    parser.add_argument("--dest", required=True, type=Path, help="thư mục project mới (chưa tồn tại)")
    parser.add_argument("--app-name", required=True, help='tên hiển thị, vd. "Anime OC Maker"')
    parser.add_argument("--app-id", required=True, help="package / applicationId, vd. com.conght.animeocmaker")
    parser.add_argument("--app-key", required=True, help="mã app trên server, vd. ST279_AnimeOCMaker")
    parser.add_argument("--project-code", help="mã dự án, vd. ST279 (đặt vào tên Gradle project)")
    parser.add_argument("--app-title", help="tên hiện ở logo, dùng \\n để xuống dòng (mặc định = --app-name)")
    parser.add_argument("--class-prefix", help="tiền tố class/theme, mặc định suy từ tên app")
    parser.add_argument("--base-url", help="máy chủ chính")
    parser.add_argument("--backup-url", help="máy chủ dự phòng")
    parser.add_argument("--legacy-term", action="append", default=[],
                        help="từ thuộc chủ đề app cũ để audit tìm (lặp lại được)")
    return parser.parse_args()


def normalized_url(url: str | None, flag: str) -> str | None:
    if url is None:
        return None
    if not re.fullmatch(r"https?://[^\s\"\\]+", url):
        fail(f"{flag} không phải URL hợp lệ: {url}")
    return url.rstrip("/") + "/"  # Retrofit bắt buộc base URL kết thúc bằng /


def main() -> None:
    args = parse_args()
    if not re.fullmatch(r"[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+", args.app_id):
        fail(f"--app-id không hợp lệ: {args.app_id}")
    prefix = args.class_prefix or default_prefix(args.app_name)
    if not re.fullmatch(r"[A-Z][A-Za-z0-9]*", prefix):
        fail(f"--class-prefix không hợp lệ: {prefix}")
    if not re.fullmatch(r"[A-Za-z0-9_.-]+", args.app_key):
        fail(f"--app-key không hợp lệ: {args.app_key}")
    args.base_url = normalized_url(args.base_url, "--base-url")
    args.backup_url = normalized_url(args.backup_url, "--backup-url")

    dest = args.dest.expanduser().resolve()
    if dest.exists():
        fail(f"{dest} đã tồn tại, hãy chọn thư mục khác")
    if dest.is_relative_to(SOURCE_ROOT):
        fail("--dest phải nằm ngoài project nguồn")

    old = detect_identity(SOURCE_ROOT)
    new = {"package": args.app_id, "class_prefix": prefix, "db_name": f"{snake_case(prefix)}.db", "app_key": args.app_key}

    copy_project(SOURCE_ROOT, dest)
    move_packages(dest, old["package"], new["package"])
    replace_identifiers(dest, old, new)
    set_build_config(dest, args.base_url, args.backup_url)
    set_app_strings(dest, args.app_name, args.app_title or args.app_name)
    set_root_project(dest, f"{args.project_code}_{args.app_name}" if args.project_code else args.app_name)
    write_brief(dest, args, new)
    (dest / "gradlew").chmod(0o755)

    legacy = sorted({*TEMPLATE_TERMS, *args.legacy_term})
    origin = {"source": old, "legacy_terms": legacy, **snapshot_resources(dest)}
    (dest / ORIGIN_FILE.relative_to(SOURCE_ROOT)).write_text(json.dumps(origin, ensure_ascii=False, indent=1), encoding="utf-8")

    print(f"Đã tạo {dest}")
    print(f"  {old['package']} -> {new['package']}")
    print(f"  {old['class_prefix']} -> {new['class_prefix']}, APP_KEY {old['app_key']} -> {new['app_key']}")
    print("\nBước tiếp theo:")
    print(f"  cd {dest}")
    print("  git init && git add . && git commit -m 'Tạo project từ template'")
    print("  Điền Figma và tài liệu API vào docs/APP_BRIEF.md, rồi mở claude tại thư mục này.")


if __name__ == "__main__":
    main()
