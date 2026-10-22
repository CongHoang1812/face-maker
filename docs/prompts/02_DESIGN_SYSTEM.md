# 02 – Design system (trích từ Figma)

> Tài liệu tham chiếu. Bước 3 (`05_STEP_design_system_resources.md`) sẽ chuyển các giá trị này thành resource.

## Màu

| Token (`colors.xml`) | Hex | Dùng ở |
|---|---|---|
| `brown_primary` | `#B25324` | Viền ngoài nút gỗ, tab đang chọn (creation), tiêu đề dialog, nút Rate |
| `orange_button` | `#FEBF7F` | Lõi nút gỗ, viền ô "none" được chọn, nút gift |
| `button_highlight` | `#FBFCDB` | Viền sáng trên (inset top 1dp) của lõi nút |
| `brown_dark` | `#4C2B13` | Viền/đổ bóng nút, viền item, chữ trên nền sáng |
| `panel_brown` | `#6D493B` | Panel dưới của customize/editor |
| `color_bar_bg` | `#331100` | Thanh chọn màu (bo 24dp) |
| `cell_tan` | `#FFDFB2` | Ô item customize, tab category chưa chọn, nền dialog |
| `editor_bg` | `#FFD9B2` | Nền màn decor editor / successful / detail |
| `cream` | `#FFFAF2` | Nền My creation, nền icon nhỏ trash/pencil |
| `language_bg` | `#B28A71` | Nền màn language lần đầu, intro, permission |
| `orange_selected` | `#FF8D1A` | Item ngôn ngữ đang chọn, chữ "Next", indicator active |
| `tab_off_outer` | `#666666` | Viền ngoài nút tab con không chọn |
| `tab_off_inner` | `#CCCCCC` | Lõi nút tab con không chọn |
| `overlay_20` | `#33000000` | Phủ tối 20% lên ảnh nền quán rượu |
| `overlay_dim` | `#99000000` | Phủ tối khi mở instruction/dialog |
| `white` | `#FFFFFF` | Ô item editor, chữ trên nút |
| `shimmer_base` | `#33FFFFFF` (trên nền tối) / `#E6D3BC` (trên nền sáng) | Khung mờ |
| `shimmer_highlight` | `#80FFFFFF` | Dải sáng chạy qua |
| `star_on` / `star_off` | `#FFCC00` / `#C4C4C4` | Rate us |

## Typography

- Font chính: **Londrina Solid** (Google Fonts, Regular + Black). Đặt tại `res/font/londrina_solid.ttf`, `londrina_solid_black.ttf`.
- Rate us dùng Roboto (font hệ thống).

| Style | Size / line height | Ví dụ |
|---|---|---|
| `Text.Logo` | 36sp, Black, viền nâu (dùng ảnh logo export từ Figma thay vì text) | D&D CHARACTER MAKER |
| `Text.Title` | 32sp / 48 | "Language", "Permission" |
| `Text.DialogTitle` | 24sp, `brown_primary` | "Exit", "Reset" |
| `Text.Button` | 16sp / 20, trắng, viền chữ nâu | Next, Random, Download |
| `Text.Body` | 16sp / 24 | item ngôn ngữ, Character/Design tab |
| `Text.TabSmall` | 14sp / 16, trắng | Color / Image / Pack |
| `Text.Caption` | 12–14sp, `brown_primary` | "No character here", nội dung dialog |

## Kích thước (dp)

- Màn hình gốc 360 × 800; padding ngang 16; status bar 44; top bar icon 40 tại y=60.
- Nút tròn icon: 36 (viền ngoài `brown_primary` bo 20, lõi 28 `orange_button` bo 24, padding 3).
- Nút gỗ chữ: cao 40, viền ngoài bo 16–20, lõi cao 32 bo 12; padding trên 3, dưới 5 (tạo cảm giác nổi).
- Ảnh nhân vật: 328 × 328 tại y=116, căn giữa.
- Panel dưới: cao 356 (y=444).
- Item customize: 56 × 64 (khung giấy rách `cell_tan`), gap 8, 5 cột, padding ngang 24.
- Item editor: 56 × 56 trắng bo 4, gap 8; ô "none" chọn: viền 2dp `orange_button`.
- Tab category (đáy): 56 × 48 bo trên 8, viền 1 `brown_dark`; tab chọn: nền trắng, cao 56, nhô lên 8.
- Thanh màu: cao 40, padding 8, chấm màu 24, gap 16.
- Item ngôn ngữ: 328 × 56, bo 16, viền 2 `brown_dark`, padding ngang 24, cờ 20, radio 16, khoảng cách dọc 16.
- Thẻ My creation: 160 × 180 (ảnh 136 bo 4, cách mép 12), icon nhỏ 24 bo 8, gap lưới 8 ngang / 16 dọc.
- Dialog: rộng 312, nền "giấy rách" `cell_tan`, nút 142 × 40.

## Component dùng chung (đặt tên View)

| Component | Mô tả |
|---|---|
| `WoodButton` | Nút chữ 2 lớp (viền ngoài + lõi + highlight). Biến thể `orange` (mặc định) và `gray` (tab không chọn) |
| `RoundIconButton` | Nút tròn 36dp chứa icon 16–20dp (back, reset, flip, move, share, home…) |
| `PaperFrameLayout` | Khung "giấy rách" (dùng drawable/9-patch export từ Figma) cho dialog, thẻ, khung tham chiếu |
| `ColorBarView` | RecyclerView ngang trong nền `color_bar_bg` |
| `ShimmerFrameLayout` | Khung mờ + dải sáng (bước 4) |
| `NoneItem` | Ô "bỏ chọn" luôn đứng đầu mọi danh sách phụ kiện |
| `TopBar` | Hàng icon y=60: trái back, phải các action |

## Asset cần export từ Figma (PNG/SVG → `drawable`)

| Asset | Node tham khảo |
|---|---|
| Nền quán rượu (home/setting/cosplay/lucky) | nền frame `3:438`, `3:41`, `3:11` |
| Nền quán rượu trong (customize) | nền frame `3:335` |
| Logo D&D CHARACTER MAKER | trong `3:438` |
| Ảnh 4 thẻ home | trong `3:438` |
| Ảnh intro 1/2/3, ảnh permission | `3:698`, `3:692`, `3:717`, `3:741` |
| Ảnh nhân vật mẫu cho empty state | `3:1229` |
| Khung giấy cata / creation item | `3:278`, `3:1269` |
| Nền dialog giấy rách | `69:35` |
| Icon: back, download, reset(refresh), flip-horizontal, arrows(move), dice, gift-dice, empty-set, background, sticker, message-quote, text, trash, pencil, share, archive, home, info, close, check, arrow up/down/left/right, rotate left/right, plus, minus, color-wheel, add-image, cờ quốc gia | trong các frame tương ứng |

> Agent có thể dùng Figma MCP `get_design_context` với node id để lấy URL asset và tải về bằng `curl -L`. SVG → chuyển sang VectorDrawable (Android Studio: New > Vector Asset), ảnh lớn → WebP.
