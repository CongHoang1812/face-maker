# Đổi API

Đọc file này khi `probe_api.py` báo KHÔNG KHỚP hoặc khi có API khác định dạng.

## Điểm nối duy nhất

```
Fragment → ViewModel → Repository → MakerRemoteDataSource → ApiRemoteDataSource → ApiService + ApiMapper
                                    └── điểm nối ──┘        └── phần được thay ───────────────────┘
```

Mọi thứ phía trái `MakerRemoteDataSource` chỉ biết `data/model/`. Vì vậy đổi API chỉ nên chạm vào:

| File | Khi nào sửa |
|---|---|
| `AppConfig.kt`, `app/build.gradle.kts` | Đổi `APP_KEY`, đường dẫn ảnh, máy chủ |
| `data/remote/ApiService.kt` | Endpoint đổi |
| `data/remote/dto/ApiDtos.kt` | JSON đổi |
| `data/remote/ApiMapper.kt` | Quy tắc ghép URL hoặc cấu trúc đổi |
| `data/remote/ApiRemoteDataSource.kt` | Cách gọi đổi (API có sẵn random, phân trang, gọi detail riêng...) |
| `di/NetworkModule.kt` | Header, token, timeout |
| `app/src/test/.../data/remote/` | Test mapper và data source theo định dạng mới |

Nếu buộc phải sửa Fragment hoặc ViewModel, gần như chắc chắn mapper chưa làm đủ việc. Hãy xem lại trước khi sửa.

## Hợp đồng UI cần

Giao diện phụ thuộc vào các đảm bảo này. Tài liệu API mới có thể không nói gì về chúng, mapper phải tự đảm bảo.

**`getTemplates()`**: danh sách đã sắp xếp. `id` ổn định vì được truyền qua navigation và lưu trong creation. Bỏ template không có bộ phận hợp lệ.

**`getTemplateDetail(id)`**

- `categories` sắp theo thứ tự tab. Category đầu tiên có `isRequired = true` (thân nhân vật).
- `zIndex` là thứ tự vẽ layer, số lớn nằm trên. Đây là chỗ dễ sai nhất và unit test không phát hiện được: phải nhìn ảnh thật.
- `defaultConfig.selections` có **đủ key** cho mọi category. Category không bắt buộc để `itemId = null`.
- `PartItem.colors` rỗng nghĩa là item không đổi màu, khi đó `layerUrl` phải có giá trị.
- `PartColor.id` và `PartItem.id` ổn định: cosplay chấm điểm bằng cách so sánh các id này.

**`getDecorPacks(type)`**: bỏ nhóm rỗng. `name` để trống thì UI tự hiện "Pack N".

**`randomize*`, `getRandomCosplayChallenge`, `getLuckyRoll`**: API mẫu không có endpoint này nên app tự random bằng `CharacterRandomizer` và ghép ảnh bằng `CharacterImageComposer`. Giữ cách này trừ khi API mới bắt buộc khác.

**Không tự chèn ô None**: UI chèn bằng `noneFirst()`. Nếu mapper cũng thêm thì danh sách sẽ có hai ô None.

## Xử lý lỗi

Hàm data source **ném exception** khi lỗi, không trả danh sách rỗng. `resourceFlow` đổi exception thành `Resource.Error` để UI hiện Retry; danh sách rỗng sẽ hiện như không có dữ liệu và người dùng không có cách thử lại.

`ErrorStateView` hiện "No internet" khi chuỗi nguyên nhân có `IOException`, các lỗi khác hiện thông báo chung.

## Những chỗ dễ sót

- **Token cho ảnh**: `core/ui/*GlideModule.kt` và `OkHttpAssetProbe` đều gọi `interceptors().clear()`. Interceptor thêm ở `NetworkModule` chỉ tác dụng với JSON, ảnh sẽ bị 401. Nếu ảnh cần header, phải thêm riêng ở hai chỗ này.
- **Ảnh từ máy chủ dự phòng**: `getDecorPacks` luôn ghép URL ảnh từ `BASE_URL`, còn ảnh nhân vật lấy theo máy chủ đã trả dữ liệu. Giữ nguyên hay đổi là quyết định riêng, đừng đổi ngầm.
- **Server chặn ảnh nhưng JSON vẫn 200**: `AssetProbe` kiểm tra một ảnh mẫu trước khi nhận dữ liệu. Giữ bước này với API mới và trỏ nó vào một URL ảnh thật.
- **Cache trong phiên**: dữ liệu giữ trong bộ nhớ qua `Mutex`, lần lỗi không được cache để Retry gọi lại được. Giữ tính chất này nếu viết lại.
- **Mã hoá URL**: tên thư mục có thể có dấu cách. Dùng `encodePath()`, không nối chuỗi thô.
- **Tên nhóm decor khác nhau giữa các app**: ST279 dùng `sticker` / `speech bubble`, ST247 dùng `stickers` / `speech_bubble`, ở cả key JSON và thư mục ảnh.
- **Mã màu là tên thư mục**: phân biệt hoa thường và có thể có đuôi (`BFCBBF_2`). URL phải giữ nguyên mã, chỉ `PartColor.hex` mới được chuẩn hoá.
- **Gson không đảm bảo non-null**: mọi field DTO để nullable, xử lý mặc định trong mapper.
- **Timeout 6 giây** ở `NetworkModule` là ngưỡng chuyển máy chủ dự phòng. Dữ liệu lớn hơn nhiều thì cần xem lại.

## Quy trình

1. Lấy JSON thật từ server (không chỉ tài liệu) và đưa một mẫu nhỏ vào test.
2. Viết hoặc sửa test mapper trước theo JSON đó.
3. Sửa DTO, mapper, service.
4. Chạy `./gradlew :app:testDebugUnitTest`.
5. Chạy app: chọn template, đổi nhiều bộ phận và màu, đối chiếu thứ tự lớp với ảnh mẫu. Bật random vài lần để phát hiện URL ảnh 404.
6. Nếu sửa định dạng thì cập nhật URL trong `probe_api.py` để lần sau vẫn dùng được.
7. Chưa có API thì bật `USE_MOCK_DATA` trong `app/build.gradle.kts` để làm giao diện trước, và nhớ tắt lại.
