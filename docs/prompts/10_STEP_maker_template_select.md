# BƯỚC 8 – Maker: chọn nhân vật gốc (cata)

> Prompt cho agent. Figma node `3:278`.

## UI
- Nền `bg_tavern`, back tròn góc trái (y=60).
- RecyclerView `GridLayoutManager(2)` – mỗi item là khung giấy cũ `cell_tan` (~148×124dp) bên trong ảnh trắng (thumbnail template). Gap theo Figma.

## Dữ liệu (CHƯA CÓ API)
- `TemplateSelectViewModel` (@HiltViewModel) inject `MakerRepository`, `state = repository.templates().stateIn(...)`.
- `Loading` → bọc lưới bằng `ShimmerFrameLayout` với `SkeletonAdapter(item_skeleton_template, count = 6)` – đây là trạng thái hiện tại vì API chưa có.
- `Success` → `TemplateAdapter` (ListAdapter + DiffUtil), load thumbnail bằng Glide (hoặc `MockImageLoader` nếu mock).
- `Error` → error view + Retry (gọi lại `retry()` trong ViewModel).
- Màn này **không** có ô None (đây là chọn nhân vật gốc, không phải phụ kiện).

## Điều hướng
- Click template → `customizeFragment` với `bundleOf(ARG_TEMPLATE_ID to id)`.

## Tiêu chí hoàn thành
- `USE_MOCK_DATA=false`: thấy 6 khung shimmer chạy mãi.
- `USE_MOCK_DATA=true`: sau 1.5s hiện 6 ô màu, click sang customize.
