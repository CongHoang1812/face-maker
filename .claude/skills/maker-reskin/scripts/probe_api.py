#!/usr/bin/env python3
"""Kiểm tra máy chủ có đúng định dạng mà ApiMapper hiện tại đọc được không (chỉ gửi GET / HEAD).

    python3 .claude/skills/maker-reskin/scripts/probe_api.py
    python3 .claude/skills/maker-reskin/scripts/probe_api.py --app-key ST300_Foo --base-url https://host/

Không truyền tham số thì đọc cấu hình từ app/build.gradle.kts và AppConfig.kt.
Exit code: 0 = khớp, chỉ cần đổi cấu hình; 1 = không khớp, cần sửa DTO / mapper.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
TIMEOUT_SECONDS = 20
USER_AGENT = "okhttp/5"


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None  # Máy chủ từng trả 302 -> /404.html cho ảnh bị chặn: coi là không có ảnh.


OPENER = urllib.request.build_opener(NoRedirect)


def fetch_json(url: str):
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS) as response:
        return json.load(response)


def image_status(url: str) -> str:
    request = urllib.request.Request(url, method="HEAD", headers={"User-Agent": USER_AGENT})
    try:
        with OPENER.open(request, timeout=TIMEOUT_SECONDS) as response:
            return str(response.status)
    except urllib.error.HTTPError as error:
        return str(error.code)
    except (urllib.error.URLError, TimeoutError) as error:
        return f"lỗi mạng ({error})"


def encode(path: str) -> str:
    return urllib.parse.quote(path, safe="/")


def configured() -> dict:
    gradle = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
    config_file = next((ROOT / "app/src/main/java").rglob("core/common/AppConfig.kt"))
    config = config_file.read_text(encoding="utf-8")
    field = lambda name: re.search(rf'"{name}", "\\"([^"\\]+)\\""', gradle).group(1)
    return {
        "base": field("BASE_URL"),
        "backup": field("BACKUP_BASE_URL"),
        "key": re.search(r'APP_KEY: String = "([^"]+)"', config).group(1),
    }


def parse_parts(value) -> tuple[int, int] | None:
    match = re.fullmatch(r"(\d+)-(\d+)", str(value or "").strip())
    return (int(match.group(1)), int(match.group(2))) if match else None


def check_server(base: str, key: str) -> list[str]:
    problems: list[str] = []
    assets = f"{base}public/app/{key}/"
    print(f"\n== {base} ==")
    try:
        data = fetch_json(f"{base}api/app/{key}")
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as error:
        return [f"Không đọc được api/app/{key}: {error}"]
    if not isinstance(data, dict) or not data:
        return ["api/app trả về không phải object có template"]

    templates = {name: parts for name, parts in data.items() if isinstance(parts, list) and parts}
    print(f"Template: {len(templates)} ({', '.join(list(templates)[:6])})")
    first_name, first_parts = None, []
    for name, parts in templates.items():
        valid = [part for part in parts if isinstance(part, dict) and parse_parts(part.get("parts")) and (part.get("quantity") or 0) > 0]
        if not valid:
            problems.append(f"Template `{name}` không có bộ phận hợp lệ (cần `parts` dạng X-Y và `quantity` > 0)")
            continue
        missing = {"position", "parts", "colorArray", "quantity", "level"} - set(parts[0])
        if missing:
            problems.append(f"Template `{name}` thiếu field: {', '.join(sorted(missing))}")
        # Mapper coi Y = 1 là tab đầu (thân nhân vật, bắt buộc chọn).
        if not any(parse_parts(part["parts"])[1] == 1 for part in valid):
            problems.append(f"Template `{name}` không có tab Y = 1: kiểm tra lại quy ước X-Y")
        if first_name is None:
            first_name, first_parts = name, valid
    print(f"Sai quy ước: {len(problems)}")

    if first_name is not None:
        part = first_parts[0]
        folder = f"{part.get('position') or first_name}/{part['parts']}"
        colors = [color.strip().lstrip("#").upper() for color in str(part.get("colorArray") or "").split(",") if color.strip()]
        layer = f"{folder}/{colors[0]}/1.png" if colors else f"{folder}/1.png"
        samples = {"avatar": f"{first_name}/avatar.png", "icon tab": f"{folder}/nav.png",
                   "thumbnail": f"{folder}/thumb_1.png", "layer": layer}
        for label, path in samples.items():
            status = image_status(assets + encode(path))
            print(f"Ảnh {label}: {status}  {path}")
            if status != "200":
                problems.append(f"Ảnh {label} trả {status}: {assets}{encode(path)}")

    try:
        decor = fetch_json(f"{assets}bg/bg.json")
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as error:
        problems.append(f"Không đọc được bg/bg.json: {error}")
        return problems
    folders = {"background": "bg/background", "sticker": "bg/sticker", "speech bubble": "bg/speech bubble"}
    for group, folder in folders.items():
        items = decor.get(group)
        if not isinstance(items, list):
            problems.append(f"bg.json thiếu nhóm `{group}`")
            continue
        usable = [item for item in items if (item.get("quantity") or 0) > 0]
        print(f"Decor {group}: {len(usable)} nhóm")
        if usable:
            category = str(usable[0].get("category") or "").strip()
            path = f"{folder}/{category}/1.png" if category else f"{folder}/1.png"
            status = image_status(assets + encode(path))
            print(f"Ảnh decor: {status}  {path}")
            if status != "200":
                problems.append(f"Ảnh decor `{group}` trả {status}: {path}")
    return problems


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app-key")
    parser.add_argument("--base-url")
    parser.add_argument("--backup-url")
    args = parser.parse_args()

    defaults = configured() if not (args.app_key and args.base_url) else {}
    key = args.app_key or defaults["key"]
    primary = args.base_url or defaults["base"]
    backup = args.backup_url or defaults.get("backup")

    primary_problems = check_server(primary.rstrip("/") + "/", key)
    backup_problems = check_server(backup.rstrip("/") + "/", key) if backup else []

    print("\n== Kết luận ==")
    if primary_problems:
        print("KHÔNG KHỚP ở máy chủ chính: cần đọc lại tài liệu API, có thể phải sửa DTO / mapper.")
        print("\n".join(f"- {item}" for item in primary_problems))
    else:
        print("KHỚP: chỉ cần đổi cấu hình, không phải sửa DTO / mapper.")
    if backup_problems:
        print("Máy chủ dự phòng có vấn đề:")
        print("\n".join(f"- {item}" for item in backup_problems))
    return 1 if primary_problems else 0


if __name__ == "__main__":
    sys.exit(main())
