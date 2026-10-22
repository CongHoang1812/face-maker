# Asset export từ Figma

Figma fileKey `QDVSAOgH3rY4DuocNmN8nM`. Mọi asset đã tải về project, **không** dùng URL Figma trong code.

## Cách export

| Loại | Xử lý | Thư mục |
|---|---|---|
| Glyph SVG đơn giản (không filter) | Làm phẳng `<use>`/`<mask>` → VectorDrawable (giữ clip-path, làm tròn 2 chữ số) | `res/drawable/*.xml` |
| SVG có filter (bóng trong, vân giấy rách) | Render SVG gốc bằng resvg @3x → WebP lossless (VectorDrawable không hỗ trợ filter) | `res/drawable-xxhdpi/*.webp` |
| Ảnh raster (nền, minh hoạ, cờ, emoji) | Ảnh gốc → WebP (q88, giới hạn cạnh dài) | `res/drawable-nodpi/*.webp` |

Ghi chú:
- Figma export PNG có nền xám đục `#363636`, nên khung giấy và icon tròn được **render lại từ SVG** (trong suốt) thay vì dùng PNG export.
- Các khung giấy rách (`bg_part_item*`, `bg_template_item`, `bg_creation_card`, `bg_dialog_paper`, `bg_rate_dialog`, `bg_instruction_paper`) là PNG/WebP thường. Đặt làm `background` (tự giãn) hoặc `ImageView` với `scaleType="fitXY"`. Các ảnh này đã chừa sẵn 2dp viền bóng như trong Figma.
- Logo "D&D CHARACTER MAKER" trong Figma là **chữ**, không phải ảnh, nên dùng `TextView` + `@style/Text.Logo` + `@string/app_title`. Không có `img_logo`.
- Mọi màn có nền quán rượu (splash, home, setting, cata, customize, cosplay, lucky roll, my creation) dùng **cùng một ảnh** → `bg_tavern` (`centerCrop`) + phủ `overlay_20`. Splash chỉ crop khác. Vì vậy không có `bg_tavern_inside` / `bg_tavern_floor`.
- Icon tròn 40dp ở top bar (back, settings, download, archive, info, close, xoay, mũi tên, zoom) trong Figma là **ảnh liền khối** (đã gồm nền nút): dùng `ImageButton` + `@style/Widget.FaceMaker.ImageIconButton`. Icon dạng nền nút + glyph (reset, flip, move, share, home, trash, rotate_right…) dùng `RoundIconButton app:icon="@drawable/ic_*"`.
- Text editor: Figma dùng Cherry Bomb, Chela One, Wantedo, Western Bang Bang. Wantedo / Western Bang Bang không có trên Google Fonts nên thay bằng các font OFL khác (xem bảng Font).

## VectorDrawable (`res/drawable`)

| File | Node Figma | Dùng ở |
|---|---|---|
| `ic_reset` | `3:346` (refresh 2) | Customize – reset |
| `ic_flip` | `3:351` (flip-horizontal 1) | Customize – flip |
| `ic_move` | `8:18` (arrows 1) | Customize – move |
| `ic_none` | `69:19` (ph:empty) | Ô "None" item customize |
| `ic_none_editor` | `3:867` (empty-set 1) | Ô "None" item editor |
| `ic_dice` | `69:29` | Ô random part |
| `ic_trash_small` | `3:917` (trash 1, 12dp) | Handle xoá sticker |
| `ic_tab_background` / `ic_tab_sticker` / `ic_tab_speech` / `ic_tab_text` | `3:930`, `3:932`, `3:934`, `3:936` | Tab editor (tint cam = chọn, xám = không) |
| `ic_rotate_right` | `3:1129` (rotate-right 1) | Successful – re-edit |
| `ic_home` | `3:1134` | Successful – home |
| `ic_share` | `3:1147` (share 2) | Successful / creation – share |
| `ic_trash` | `3:1526` (trash 2, 20dp) | Creation – xoá (top bar) |
| `ic_trash_card` | `3:1277` (trash 2, 12dp) | Thẻ creation – xoá |
| `ic_pencil` | `3:1281` (pencil 1) | Thẻ creation – sửa |
| `ic_check_small` | `I3:1547;2:1906` (check 3) | Checkbox chế độ chọn nhiều |
| `ic_check` | `69:356` | Text editor – xác nhận |
| `ic_check_language` | `3:666` (check 1) | Màn language – ✓ |
| `ic_language` / `ic_star` / `ic_share_app` / `ic_privacy` | `3:470`, `3:474`, `3:478`, `3:482` | Màn setting |
| `ic_radio_on` / `ic_radio_off` | `3:627` / `3:634` | Item ngôn ngữ, toggle permission |
| `ic_star_rate` | `69:268` (Star 1, tint `star_on`/`star_off`) | Rate us |
| `img_lucky_arrow` | `3:14` | Lucky roll – mũi tên chỉ xuống |

## Render từ SVG (`res/drawable-xxhdpi`, WebP @3x)

| File | Node Figma | Dùng ở |
|---|---|---|
| `ic_back` | `3:370` | Back (mọi màn) |
| `ic_settings` | `3:463` | Home |
| `ic_download` | `3:989` | Editor / creation |
| `ic_archive` | `3:1139` | Successful – my creation |
| `ic_info` | `3:63` | Cosplay – instruction |
| `ic_close` | `3:119` | Instruction / score popup |
| `ic_rotate_left` / `ic_rotate_right_button` | `8:330` / `8:334` | Move panel |
| `ic_arrow_up` / `ic_arrow_down` / `ic_arrow_left` / `ic_arrow_right` | `8:296`, `8:292`, `8:288`, `8:284` | Move panel |
| `ic_zoom_in` | `61:19` | Move panel |
| `ic_zoom_out` | `61:23` (dựng lại từ `ic_zoom_in`, thay dấu + bằng −) | Move panel |
| `bg_round_icon_inner` | `3:1128` (Subtract 32) | Lõi `RoundIconButton` |
| `bg_part_item` / `bg_part_item_selected` | `3:394` / `3:392` | Ô item customize (giấy rách) |
| `bg_template_item` | `3:299` | Màn cata |
| `bg_creation_card` | `3:1270` + `3:1271` (ghép) | Thẻ My creation |
| `bg_dialog_paper` | `69:35` | `PaperDialogContainer` |
| `bg_rate_dialog` | `69:234` | Rate us |
| `bg_instruction_paper` | `3:116` | Instruction overlay |

## Ảnh raster (`res/drawable-nodpi`, WebP)

| File | Node Figma | Dùng ở |
|---|---|---|
| `bg_tavern` | nền `3:438` (= `3:572`, `3:335`, `3:41`, `3:11`…) | Nền quán rượu |
| `img_home_maker_bg` + `img_home_maker` | `3:439` (2 lớp) | Thẻ D&D maker |
| `img_home_cosplay` | `3:443` | Thẻ D&D cosplay |
| `img_home_lucky` | `3:441` | Thẻ D&D lucky roll |
| `img_home_creation` | `3:445` | Thẻ My creation |
| `img_intro_1` | `3:675` | Intro 1 |
| `img_intro_2` | `3:698` (export ghép 2 lớp) | Intro 2 |
| `img_intro_3` | `3:718` | Intro 3 |
| `img_permission` | `3:742` | Permission |
| `img_empty_character` | `3:1237` | Empty state My creation |
| `img_dice_gift` | `3:343` | Nút random-all (customize) |
| `img_color_wheel` | `3:1195` | Ô màu tuỳ chọn (text editor) |
| `img_add_image` | `3:870` | Ô chọn ảnh từ thư viện |
| `img_star_on` / `img_star_off` | `3:274` / `3:276` | Huy hiệu sao Cosplay successful |
| `img_rate_0` … `img_rate_5` | `69:187`, `69:240`, `69:260`, `69:280`, `69:300`, `69:320` | Emoji Rate us (0–5 sao) |
| `ic_flag_en` / `ic_flag_es` / `ic_flag_fr` / `ic_flag_hi` / `ic_flag_pt` / `ic_flag_in` | `3:625`, `3:632`, `3:639`, `3:646`, `3:653`, `3:660` | Cờ ngôn ngữ |

## Font

| File | Nguồn |
|---|---|
| `res/font/londrina_solid.ttf`, `londrina_solid_black.ttf`, `londrina_solid_family.xml` | Google Fonts – Londrina Solid (SIL OFL 1.1, xem `docs/licenses/OFL-LondrinaSolid.txt`) |
| `res/font/chela_one.ttf`, `cherry_bomb_one.ttf`, `bangers.ttf`, `pacifico.ttf`, `lobster.ttf`, `creepster.ttf`, `bungee.ttf` | Google Fonts (SIL OFL 1.1) – font cho Speech / Text ở Decor editor, giấy phép trong `docs/licenses/OFL-*.txt` |
