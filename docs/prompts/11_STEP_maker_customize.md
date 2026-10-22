# BƯỚC 9 – Maker: Customize nhân vật + chế độ Move

> Prompt cho agent. Figma nodes: customize `3:335`, move `8:24`. Màn này được **tái sử dụng** cho Cosplay play (bước 13) và Edit từ Lucky roll / My creation – thiết kế thành component.

## Bố cục (360×800)
- Nền `bg_tavern_inside` + `overlay_20`.
- Top: back (16,60); nút **Next** dạng WoodButton nhỏ (phải, y=60).
- Cột tool trái (x=16): `RoundIconButton` reset (y=116), flip (y=160), move (y=204).
- `CharacterCanvasView` 328×328 căn giữa, y=116.
- Nút random-all (ảnh xúc xắc màu `ic_dice_color`, nền `orange_button`, viền trắng, bo 12) tại góc phải dưới canvas (x=316, y=400).
- Panel dưới cao 356dp nền `panel_brown`:
  1. **ColorBar**: RecyclerView ngang trong `bg_color_bar` (padding 16 ngang, 8 dọc), chấm màu 24dp, gap 16. Ẩn nếu item đang chọn không có màu.
  2. **Lưới item**: `GridLayoutManager(5)`, item 56×64 khung giấy `cell_tan`, padding ngang 24, gap 8, cuộn dọc.
     - **Ô 1 = None** (`ic_none`) – LUÔN có, chọn → `PartSelection(itemId = null)`.
     - **Ô 2 = Xúc xắc** (`ic_dice`) – random part của category hiện tại.
     - Các ô sau: item từ API. Ô đang chọn nền trắng.
  3. **Tab category** (đáy, cao 48): RecyclerView ngang, tab 56×48 bo trên 8 viền `brown_dark`, nền `cell_tan`, icon category; tab chọn nền trắng, cao 56, nhô lên 8dp.

## CharacterCanvasView (core/widget hoặc feature/maker)
- `FrameLayout` chứa nhiều `ImageView` layer xếp theo `PartCategory.zIndex`.
- `render(detail: TemplateDetail, config: CharacterConfig)`: với mỗi category lấy `PartSelection` → url layer (theo colorId, fallback màu đầu) → Glide load; None → ẩn layer.
- Áp `CharacterTransform` lên container: `translationX/Y`, `scaleX/Y` (flip = scaleX âm), `rotation`.
- `exportBitmap(): Bitmap` – vẽ view ra bitmap 1024×1024 (dùng ở Next / save).

## ViewModel (`CustomizeViewModel`, @HiltViewModel, dùng `SavedStateHandle`)
- Input: `templateId` hoặc `initialConfigJson` (khi Edit).
- State: `detail: Resource<TemplateDetail>`, `config: CharacterConfig`, `selectedCategoryId`, `mode: Edit|Move`, `isDirty`.
- Hành động:
  - `selectCategory(id)`, `selectItem(itemId?)`, `selectColor(colorId)`
  - `reset()` → về `detail.defaultConfig` (sau khi Reset dialog xác nhận)
  - `flip()` → đảo `transform.flipped`
  - `move(dx, dy)`, `rotate(deg)`, `zoom(factor)` (giới hạn scale 0.5–2.0, bước 10dp / 15° / 0.1)
  - `randomCategory()` → **để trống**:
    ```kotlin
    fun randomCategory() {
        // TODO(API): gọi makerRepository.randomizeCategory(templateId, selectedCategoryId) khi có API
    }
    ```
  - `randomAll()` → hiển thị Random dialog (Watch Ad) rồi gọi hàm **rỗng** `// TODO(API): makerRepository.randomizeCharacter(templateId)`.
- Lưu config vào `SavedStateHandle` (JSON) để không mất khi process death.

## Trạng thái loading (hiện tại luôn Loading vì chưa có API)
- Canvas: `view_skeleton_image` 328×328 có shimmer.
- ColorBar: 8 `item_skeleton_color_dot` shimmer.
- Lưới: ô None (thật) + ô Xúc xắc (thật, click → hàm rỗng) + 18 skeleton shimmer (dùng `ConcatAdapter`).
- Tab category: 6 skeleton tab shimmer.
- Next bị disable khi chưa Success.

## Chế độ Move (`8:24`)
- Nhấn nút move → panel dưới đổi sang `MovePanelView` (cùng nền `panel_brown`, chiếm vị trí panel): WoodButton "Exit" (trái trên), cột trái: xoay trái / xoay phải; giữa: D-pad 4 mũi tên tròn; phải: zoom + / −. Giữ nút để lặp (repeat 80ms).
- Cho phép kéo/pinch trực tiếp trên canvas ở chế độ Move (tuỳ chọn, nên có).
- Exit → về panel thường.

## Điều hướng
- Back khi `isDirty` → Exit dialog; Yes → popBackStack.
- **Next** → `exportBitmap()` lưu tạm vào `cacheDir/character_tmp.png`, đồng thời lưu bản Character vào My creation (`CreationRepository.save(CHARACTER, bitmap, configJson)`) → `decorEditorFragment` với `ARG_CHARACTER_PATH`, `ARG_CONFIG_JSON`.
- Tham số `mode`: `MAKER` (mặc định) | `COSPLAY` (bước 13 ẩn Next, hiện timer…) – thiết kế layout bằng `include`/flag để tái dùng.

## Tiêu chí hoàn thành
- Không mock: thấy shimmer ở canvas, thanh màu, lưới (trừ None & Xúc xắc), tab.
- Mock: chọn category/item/màu/None đổi canvas; reset, flip, move/rotate/zoom hoạt động; xúc xắc không làm gì (log TODO).
- Xoay màn hình/recreate không mất lựa chọn.
