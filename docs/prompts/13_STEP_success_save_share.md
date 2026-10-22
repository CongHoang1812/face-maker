# BƯỚC 11 – Màn Successful, lưu ảnh, share

> Prompt cho agent. Figma nodes: successful maker `3:1109`, successful cosplay `3:223` (bước 13 dùng lại).

## UI (`SuccessFragment`)
- Nền `editor_bg`. Top bar: trái nút **re-edit** (`ic_reset` dạng vòng xoay) ; phải 3 nút: **share**, **my creation** (`ic_archive`), **home**.
- Ảnh kết quả 328×328 nền trắng, căn giữa y=116 (load từ `Creation.imagePath`).
- WoodButton **Download** rộng ~112 dưới ảnh.
- Tham số: `ARG_CREATION_ID`, `ARG_SOURCE` (`MAKER` | `COSPLAY`), với COSPLAY thêm `ARG_STARS` (0–3): hiển thị huy hiệu tab giấy có 3 sao phía trên ảnh và viền ảnh `brown_primary` 2dp (theo `3:223`).

## Hành vi (toàn bộ local – làm đầy đủ)
- **Download**: `CreationRepository.download(listOf(id))` → copy ảnh vào **internal storage** `filesDir/downloads/` (qua `ImageFileStore.copyToDownloads`). **Không** dùng MediaStore / external storage, **không** xin quyền WRITE. Thành công → Snackbar/Toast "Saved" (`R.string.saved`); lỗi → Toast "Save failed". Hiện dialog loading trong lúc lưu.
- **Share**: `FileProvider.getUriForFile` → `Intent.ACTION_SEND` type `image/png`, `FLAG_GRANT_READ_URI_PERMISSION`.
- **Re-edit**: `popBackStack()` về editor trước đó (không mất state).
- **My creation**: navigate `myCreationFragment` (tab DESIGN mặc định), popUpTo home.
- **Home**: `popBackStack(R.id.homeFragment, false)`.
- Back hệ thống = Home.

## Tiêu chí hoàn thành
- Sau Download, file PNG có trong `/data/data/<package>/files/downloads/` (kiểm tra bằng Device File Explorer của Android Studio), **không** xuất hiện trong Gallery; share được sang app khác; các nút điều hướng đúng.
