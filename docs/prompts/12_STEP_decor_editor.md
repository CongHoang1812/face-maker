# BƯỚC 10 – Decor editor: Background / Sticker / Speech bubble / Text

> Prompt cho agent. Figma nodes: add color `3:773`, add image `3:843`, sticker `3:913`, speech `3:993`, text `3:1155`.

## Bố cục
- Nền `editor_bg`. Top bar: back (trái), **reset** (về mặc định như lúc vào màn, qua Reset dialog), **Save** (phải; Figma vẽ icon download).
- `DecorCanvasView` 328×328 căn giữa y=116 (vuông, bo 0), gồm các lớp:
  1. Background (màu hoặc ảnh, `centerCrop`) – mặc định trắng
  2. Ảnh nhân vật (từ `ARG_CHARACTER_PATH`) – kéo thả, chỉnh kích thước được (như `TransformableItemView` nhưng không có nút delete)
  3. Các `TransformableItemView` (sticker / speech bubble / text), có thể nhiều.
- Panel dưới 356dp `panel_brown`:
  1. **Hàng tab chính** (4 icon 32dp, gap 32, căn giữa): Background, Sticker, Speech, Text. Tab chọn: icon màu cam `orange_button`, còn lại xám. Vào màn mặc định chọn tab Background.
  2. **Hàng tab con**: WoodButton rộng 116 cuộn ngang (chọn: orange, không chọn: gray).
  3. **Nội dung**: lưới 5 cột ô 56×56 trắng bo 4, gap 8.

## Tab Background
- Tab con: **Image**, **Color** (plan.xlsx chỉ có 2 tab này, không có tab Pack).
- Image: [None] [Thêm ảnh → Photo Picker `PickVisualMedia(ImageOnly)`] + ảnh người dùng đã chọn gần đây trong phiên + **list ảnh nền** từ API. Phần từ người dùng → **làm đầy đủ**; list ảnh nền (API) hiện là skeleton shimmer.
- Color: [None] [Bánh xe màu → ColorPickerDialog] + danh sách màu local cố định (khoảng 18 màu đặt trong `arrays.xml`). → **làm đầy đủ, không cần API**.
- None → background về trắng.

## Tab Sticker
- Tab con: các pack sticker (API). Lưới: **[None]** + sticker.
- Chạm sticker → thêm `TransformableItemView` vào giữa canvas. None → xoá item sticker đang được chọn trên canvas (nếu có) / bỏ chọn.
- Hiện tại: shimmer (tab con + lưới), riêng ô None hoạt động.

## Tab Speech bubble
- Tab con: pack bubble (API). Lưới: **[None]** + bubble.
- Thêm 2 hàng phía trên lưới (theo `3:993`): hàng **font** (chip tròn 32 hiển thị "Aa" bằng font tương ứng, ô đầu "T" = font mặc định) và hàng **màu chữ** (ô đầu bánh xe màu → ColorPicker, sau đó các màu cố định).
- Chạm item bubble → thêm lên canvas và mở ô nhập text ngay (text mặc định "Hello"); chạm 2 lần bubble trên canvas để sửa lại text.
- Font list là local (5–8 font Google Fonts OFL trong `res/font`), màu local → làm đầy đủ. Pack bubble từ API → shimmer.

## Tab Text
- Hàng nhập: `EditText` nền trắng viền cam bo 4 + nút ✓ (WoodButton vuông nhỏ). ✓ → thêm text mới lên canvas (hoặc cập nhật text đang chọn).
- Hàng font + hàng màu như speech. Toàn bộ local → làm đầy đủ.

## TransformableItemView
- Khi chọn: viền nét đứt `brown_primary` + 3 handle tròn nhỏ: **flip** (trên-phải), **delete** (dưới-trái), **resize/rotate** (dưới-phải, kéo để scale + xoay quanh tâm).
- Kéo thân để di chuyển; pinch 2 ngón để scale/rotate. Chạm vùng trống canvas → bỏ chọn tất cả.
- Model state: `DecorElement(id, type, source(url|text), x, y, scale, rotation, flipped, fontId?, color?)` giữ trong `DecorEditorViewModel` (SavedStateHandle) để không mất khi recreate.

## ViewModel (`DecorEditorViewModel`)
- Inject `DecorRepository`, `CreationRepository`.
- `packs(type)` → `Resource` cho mỗi `DecorType` (hiện luôn Loading).
- State: `background`, `elements`, `selectedElementId`, `isDirty`.
- `save()`: canvas → bitmap (bỏ viền chọn trước khi vẽ) → `CreationRepository.save(DESIGN, bitmap, null)` → trả id.

## Điều hướng
- Back với `isDirty` → Exit dialog.
- Save → hiện loading dialog (bước 15) → `save()` → `successFragment(ARG_CREATION_ID, ARG_SOURCE = MAKER)`.

## Tiêu chí hoàn thành
- Không mock: Color, Image, Text, font, màu, reset, kéo thả/chỉnh kích thước nhân vật chạy đầy đủ; list ảnh nền và pack sticker/speech hiện shimmer; ô None ở mọi lưới phụ kiện luôn đứng đầu và hoạt động.
- Mock: thêm được sticker/bubble, kéo/xoay/scale/flip/xoá ổn định.
