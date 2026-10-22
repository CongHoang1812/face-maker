# BƯỚC 3 – Resource design system & component dùng chung

> Prompt cho agent. Tham chiếu giá trị ở `02_DESIGN_SYSTEM.md`. Figma fileKey `QDVSAOgH3rY4DuocNmN8nM`.

## Yêu cầu

1. **colors.xml**: tạo đủ token trong bảng màu `02_DESIGN_SYSTEM.md` (tên y hệt). Xoá màu mẫu không dùng.
2. **dimens.xml**: khai báo các kích thước ở mục "Kích thước" (vd `screen_padding=16dp`, `top_bar_top=60dp`, `icon_button=36dp`, `character_size=328dp`, `bottom_panel_height=356dp`, `part_item_w=56dp`, `part_item_h=64dp`, `decor_item=56dp`, `grid_gap=8dp`, `language_item_h=56dp`, `creation_card_w=160dp`, `creation_card_h=180dp`, `dialog_width=312dp`…).
3. **Font**: tải Londrina Solid (Regular, Black) từ Google Fonts (OFL) vào `res/font/`, tạo `font/londrina_solid_family.xml`. Đặt làm `fontFamily` mặc định trong theme.
4. **styles.xml / themes.xml**: các TextAppearance `Text.Title`, `Text.DialogTitle`, `Text.Button`, `Text.Body`, `Text.TabSmall`, `Text.Caption`. Style dialog: `Theme.FaceMaker.Dialog` (nền trong suốt, dim 0.6, không title).
5. **strings.xml** + bản dịch `values-hi`, `values-es`, `values-fr`, `values-pt`, `values-in` cho toàn bộ chuỗi trong Figma (Language, Next, Continue, Permission, intro texts, D&D maker, D&D cosplay, D&D lucky roll, My creation, Rate us, Share app, Privacy & Policy, Color, Image, Pack, Download, Random, Cosplay, Edit, Exit, Character, Design, No character here, Add to Telegram, Add to Whatsapp, Instruction + nội dung, dialog texts, rate texts, Retry, Saved, Save failed…).
6. **Drawable shape** (không cần ảnh):
   - `bg_wood_button_outer` (brown_primary, bo 16) + `bg_wood_button_inner` (orange_button, bo 12, viền trên highlight bằng layer-list) ; bản gray (`tab_off_outer`/`tab_off_inner`).
   - `bg_round_icon_outer` / `bg_round_icon_inner`.
   - `bg_language_item` (selector: checked → `orange_selected`, default → white; viền 2dp `brown_dark`, bo 16).
   - `bg_color_bar` (`color_bar_bg`, bo 24).
   - `bg_decor_item` (white bo 4) + `bg_decor_item_selected` (viền 2dp orange_button).
   - `bg_category_tab` (selector selected → white, default → `cell_tan`; bo trên 8, viền 1dp `brown_dark`).
   - `bg_creation_tab_on/off`, `bg_small_icon` (cream, viền 1dp brown_dark, bo 8).
   - `bg_color_dot` (oval, viền trắng 2dp khi selected).
7. **Asset ảnh/icon**: dùng Figma MCP `get_design_context` cho các node trong bảng asset (`02_DESIGN_SYSTEM.md`) để lấy URL, tải bằng `curl -L` vào thư mục tạm, sau đó:
   - SVG icon → VectorDrawable `ic_*.xml` (tên: `ic_back`, `ic_download`, `ic_reset`, `ic_flip`, `ic_move`, `ic_dice`, `ic_dice_color`, `ic_none`, `ic_tab_background`, `ic_tab_sticker`, `ic_tab_speech`, `ic_tab_text`, `ic_trash`, `ic_pencil`, `ic_share`, `ic_archive`, `ic_home`, `ic_info`, `ic_close`, `ic_check`, `ic_arrow_up/down/left/right`, `ic_rotate_left/right`, `ic_zoom_in/out`, `ic_color_wheel`, `ic_add_image`, `ic_settings`, `ic_language`, `ic_star`, `ic_privacy`, cờ `ic_flag_*`).
   - Ảnh nền/ảnh minh hoạ → WebP trong `drawable-nodpi` (`bg_tavern`, `bg_tavern_inside`, `bg_tavern_floor`, `img_logo`, `img_home_maker`, `img_home_cosplay`, `img_home_lucky`, `img_home_creation`, `img_intro_1..3`, `img_permission`, `img_empty_character`).
   - Khung giấy rách (cata, creation card, dialog, item customize) → 9-patch `*.9.png` hoặc PNG + scaleType fitXY.
   - **Không** dùng URL Figma tạm thời trong code.
8. **Component** trong `core/widget` (custom View/compound view + `attrs.xml`):
   - `WoodButton` (attr: `text`, `variant=orange|gray`, `buttonWidth`) – hiệu ứng nhấn: scale 0.95.
   - `RoundIconButton` (attr: `icon`).
   - `TopBarView` (back + slot action bên phải) – tuỳ chọn, có thể dùng `include` layout.
   - `PaperDialogContainer` (nền giấy rách + title + message + 2 WoodButton).
9. Extension `View.setOnSingleClickListener` (debounce 500ms) và `Int.dp`.

## Tiêu chí hoàn thành

- Build pass; mở layout preview trong Android Studio thấy WoodButton/RoundIconButton giống Figma.
- Không còn hex màu hardcode trong layout.
- Danh sách asset đã export (tên file ↔ node id) được ghi vào `docs/assets.md`.
