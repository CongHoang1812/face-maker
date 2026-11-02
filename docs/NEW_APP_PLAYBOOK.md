# Playbook: làm app maker mới

Dùng khi app mới có **cùng tính năng** với app này, chỉ khác giao diện và API.

Tính năng, luồng màn hình và logic giữ nguyên. Chỉ đổi 3 thứ, theo thứ tự:

```
1. Định danh  →  2. API  →  3. Giao diện  →  4. Nội dung và nghiệm thu
   (script)      (probe)    (Claude + Figma)   (audit)
```

## Bộ công cụ

| File | Dùng để |
|---|---|
| `.claude/skills/maker-reskin/` | Skill hướng dẫn Claude làm đúng quy trình và đúng phạm vi |
| `scripts/new_app.py` | Sao chép project và đổi package, tên, theme, `APP_KEY`, máy chủ |
| `scripts/probe_api.py` | Kiểm tra server thật có khớp định dạng app đang đọc không |
| `scripts/audit.py` | Tìm phần sót của app cũ và cho biết đã thay bao nhiêu ảnh, màu |
| `CLAUDE.md` | Quy ước dự án, Claude đọc tự động |
| `docs/APP_BRIEF.md` | Được tạo trong project mới; Claude đọc để biết app đang làm gì |

Các file nói trên được sao chép sang project mới, nên app nào cũng có thể làm mẫu cho app sau.

## Bước 1: Tạo project

Chạy tại project mẫu:

```bash
python3 .claude/skills/maker-reskin/scripts/new_app.py \
  --dest ../anime-oc-maker \
  --app-name "Anime OC Maker" \
  --app-id com.conght.animeocmaker \
  --app-key ST279_AnimeOCMaker \
  --project-code ST279 \
  --base-url https://lvtglobal.tech/ \
  --backup-url https://lvt-api-tech.io.vn/
```

Sau đó:

```bash
cd ../anime-oc-maker
git init && git add . && git commit -m "Tạo project từ template"
sh gradlew :app:assembleDebug :app:testDebugUnitTest
```

Mở `docs/APP_BRIEF.md`, điền Figma, chủ đề, tài liệu API. **Brief càng rõ, prompt càng ngắn.**

## Lệnh cho Windows

Các lệnh trong tài liệu này viết cho Linux / macOS. Trên Windows có hai cách:

- **Git Bash hoặc WSL**: dùng nguyên các lệnh ở trên, không phải đổi gì.
- **PowerShell**: đổi theo bảng dưới.

| Linux / macOS | Windows (PowerShell) |
|---|---|
| `python3` | `python` (hoặc `py`) |
| `\` để xuống dòng | gõ liền một dòng (hoặc dùng dấu huyền `` ` ``) |
| `a && b` | gõ hai dòng riêng (PowerShell 5 không có `&&`) |
| `sh gradlew ...` | `.\gradlew.bat ...` |
| `/` trong đường dẫn | `\` hoặc `/` đều được |

Cần Python 3.9 trở lên, và đánh dấu "Add Python to PATH" khi cài.

Tạo project (Bước 1):

```powershell
python .claude\skills\maker-reskin\scripts\new_app.py --dest ..\anime-oc-maker --app-name "Anime OC Maker" --app-id com.conght.animeocmaker --app-key ST279_AnimeOCMaker --project-code ST279 --base-url https://lvtglobal.tech/ --backup-url https://lvt-api-tech.io.vn/

cd ..\anime-oc-maker
git init
git add .
git commit -m "Tạo project từ template"
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

Kiểm tra API (Bước 2) và nghiệm thu (Bước 4):

```powershell
python .claude\skills\maker-reskin\scripts\probe_api.py
python .claude\skills\maker-reskin\scripts\audit.py
python .claude\skills\maker-reskin\scripts\audit.py --release
```

Nếu `gradlew.bat` báo thiếu `JAVA_HOME`, mở project bằng Android Studio và build bằng nút Run (Android Studio dùng JDK đi kèm).

## Bước 2: API

```bash
python3 .claude/skills/maker-reskin/scripts/probe_api.py
```

Nếu báo **KHỚP** thì xong bước này. Nếu **KHÔNG KHỚP**, mở `claude` trong project mới và dùng prompt:

```text
/maker-reskin Làm giai đoạn API. Tài liệu API: <đường dẫn hoặc link>.
JSON mẫu thật: <đường dẫn file>. Viết test mapper theo JSON này trước.
```

## Bước 3: Giao diện

Làm lần lượt từng prompt, mỗi lần chạy xong thì xem kết quả và commit.

**Token và component:**

```text
/maker-reskin Làm bước token và component chung theo Figma: <link>.
Lấy màu, font, kiểu nút, kiểu dialog. Chưa sửa từng màn hình.
Xong thì liệt kê những gì Figma không rõ để mình quyết định.
```

**Ảnh dùng chung:**

```text
/maker-reskin Xuất ảnh từ Figma: <link>, ghi đè đúng tên file hiện có.
Xong chạy audit.py và cho mình biết còn ảnh nào của app cũ, ảnh nào Figma không có.
```

**Lập bảng màn hình** (làm một lần):

```text
/maker-reskin Đọc Figma <link>, điền cột Figma node trong bảng màn hình ở docs/APP_BRIEF.md.
Màn nào không ghép được hoặc có màn mới thì hỏi mình, đừng đoán.
```

**Từng màn hình** (lặp lại cho mỗi màn):

```text
/maker-reskin Làm màn Home theo node trong brief.
Giữ view id, không sửa ViewModel. Chạy app và so với ảnh Figma.
```

## Bước 4: Nội dung và nghiệm thu

```text
/maker-reskin Làm giai đoạn nghiệm thu: sửa chữ theo chủ đề trong mọi ngôn ngữ,
chạy audit.py, build, test, rồi đi hết luồng trên máy ảo. Báo cáo những gì đã kiểm tra thật.
```

Trước khi phát hành:

```bash
python3 .claude/skills/maker-reskin/scripts/audit.py --release
```

Lệnh này báo các mục cần xử lý: URL chính sách bảo mật và email còn là `example.com`, quảng cáo và sticker còn là stub, bản release đang tắt R8.

## Cách viết prompt hiệu quả

- **Mỗi prompt một việc có điểm kết thúc rõ.** "Làm màn Home" tốt hơn "đổi hết giao diện", vì dễ kiểm tra và dễ sửa.
- **Đưa dữ liệu thật:** link Figma có `node-id`, JSON thật, ảnh chụp lỗi. Dữ liệu tốt hơn mô tả bằng lời.
- **Nói điều không được làm:** "giữ view id", "không sửa ViewModel". Skill đã có sẵn, nhưng nhắc lại khi việc dễ vượt phạm vi.
- **Yêu cầu bằng chứng:** "chạy test và cho xem kết quả", "so với ảnh Figma".
- **Sửa theo triệu chứng cụ thể:** "nút Next cách mép phải 16dp, đang là 8dp" tốt hơn "chưa giống Figma".
- **Gặp lỗi lặp lại thì đưa vào kit:** thêm vào `references/` trong skill, các app sau sẽ không bị lại.

Tiết kiệm lượt dùng:

- `/clear` giữa các màn hình. Tiến độ đã nằm trong brief và git.
- `/effort medium` cho việc thông thường, tăng lên `high` với Customize, Decor editor hoặc khi đổi API.

## Khi app mới không chỉ khác giao diện và API

Ghi vào mục "Khác biệt so với app mẫu" trong brief rồi nói rõ trong prompt. Skill sẽ dừng lại hỏi nếu cần vượt ranh giới.

```text
/maker-reskin App này bỏ tính năng Cosplay. Lập kế hoạch các file cần đổi trước, chưa sửa code.
```

## Xử lý sự cố

| Triệu chứng | Nguyên nhân thường gặp |
|---|---|
| Ảnh trống nhưng JSON vẫn có | Server chặn ảnh, chạy `probe_api.py` |
| Bộ phận đè sai thứ tự | Sai `zIndex` hoặc đảo X-Y của `parts` trong mapper |
| Ảnh lỗi 401 dù API chạy được | Glide bỏ interceptor, xem `references/api-adaptation.md` |
| Lỗi `Unresolved reference: binding.xxx` | Layout mất view id, thêm lại |
| Hai ô None trong danh sách | Mapper tự thêm None, UI đã chèn sẵn |
| Nút vẫn có vết mẻ gỗ | Hình vẽ bằng code trong `WoodButtonDrawable.kt` |
| Không chạy được `./gradlew` | Thiếu quyền chạy, dùng `sh gradlew` |

## Giữ template luôn tốt

- Sửa lỗi chung ở một app thì đưa lại về template, nếu không các app sau sẽ lặp lại lỗi đó.
- Khi có đủ 2–3 app, cân nhắc tách `core/`, `data/` thành module dùng chung. Chưa cần làm sớm.
