# BƯỚC 15 – Màn hình/trạng thái thiếu trong Figma & hoàn thiện

> Prompt cho agent. Các màn dưới đây KHÔNG có trong Figma – tự thiết kế đúng design system (`02_DESIGN_SYSTEM.md`): nền giấy rách `cell_tan`, chữ Londrina Solid, nút WoodButton, màu `brown_primary`.

## 1. LoadingDialog
- Thẻ giấy 200×140, `ProgressBar` ngang kiểu splash (ô vuông cam) hoặc vòng tròn tint `brown_primary`, text "Saving..." / "Loading...". Không huỷ được bằng chạm ngoài.
- Dùng khi: Next ở customize, Download ở decor editor, export nhiều ảnh ở My creation.

## 2. ErrorStateView (thay shimmer khi `Resource.Error`)
- Compound view: icon (ảnh nhân vật buồn hoặc `ic_info`), text "Something went wrong" / "No internet connection" (phân biệt bằng `IOException`), WoodButton "Retry".
- Kích thước co theo vùng chứa (lưới item, khung ảnh). Tích hợp vào helper `bindResource` của bước 4.
- `NetworkMonitor` (ConnectivityManager callback, inject Hilt) – khi có mạng trở lại tự retry các màn đang Error.

## 3. Empty Design tab
- Dùng chung layout empty của Character, text "No design here".

## 4. Snackbar/Toast style
- `showAppToast(message)`: thẻ nhỏ nền `brown_primary` bo 16 chữ trắng, hiện 2s ở đáy.

## 5. Cosplay
- Nút Done ở play screen, time-up dùng score popup, Exit dialog khi back (đã nêu ở bước 13 – kiểm tra lại).

## 6. Rate us trigger
- Hiện RateUsDialog khi: người dùng lưu thành công lần thứ 2, hoặc back ở Home lần đầu (nếu `rated == false`). Không hiện quá 1 lần/ngày.

## 7. Polish chung
- Hiệu ứng nhấn cho mọi nút (scale 0.95, 100ms), ripple tắt cho WoodButton.
- Transition fragment: fade 150ms.
- Âm thanh/haptic khi chọn item: `performHapticFeedback(KEYBOARD_TAP)`.
- Kiểm tra màn nhỏ (360×640) và lớn (412×915): panel dưới cố định 356dp, canvas co lại (giữ tỉ lệ vuông) nếu thiếu chiều cao – dùng ConstraintLayout `layout_constraintDimensionRatio="1:1"` + `layout_constraintHeight_max`.
- Insets: nội dung top bar không bị status bar che (padding top = inset + 16).
- Kiểm tra mọi danh sách phụ kiện có **None ở đầu**: customize parts, background color/image/pack, sticker, speech bubble. Viết UI test (Espresso) hoặc unit test cho adapter đảm bảo `position 0 == NoneItem`.
- Chạy `./gradlew lint` và sửa lỗi mới.

## Tiêu chí hoàn thành
- Mọi trạng thái Loading / Error / Empty có UI; app không crash khi tắt mạng.
