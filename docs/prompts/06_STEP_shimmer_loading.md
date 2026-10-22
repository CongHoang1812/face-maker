# BƯỚC 4 – Hiệu ứng loading: khung ảnh mờ + dải sáng chạy qua

> Prompt cho agent. Đây là trạng thái UI hiển thị **trong lúc chờ API** – vì hiện chưa có API nên các màn maker sẽ ở trạng thái này liên tục.

## Yêu cầu hiệu ứng

- Mỗi khung ảnh (ô item, thẻ template, ảnh nhân vật, ảnh tham chiếu, chấm màu, tab category) thay bằng **khối skeleton cùng kích thước & bo góc** với màu `shimmer_base` (mờ, bán trong suốt, hợp nền tối/sáng).
- Một **dải sáng chéo** (gradient `transparent → shimmer_highlight → transparent`, góc ~20°, rộng ~40% view) chạy từ trái sang phải, lặp vô hạn, chu kỳ 1200ms, delay 300ms giữa các lần.
- Tất cả skeleton trong một màn chạy **đồng bộ** (cùng một dải sáng quét qua cả vùng) – bọc cả vùng bằng 1 `ShimmerFrameLayout`.
- Tự dừng animation khi view detached / ẩn (tiết kiệm pin).

## Triển khai

1. Nếu `com.facebook.shimmer` đã có ở bước 1: dùng `ShimmerFrameLayout` với `Shimmer.AlphaHighlightBuilder` hoặc `ColorHighlightBuilder` (base alpha 0.5, highlight alpha 1, tilt 20, duration 1200).
   Nếu không: tự viết `core/widget/ShimmerFrameLayout : FrameLayout` – override `dispatchDraw`, vẽ `LinearGradient` với `Matrix.setTranslate` animate bằng `ValueAnimator`, `PorterDuff.Mode.SRC_ATOP` để chỉ phủ lên pixel của child.
2. Layout skeleton dùng lại:
   - `item_skeleton_part.xml` (56×64, bo theo khung giấy)
   - `item_skeleton_decor.xml` (56×56, bo 4)
   - `item_skeleton_template.xml` (khung giấy cata)
   - `item_skeleton_color_dot.xml` (24 tròn)
   - `item_skeleton_category_tab.xml` (56×48 bo trên 8)
   - `view_skeleton_image.xml` (khối lớn, dùng cho ảnh nhân vật/tham chiếu)
3. `SkeletonAdapter(layoutRes, count)` – RecyclerView adapter chỉ inflate skeleton.
4. Helper `fun <T> bindResource(resource, shimmer: ShimmerFrameLayout, content: View, error: View?, onSuccess: (T) -> Unit)`:
   - `Loading` → shimmer VISIBLE + `startShimmer()`, content GONE.
   - `Success` → `stopShimmer()`, shimmer GONE, content VISIBLE, gọi `onSuccess`.
   - `Error` → shimmer GONE, hiện `error` view (bước 15 làm đẹp) có nút Retry.
5. **Ô "None" không bị shimmer**: trong lưới phụ kiện, ô đầu tiên (None) luôn là ô thật, có thể click; phần còn lại là skeleton. Cách làm: `ConcatAdapter(NoneAdapter, SkeletonAdapter/ItemAdapter)` dùng chung `GridLayoutManager`.
6. Ảnh tải bằng Glide khi đã có URL: dùng placeholder là drawable skeleton có shimmer nhỏ (hoặc `ColorDrawable(shimmer_base)`), crossfade 200ms.

## Tiêu chí hoàn thành

- Tạo màn demo tạm `ShimmerDemoFragment` (hoặc preview trong customize ở bước 9) cho thấy lưới skeleton + dải sáng chạy đều, đồng bộ.
- Không leak animator (kiểm tra `onDetachedFromWindow` dừng animator).
