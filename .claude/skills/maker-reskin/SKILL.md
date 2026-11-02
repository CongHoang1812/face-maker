---
name: maker-reskin
description: Tạo app maker mới từ code base này khi chỉ đổi giao diện và API (Android, Kotlin, XML, Hilt). Dùng skill này mỗi khi người dùng muốn clone, nhân bản, reskin, đổi tên, làm app maker mới hoặc làm app "giống app này"; khi cần áp thiết kế Figma mới vào các màn hình có sẵn, đổi màu / font / ảnh / nút theo chủ đề mới; khi cần đổi APP_KEY, máy chủ, nối API mới hoặc sửa DTO / mapper; khi kiểm tra app có còn sót tên, ảnh, chữ của app cũ không, kể cả khi người dùng không nhắc đến skill. Use for "reskin", "clone this maker app", "new maker app", "swap the UI and API".
---

# Maker reskin

Mục tiêu là ra app mới nhanh mà **không đụng vào phần đã chạy đúng**. Tính năng, luồng màn hình và logic đều giữ nguyên. Chỉ ba thứ thay đổi: định danh, nguồn dữ liệu, giao diện.

Lý do của ranh giới này: các màn hình chỉ đọc domain model và view id. Giữ nguyên hai thứ đó thì có thể thay toàn bộ API và giao diện mà không phải sửa ViewModel hay test logic. Mỗi lần vượt ranh giới là mất lợi thế này, nên hãy dừng lại và hỏi nếu một yêu cầu buộc phải làm vậy.

## Xác định đang ở giai đoạn nào

Đọc `docs/APP_BRIEF.md` rồi chọn việc tiếp theo:

| Tình trạng | Việc cần làm |
|---|---|
| Không có `docs/APP_BRIEF.md` | Giai đoạn 1: tạo project |
| Có brief, chưa kiểm tra API | Giai đoạn 2: API |
| API xong, `audit.py` báo còn nhiều ảnh / màu cũ | Giai đoạn 3: giao diện |
| Giao diện xong | Giai đoạn 4: nội dung và nghiệm thu |

Người dùng yêu cầu rõ một giai đoạn thì làm đúng giai đoạn đó. Chỉ hỏi thông tin mà giai đoạn hiện tại thực sự cần. Ghi lại quyết định mới vào mục "Quyết định đã chốt" trong brief.

## Giai đoạn 1: Tạo project

Chạy `scripts/new_app.py` từ project nguồn. Script sao chép sang thư mục mới, đổi package, tiền tố class và theme, tên database, tên app trong mọi ngôn ngữ, `APP_KEY`, máy chủ, rồi tạo brief. Project nguồn không bị sửa.

Cần: tên app, package, `APP_KEY`, thư mục đích. Máy chủ để trống thì giữ như app nguồn.

```bash
python3 .claude/skills/maker-reskin/scripts/new_app.py --dest ../ten-app --app-name "Ten App" \
  --app-id com.conght.tenapp --app-key STxxx_TenApp --project-code STxxx
```

Sau đó vào project mới, chạy `./gradlew :app:assembleDebug :app:testDebugUnitTest` và báo kết quả. Phải có mốc xanh này trước khi đổi gì khác, để lỗi về sau chắc chắn do thay đổi mới gây ra. Đề xuất người dùng commit mốc này.

## Giai đoạn 2: API

Chạy `scripts/probe_api.py`:

- **KHỚP**: chỉ cần đổi cấu hình. Không sửa DTO hay mapper, chuyển sang giai đoạn 3.
- **KHÔNG KHỚP** hoặc có tài liệu API khác: đọc `references/api-adaptation.md` rồi sửa theo đúng phạm vi trong đó.

Kiểm tra API thật bằng probe và unit test, không chỉ dựa vào tài liệu. App mẫu từng gặp tài liệu ghi ngược ý nghĩa của `parts` và server chặn ảnh dù JSON vẫn trả 200.

## Giai đoạn 3: Giao diện

Đọc `references/ui-reskin.md`. Làm theo thứ tự: token, component, ảnh dùng chung, rồi từng màn hình. Thứ tự này giúp phần lớn màn hình đã gần đúng trước khi phải chỉnh từng màn.

Với Figma: nạp skill `figma-design-to-code` trước khi gọi `get_design_context`, và coi code Figma sinh ra chỉ là tài liệu tham khảo.

Mỗi lần chỉ làm một màn. Cập nhật cột trạng thái trong brief sau mỗi màn để phiên sau biết đang dở ở đâu.

## Giai đoạn 4: Nội dung và nghiệm thu

1. Chạy `scripts/audit.py`, xử lý hết mục LỖI.
2. Sửa chữ theo chủ đề mới trong `values/strings.xml` và **mọi** `values-*/strings.xml`. Dịch tự nhiên theo từng ngôn ngữ, không thay từ máy móc, vì `D&D` trong bản dịch có thứ tự từ khác nhau.
3. Chạy `./gradlew :app:assembleDebug :app:testDebugUnitTest`.
4. Nếu có thiết bị hoặc máy ảo, chạy app bằng skill `run` và đi hết luồng trong `references/ui-reskin.md`. Build xanh không chứng minh giao diện đúng. Không có thiết bị thì nói rõ là chưa kiểm tra trên máy.
5. Trước khi phát hành, chạy `scripts/audit.py --release`.

Khi báo cáo, nêu rõ: đã chạy lệnh nào, kết quả thật, phần nào chưa kiểm chứng.

## Ranh giới

Được đổi tự do:

- `res/` (giữ view id, xem `references/ui-reskin.md`)
- `core/widget/` phần vẽ, giữ nguyên API công khai
- `core/common/AppConfig.kt`, cấu hình trong `app/build.gradle.kts`
- `data/remote/` khi đổi API

Phải hỏi trước khi đổi:

- `data/model/`, `MakerRemoteDataSource`, repository
- ViewModel, luồng trong `navigation/nav_main.xml`
- Thêm thư viện, thêm hoặc bỏ màn hình, đổi cách lưu trữ
- Sửa test có sẵn để cho pass. Test đỏ thường là dấu hiệu đã vượt ranh giới; riêng test mapper thì được cập nhật khi API thực sự đổi định dạng.
