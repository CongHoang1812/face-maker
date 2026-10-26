# D&D Character Maker – Bộ prompt xây dựng app từng bước

Bộ tài liệu này chia việc xây dựng app **D&D Character Maker** (Kotlin + XML Views + Hilt DI) thành các bước nhỏ.
Mỗi file `NN_*.md` là **một prompt độc lập**: copy toàn bộ nội dung file đó (hoặc nói "làm theo `docs/prompts/NN_*.md`") cho AI agent, chạy xong, build pass, rồi mới sang bước tiếp theo.

- Figma: https://www.figma.com/design/QDVSAOgH3rY4DuocNmN8nM/ST237_D-D-Character-Maker--Copy-
- fileKey: `QDVSAOgH3rY4DuocNmN8nM`
- Project: `com.conght.facemakerapp`, minSdk 24, compile/targetSdk 37, AGP 9.4.1 (Kotlin tích hợp sẵn trong AGP 9), Gradle 9.6

## Thứ tự thực hiện

| Bước | File | Nội dung |
|---|---|---|
| – | [01_ANALYSIS.md](01_ANALYSIS.md) | Phân tích màn hình, luồng điều hướng, màn hình bị thiếu (đọc trước, không phải prompt code) |
| – | [02_DESIGN_SYSTEM.md](02_DESIGN_SYSTEM.md) | Màu, font, kích thước, component dùng chung (tài liệu tham chiếu) |
| 1 | [03_STEP_setup_project.md](03_STEP_setup_project.md) | Gradle, dependency, Hilt, ViewBinding, Navigation |
| 2 | [04_STEP_architecture_di.md](04_STEP_architecture_di.md) | Package, model, repository, API placeholder, Hilt module |
| 3 | [05_STEP_design_system_resources.md](05_STEP_design_system_resources.md) | colors/dimens/styles/font/drawable, component dùng chung, export asset |
| 4 | [06_STEP_shimmer_loading.md](06_STEP_shimmer_loading.md) | Hiệu ứng skeleton mờ + dải sáng chạy qua |
| 5 | [07_STEP_splash_language_intro_permission.md](07_STEP_splash_language_intro_permission.md) | Splash, chọn ngôn ngữ, intro 1-3, permission |
| 6 | [08_STEP_home_settings.md](08_STEP_home_settings.md) | Home, Setting, Language trong setting |
| 7 | [09_STEP_dialogs.md](09_STEP_dialogs.md) | Exit / Reset / Random / Delete / Choose color / Rate us |
| 8 | [10_STEP_maker_template_select.md](10_STEP_maker_template_select.md) | Màn chọn nhân vật gốc (cata) |
| 9 | [11_STEP_maker_customize.md](11_STEP_maker_customize.md) | Màn customize nhân vật + chế độ Move |
| 10 | [12_STEP_decor_editor.md](12_STEP_decor_editor.md) | Background / Sticker / Speech bubble / Text |
| 11 | [13_STEP_success_save_share.md](13_STEP_success_save_share.md) | Lưu ảnh, màn Successful, share |
| 12 | [14_STEP_my_creation.md](14_STEP_my_creation.md) | My creation (Character / Design), chọn nhiều, xem chi tiết |
| 13 | [15_STEP_cosplay.md](15_STEP_cosplay.md) | Chế độ D&D cosplay (game) – **NEXT VER** |
| 14 | [16_STEP_lucky_roll.md](16_STEP_lucky_roll.md) | Character Random (Figma: D&D lucky roll) |
| 15 | [17_STEP_missing_screens_polish.md](17_STEP_missing_screens_polish.md) | Màn hình thiếu trong Figma, empty/error state, polish |
| – | [18_API_INTEGRATION_TODO.md](18_API_INTEGRATION_TODO.md) | Danh sách mọi chỗ để trống chờ API – dùng khi có API |

## Phạm vi VER 1.0 (theo `docs/plan.xlsx`, Sheet1)

`plan.xlsx` là kế hoạch tính năng gốc: cột **VER 1.0** = 1 làm ở bản 1.0, = 0 để **NEXT VER**. Khi các file md lệch với `plan.xlsx` thì theo `plan.xlsx`.

- **VER 1.0**: Splash, Intro, Permission, Language; Home (D&D Character Maker, Character Random, My Creation, Setting); Maker (Category → Customize → Add BG → Successful); Character Random; My Creation.
- **NEXT VER** (chưa làm ở 1.0):
  - Toàn bộ **Cosplay** (thẻ ở Home và mọi màn cosplay).
  - **List màu cho category** ở Customize.
  - Nút **Random** ở màn Character Random (bản 1.0 chỉ random 1 lần khi vào màn).
  - Nút **Edit** ở màn View của My Character.

## Quy ước bắt buộc cho MỌI bước (agent phải tuân thủ)

1. **Ngôn ngữ/UI**: Kotlin + XML layout + ViewBinding. Không dùng Jetpack Compose.
2. **Kiến trúc**: MVVM, single-activity + Navigation Component (Fragment). ViewModel expose `StateFlow<UiState>`; Fragment collect bằng `repeatOnLifecycle(STARTED)`.
3. **DI**: Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, module `@InstallIn(SingletonComponent)`). Không `new` repository trong ViewModel/Fragment.
4. **Chưa có API**:
   - Mọi dữ liệu "maker" (nhân vật mẫu, part, màu, pack background/sticker/speech, cosplay challenge, lucky roll) đi qua **Repository interface**. Implementation hiện tại gọi `RemoteDataSource` **placeholder** – trả về `Resource.Loading` và không bao giờ có data ⇒ UI hiển thị **shimmer** (khung ảnh mờ + dải sáng chạy qua).
   - Mọi hàm cần API để trống thân hàm với comment `// TODO(API): ...` – **không tự bịa endpoint, không tự bịa logic random**.
   - Có cờ `AppConfig.USE_MOCK_DATA` (mặc định `false`). Khi `true` dùng `MockDataSource` sinh dữ liệu giả (ô màu/placeholder) để test tương tác UI. Khi có API thật chỉ cần thay `RemoteDataSource`.
5. **Nút bỏ chọn ("None")**: Ở **tất cả** danh sách chọn phụ kiện/trang trí (part nhân vật, background color/image/pack, sticker, speech bubble…), **item đầu tiên luôn là ô "bỏ chọn"** (icon `ø` empty-set). Item này do app tự chèn ở UI layer (không đến từ API) và vẫn hiển thị kể cả khi đang shimmer.
6. **Không hardcode** màu/kích thước/chuỗi trong layout: dùng `@color`, `@dimen`, `@string` (có `values-hi`, `values-es`, `values-fr`, `values-pt`, `values-in`).
7. Mỗi bước kết thúc phải: `./gradlew :app:assembleDebug` thành công, không warning lint nghiêm trọng mới, và liệt kê file đã tạo/sửa.
8. Kích thước thiết kế gốc là **360 × 800 dp** – map 1px Figma = 1dp.
