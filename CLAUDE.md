# Maker app

App Android tạo nhân vật theo lớp ảnh: chọn bộ phận, trang trí, lưu, chia sẻ. Code base này được tái sử dụng cho nhiều app, mỗi app chỉ khác giao diện và API. Thông tin riêng của app, nếu có, nằm ở `docs/APP_BRIEF.md`.

Khi tạo app mới, đổi giao diện hoặc đổi API: dùng skill `maker-reskin`. Hướng dẫn cho người: `docs/NEW_APP_PLAYBOOK.md`.

## Lệnh

```bash
sh gradlew :app:assembleDebug
sh gradlew :app:testDebugUnitTest
```

## Kiến trúc

Kotlin, XML + ViewBinding (không dùng Compose), MVVM, single-activity + Navigation, Hilt, Room, DataStore, Retrofit, Glide.

```
core/      cấu hình, widget, tiện ích UI
data/      model, remote (API), local (Room, file), repository
di/        Hilt module
feature/   mỗi màn một package: Fragment + ViewModel
```

Luồng dữ liệu: `Fragment → ViewModel (StateFlow) → Repository → MakerRemoteDataSource`. Domain model trong `data/model/` là ranh giới: API đổi thì sửa DTO và mapper, không sửa màn hình.

## Quy ước

- Không hardcode màu, kích thước, chữ trong layout. Dùng `@color`, `@dimen`, `@string`.
- Thêm chuỗi thì thêm vào **mọi** `values-*/strings.xml`.
- Danh sách phụ kiện luôn có ô None ở đầu, do UI chèn qua `noneFirst()`, không lấy từ API.
- Data source ném exception khi lỗi, `resourceFlow` đổi thành `Resource.Error` để UI hiện Retry.
- Thiết kế 360 × 800, 1px Figma = 1dp.
- Ảnh của app lưu ở internal storage. Chỉ nút Download mới ghi ra Gallery (`Pictures/<tên app>`).

Cấu hình app (`APP_KEY`, máy chủ, cờ mock) nằm ở `core/common/AppConfig.kt` và `app/build.gradle.kts`.
