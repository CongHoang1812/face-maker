# BƯỚC 12 – My creation

> Prompt cho agent. Figma section `3:1228`: empty `3:1229`/`3:1243`, list `3:1257`/`3:1359`, select `3:1437`/`3:1515`, detail `3:1583`/`3:1608`.

## Màn danh sách (`MyCreationFragment`)
- Header: ảnh nền `bg_tavern` cắt phần trần (cao ~116), back tròn trái; khi ở chế độ chọn: bên phải thêm 3 nút tròn **xoá**, **share**, **download**.
- 2 tab dạng "tab giấy" ở đáy header: **Character** / **Design** (96×40, bo trên 8). Tab chọn: nền `cream`, chữ `brown_primary`, cao hơn; tab không chọn: nền `brown_primary`, chữ trắng.
- Thân nền `cream`. Dùng `ViewPager2` + 2 fragment con (hoặc 1 RecyclerView đổi data) – đọc từ `CreationRepository.observe(type)` (Room Flow, **dữ liệu thật**).
- **Empty**: ảnh `img_empty_character` + "No character here" / "No design here" `brown_primary`.
- **Item** (160×180, khung giấy `cell_tan` + đổ bóng 0,4,2 25%): ảnh 136×136 bo 4; dưới ảnh:
  - Character: nút nhỏ **xoá** + **sửa** (24dp, nền cream viền nâu bo 8).
  - Design: chỉ nút **xoá**.
- Lưới 2 cột, gap 8 ngang / 16 dọc, padding 16.

## Chế độ chọn nhiều
- Long-press item → vào selection mode: nút xoá/sửa của item thay bằng **checkbox** vuông bo 4 (chưa chọn: xám; chọn: nền cream + ✓).
- Header hiện xoá / share / download cho các item đã chọn.
- Riêng tab Character: thanh dưới cùng 2 WoodButton **Add to Telegram** / **Add to Whatsapp** → stub `// TODO(STICKER_EXPORT): tích hợp WhatsApp sticker / Telegram` (hiện Toast "Coming soon").
- Back khi đang selection → thoát selection mode.
- Xoá → Delete dialog → `CreationRepository.delete(ids)` (xoá cả file).
- Share nhiều → `ACTION_SEND_MULTIPLE`. Download → `CreationRepository.download(ids)` (lưu vào internal storage `filesDir/downloads/`, Toast "Saved").

## Chi tiết (`CreationDetailFragment`)
- Nền `editor_bg`; top bar: back, và bên phải xoá / share / download.
- Ảnh 328×328 nền trắng.
- Character: WoodButton **Edit** → `customizeFragment` với `ARG_CONFIG_JSON` của creation (nếu template detail chưa load được thì customize vẫn ở trạng thái shimmer – OK).
- Design: không có Edit.
- Nút sửa (pencil) trên item danh sách = đi thẳng Edit như trên.

## Tiêu chí hoàn thành
- Tạo nhân vật/design ở bước 9–10 → xuất hiện đúng tab. Xoá đơn, xoá nhiều, share, download hoạt động. Empty state hiện khi rỗng.
