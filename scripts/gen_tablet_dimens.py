#!/usr/bin/env python3
"""Sinh dimen cho tablet từ values/dimens.xml (thiết kế điện thoại 360 × 800).

    python3 scripts/gen_tablet_dimens.py

Không sửa tay values-sw*/dimens.xml, values-sw*/integers.xml: sửa BUCKETS bên dưới rồi chạy lại.

Mỗi bucket nhân mọi dimen dp/sp với `scale`. Chọn scale ≤ rộng màn / 360 nên mọi hàng ngang
của bản điện thoại vẫn vừa. Tablet (16:10) thấp hơn tỉ lệ điện thoại (20:9), nên các màn xếp
dọc cố định được kiểm tra bằng check_heights(); màn nào tràn thì thêm giá trị vào `fixed`.

- fixed:   giá trị chọn tay, thay cho giá trị nhân scale.
- PHONE:   giữ nguyên giá trị điện thoại (toạ độ dọc tuyệt đối mà nhân scale sẽ rơi ra ngoài màn).
- derived(): dimen tính từ dimen khác hoặc số cột lưới.
"""
import re
import xml.etree.ElementTree as ET
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "app/src/main/res"

BUCKETS = {
    # Máy 7–8": 600 × 960 (Nexus 7, Fire HD 8). Máy 768 × 1024 cũng rơi vào đây.
    "sw600dp": {
        "width": 600, "usable_h": 888,  # 960 - status bar 24 - nav bar 48
        "scale": 1.6,
        "tab_boost": 1.13,  # tab category to hơn phần còn lại
        "fixed": {
            # Customize/Decor: nhân vật co giãn ở phần còn lại sau panel
            "bottom_panel_height": 400,
            "permission_image": 360,
        },
        # Template luôn 2 cột (yêu cầu thiết kế)
        "columns": {"template_grid_columns": 2, "creation_grid_columns": 2,
                    "part_grid_columns": 5, "decor_grid_columns": 6},
    },
    # Máy 10–11": 800 × 1280 (Pixel Tablet, Galaxy Tab).
    "sw800dp": {
        "width": 800, "usable_h": 1208,
        "scale": 2.0,
        "tab_boost": 1.13,
        "fixed": {
            "bottom_panel_height": 550,
            "permission_image": 520,
        },
        "columns": {"template_grid_columns": 2, "creation_grid_columns": 2,
                    "part_grid_columns": 5, "decor_grid_columns": 8},
    },
}

PHONE = {
    # Intro tablet dùng layout-sw600dp đặt theo % chiều cao màn, không dùng 2 dimen này
    "intro_image_h",
    "intro_controls_top",
}

TAB_DIMENS = ["category_tab_w", "category_tab_h", "category_tab_selected_h",
              "category_tab_radius", "category_tab_icon", "category_tab_gap"]
# Cả nhóm cùng hệ số để ảnh nền (có bóng) và thumbnail vẫn khớp nhau
TEMPLATE_DIMENS = ["template_item_w", "template_item_h", "template_thumb", "template_thumb_left",
                   "template_thumb_top", "template_grid_gap_v"]
PART_DIMENS = ["part_item_h", "part_item_icon"]

# Màn xếp dọc cố định (không cuộn, không co giãn): tổng chiều cao tính từ dưới status bar
STACKS = {
    "home": ["home_cards_top", "home_card_size", "home_card_gap", "home_card_size"],
    "lucky_roll": ["lucky_random_top", "wood_button_height"],
    "cosplay_start": ["cosplay_buttons_top", "wood_button_height"],
    "success": ["success_image_top", "character_size_large", "screen_padding", "wood_button_height"],
    "creation_detail": ["content_top", "character_size", "screen_padding", "wood_button_height"],
    "permission": ["list_top", "permission_image", "screen_padding", "language_item_h", "screen_padding",
                   "language_item_h", "permission_continue_gap", "line_24"],
    "customize (nhân vật ≥ 300)": ["content_top", "grid_gap", "bottom_panel_height", 300],
}


def factors(bucket):
    """Hệ số riêng: lưới template/part lấp đầy chiều ngang, tab category to hơn."""
    b = BUCKETS[bucket]
    s, cols = b["scale"], b["columns"]
    out = dict.fromkeys(TAB_DIMENS, s * b["tab_boost"])
    # Template: cột × item = rộng màn - 2 × screen_padding
    out.update(dict.fromkeys(TEMPLATE_DIMENS, (b["width"] - 2 * 16 * s) / cols["template_grid_columns"] / 168))
    # Part: ô gần vuông, rộng = (rộng màn - 2 × part_grid_padding_h) / số cột
    out.update(dict.fromkeys(PART_DIMENS, (b["width"] - 2 * 24 * s) / cols["part_grid_columns"] / 64))
    return out


def derived(d, cols):
    return {
        # Nút ngay dưới nhân vật như bản điện thoại (khoảng cách = screen_padding)
        "cosplay_buttons_top": d["content_top"] + d["character_size"] + d["screen_padding"],
        "lucky_random_top": d["content_top_lucky_roll"] + d["character_size"] + d["screen_padding"],
        # Rộng lưới = cột × item + khe
        "template_grid_w": cols["template_grid_columns"] * d["template_item_w"],
        "creation_grid_w": cols["creation_grid_columns"] * d["creation_item_w"]
        + (cols["creation_grid_columns"] - 1) * d["creation_grid_gap_h"],
    }


def rnd(s):
    return round(s) if abs(s) >= 4 else round(s, 1)


def fmt(v):
    return str(int(v)) if float(v).is_integer() else str(v)


def read_phone():
    out = {}
    for el in ET.parse(RES / "values/dimens.xml").getroot().iter("dimen"):
        m = re.fullmatch(r"(-?[\d.]+)(dp|sp)", el.text.strip())
        if not m:
            raise SystemExit(f"Không hiểu giá trị {el.get('name')}={el.text}")
        out[el.get("name")] = (float(m.group(1)), m.group(2))
    return out


def build(bucket, phone):
    b, f = BUCKETS[bucket], factors(bucket)
    rows = {}
    for name, (v, unit) in phone.items():
        if name not in PHONE:
            rows[name] = (b["fixed"].get(name, rnd(v * f.get(name, b["scale"]))), unit)
    values = {n: v for n, (v, _) in rows.items()}
    for n, v in derived(values, b["columns"]).items():
        rows[n] = (v, "dp")
    return rows


def check(bucket, rows):
    b, ok = BUCKETS[bucket], True
    for screen, parts in STACKS.items():
        total = sum(p if isinstance(p, (int, float)) else rows[p][0] for p in parts)
        if total > b["usable_h"]:
            ok = False
            print(f"  ! {bucket} {screen}: cao {total:.0f} > {b['usable_h']}")
    for grid in ("template_grid_w", "creation_grid_w"):
        if rows[grid][0] > b["width"]:
            ok = False
            print(f"  ! {bucket} {grid} {rows[grid][0]} > {b['width']}")
    return ok


def write(path, header, lines):
    out = ['<?xml version="1.0" encoding="utf-8"?>', f"<!-- {header}",
           "     FILE SINH TỰ ĐỘNG bởi scripts/gen_tablet_dimens.py, đừng sửa tay. -->", "<resources>"]
    out += lines + ["</resources>\n"]
    path.parent.mkdir(exist_ok=True)
    path.write_text("\n".join(out), encoding="utf-8")


def main():
    phone = read_phone()
    ok = True
    for bucket, b in BUCKETS.items():
        rows = build(bucket, phone)
        ok &= check(bucket, rows)
        write(RES / f"values-{bucket}/dimens.xml",
              f"Tablet ≥{b['width']}dp: dimen điện thoại × {b['scale']}. "
              f"Giữ cỡ điện thoại: {', '.join(sorted(PHONE))}.",
              [f'    <dimen name="{n}">{fmt(v)}{u}</dimen>' for n, (v, u) in rows.items()])
        write(RES / f"values-{bucket}/integers.xml", f"Tablet ≥{b['width']}dp: số cột lưới.",
              [f'    <integer name="{n}">{v}</integer>' for n, v in b["columns"].items()])
        print(f"{bucket}: {len(rows)} dimen")
    if not ok:
        raise SystemExit("Có màn tràn, chỉnh `fixed` hoặc `scale`.")


if __name__ == "__main__":
    main()
