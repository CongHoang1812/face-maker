# 18 – Checklist tích hợp API (dùng khi đã có API)

> Prompt cho agent ở thời điểm có tài liệu API. Đưa kèm file mô tả API (Swagger/Postman/JSON mẫu).

## Bước thực hiện

1. Tìm mọi điểm chờ API: `grep -rn "TODO(API)" app/src`. Danh sách dự kiến:

| Vị trí | Việc cần làm |
|---|---|
| `app/build.gradle.kts` – `BASE_URL` | Thay URL thật (tách debug/release nếu cần) |
| `data/remote/ApiService.kt` | Khai báo endpoint thật + DTO trong `data/remote/dto` |
| `data/remote/PendingRemoteDataSource.kt` | Đổi thành `ApiRemoteDataSource` gọi `ApiService`, map DTO → model |
| `getTemplates()` | Danh sách template cho màn cata |
| `getTemplateDetail(id)` | Category, item, màu, layer url, default config |
| `getDecorPacks(type)` | Pack background / sticker / speech |
| `randomizeCategory(templateId, categoryId)` | Ô xúc xắc trong lưới customize & cosplay |
| `randomizeCharacter(templateId)` | Nút xúc xắc random-all ở customize |
| `getRandomCosplayChallenge()` | Ảnh tham chiếu + đáp án cosplay |
| `getLuckyRoll()` | Kết quả lucky roll |
| `CustomizeViewModel.randomCategory()/randomAll()` | Gọi repository, cập nhật config |
| `ConfirmDialogs.random()` – `TODO(ADS)` | Tích hợp rewarded ad |
| My creation – `TODO(STICKER_EXPORT)` | WhatsApp/Telegram sticker |

2. Trong `DataSourceModule` bind `ApiRemoteDataSource` thay `PendingRemoteDataSource` (giữ `MockDataSource` cho debug).
3. Thêm cache (tuỳ chọn): OkHttp cache cho ảnh/JSON, hoặc Room cho template detail để mở offline.
4. Map lỗi: `IOException` → no internet, `HttpException` → server error → `Resource.Error` → `ErrorStateView` (đã có).
5. Không cần sửa Fragment/layout: UI đã xử lý `Loading → Success → Error`. Nếu phải sửa UI nghĩa là kiến trúc bước 2 bị vi phạm – báo lại.
6. Kiểm tra lại: ô **None** vẫn đứng đầu mọi danh sách phụ kiện sau khi có dữ liệu thật (API không cần trả item None).
7. Viết unit test cho mapper DTO → model và cho repository (fake ApiService).

## Tiêu chí hoàn thành
- `grep "TODO(API)"` không còn kết quả (hoặc chỉ còn những mục chủ động hoãn, có ghi chú).
- Mọi màn có shimmer chuyển sang dữ liệu thật; random hoạt động.
