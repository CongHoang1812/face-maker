# Character Maker (face-maker)

App Android tạo nhân vật theo lớp ảnh: chọn bộ phận và màu, trang trí (nền, sticker, bong bóng thoại, chữ), lưu, chia sẻ. Code base này được dùng làm **template** cho nhiều app maker, mỗi app chỉ khác giao diện và API.

## Tính năng

| Tính năng | Mô tả |
|---|---|
| Maker | Chọn nhân vật gốc, đổi bộ phận và màu, random, di chuyển / zoom / xoay / lật |
| Decor editor | Nền (màu, ảnh, pack), sticker, speech bubble, chữ với nhiều font |
| Cosplay | Dựng lại nhân vật mẫu trong 10 phút, chấm điểm theo % khớp |
| Lucky roll | Random một nhân vật hoàn chỉnh rồi chỉnh sửa tiếp |
| My creation | Hai tab Character / Design, chọn nhiều, xoá, tải về, chia sẻ |
| Khác | Đa ngôn ngữ (en, hi, es, fr, pt, in), onboarding, rate us, màn hình lỗi và Retry khi mất mạng |

## Công nghệ

- Kotlin, XML + ViewBinding (không dùng Compose)
- MVVM, single-activity + Navigation
- Hilt, Room, DataStore, Retrofit + OkHttp, Glide, Coroutines / Flow
- `minSdk` 24, `compileSdk` / `targetSdk` 37

## Cấu trúc

```
app/src/main/java/com/conght/facemakerapp/
├── core/      cấu hình, widget tự vẽ, tiện ích UI
├── data/      model, remote (API), local (Room, file), repository
├── di/        Hilt module
└── feature/   mỗi màn một package: Fragment + ViewModel
```

Luồng dữ liệu:

```
Fragment → ViewModel (StateFlow) → Repository → MakerRemoteDataSource → ApiService + ApiMapper
```

Domain model trong `data/model/` là ranh giới: API đổi thì chỉ sửa DTO và mapper, không sửa màn hình.

## Chạy và build

Yêu cầu: Android Studio bản mới, JDK 17 trở lên (dùng JDK đi kèm Android Studio là được).

**Android Studio:** mở thư mục project, chờ Gradle Sync, chọn cấu hình **app** và một thiết bị, bấm Run.

**Terminal** (cần `JAVA_HOME`):

```bash
sh gradlew :app:assembleDebug        # build APK debug
sh gradlew :app:testDebugUnitTest    # chạy unit test
```

Trên Windows dùng `.\gradlew.bat` thay cho `sh gradlew`.

## Cấu hình

| Mục | Nơi đặt |
|---|---|
| Máy chủ chính, máy chủ dự phòng, cờ `USE_MOCK_DATA` | `app/build.gradle.kts` (`buildConfigField`) |
| `APP_KEY`, đường dẫn ảnh, URL chính sách, email góp ý | `app/src/main/java/com/conght/facemakerapp/core/common/AppConfig.kt` |

- API chỉ dùng GET, không cần token. Máy chủ chính lỗi hoặc quá 6 giây thì tự chuyển sang máy chủ dự phòng.
- Đặt `USE_MOCK_DATA` thành `"true"` để làm giao diện bằng dữ liệu giả khi chưa có API. Nhớ tắt lại trước khi phát hành.
- Ảnh chỉ lưu trong internal storage của app, không ghi ra Gallery.

## Quy ước

- Không hardcode màu, kích thước, chữ trong layout: dùng `@color`, `@dimen`, `@string`.
- Thêm chuỗi thì thêm vào **mọi** `values-*/strings.xml`.
- Danh sách phụ kiện luôn có ô None ở đầu, do UI chèn bằng `noneFirst()`.
- Thiết kế 360 × 800, 1px Figma = 1dp.

Chi tiết hơn nằm ở [`CLAUDE.md`](CLAUDE.md).

## Tạo app mới từ template

Dùng khi app mới có cùng tính năng, chỉ khác giao diện và API. Bộ công cụ nằm ở `.claude/skills/maker-reskin/`:

```bash
python3 .claude/skills/maker-reskin/scripts/new_app.py \
  --dest ../ten-app --app-name "Ten App" \
  --app-id com.conght.tenapp --app-key STxxx_TenApp --project-code STxxx
```

Xem hướng dẫn từng bước (kể cả lệnh cho Windows) trong [`docs/NEW_APP_PLAYBOOK.md`](docs/NEW_APP_PLAYBOOK.md).

## Tài liệu

| File | Nội dung |
|---|---|
| [`docs/NEW_APP_PLAYBOOK.md`](docs/NEW_APP_PLAYBOOK.md) | Quy trình làm app maker mới |
| [`docs/assets.md`](docs/assets.md) | Danh sách ảnh và tài nguyên |
| [`docs/prompts/`](docs/prompts) | Các bước xây dựng ban đầu của app |
| [`docs/licenses/`](docs/licenses) | Giấy phép font (SIL OFL) |

## Trước khi phát hành

- Thay `PRIVACY_URL` và `FEEDBACK_EMAIL` còn là `example.com` trong `AppConfig.kt`.
- Kiểm tra `APP_KEY` và máy chủ là của app thật, không phải giá trị dùng tạm để test.
- Bản release hiện **tắt R8** (`optimization { enable = false }`). Nếu bật thì cần keep rule cho các model Gson và phải test bản release trên máy.
- Đặt `USE_MOCK_DATA` về `false`, kiểm tra `versionCode`, icon launcher và keystore ký app.
