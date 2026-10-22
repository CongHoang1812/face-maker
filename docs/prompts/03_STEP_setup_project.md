# BƯỚC 1 – Setup project, dependency, Hilt, Navigation

> Prompt cho agent. Đọc trước `docs/prompts/00_README.md` (quy ước chung).

## Bối cảnh

Project Android có sẵn tại thư mục gốc, package `com.conght.facemakerapp`, AGP 9.4.1 (Kotlin built-in – **không** apply plugin `org.jetbrains.kotlin.android`), Gradle 9.6, version catalog `gradle/libs.versions.toml`, minSdk 24, compile/targetSdk 37, Java 11. Hiện chỉ có `MainActivity` + `activity_main.xml` mẫu.

## Yêu cầu

1. **Version catalog** – thêm (dùng bản stable mới nhất tương thích AGP 9 / Kotlin mà AGP đang bundle; kiểm tra khi sync, nếu lỗi thì nâng/hạ version cho khớp):
   - Plugin: `com.google.devtools.ksp` (KSP version phải khớp Kotlin version của AGP), `com.google.dagger.hilt.android`.
   - Thư viện:
     - Hilt: `hilt-android`, `hilt-compiler` (ksp)
     - AndroidX: `fragment-ktx`, `lifecycle-viewmodel-ktx`, `lifecycle-runtime-ktx`, `navigation-fragment-ktx`, `navigation-ui-ktx`, `recyclerview`, `viewpager2`, `core-splashscreen`, `datastore-preferences`, `browser` (Custom Tabs)
     - Room: `room-runtime`, `room-ktx`, `room-compiler` (ksp)
     - Coroutines: `kotlinx-coroutines-android`
     - Ảnh: `glide` (+ `ksp` compiler) **hoặc** `coil` (chọn Glide để đồng nhất với View)
     - Network (chuẩn bị cho API sau này): `retrofit`, `converter-gson` (hoặc kotlinx-serialization), `okhttp`, `logging-interceptor`
     - Shimmer: `com.facebook.shimmer:shimmer:0.5.0` (nếu không resolve được thì bước 4 sẽ tự viết ShimmerFrameLayout – ghi chú lại)
     - Test: `kotlinx-coroutines-test`, `turbine` (tuỳ chọn)
   - Không dùng Navigation Safe Args plugin (tránh xung đột AGP 9) – truyền argument bằng `bundleOf` + key hằng số.
2. **build.gradle.kts (root)**: khai báo `alias(...) apply false` cho ksp, hilt.
3. **app/build.gradle.kts**:
   - apply plugin ksp, hilt.
   - `buildFeatures { viewBinding = true; buildConfig = true }`.
   - `buildConfigField("String", "BASE_URL", "\"https://example.invalid/\"")` – placeholder, comment `// TODO(API): thay BASE_URL thật`.
   - `buildConfigField("boolean", "USE_MOCK_DATA", "false")`.
   - `androidResources { localeFilters += listOf("en","hi","es","fr","pt","in") }` (hoặc `resourceConfigurations` tuỳ AGP), bật `generateLocaleConfig = true` để hỗ trợ per-app language.
4. **Application**: tạo `FaceMakerApp : Application` gắn `@HiltAndroidApp`, khai báo trong `AndroidManifest.xml` (`android:name=".FaceMakerApp"`).
5. **MainActivity**: `@AndroidEntryPoint`, kế thừa `AppCompatActivity`, layout chứa `FragmentContainerView` (`NavHostFragment`, id `nav_host`), edge-to-edge (`enableEdgeToEdge()`), xử lý insets cho status bar (thiết kế có status bar trong suốt, nội dung vẽ dưới status bar).
6. **Navigation graph** `res/navigation/nav_main.xml` – tạo sẵn các destination rỗng (Fragment placeholder với TextView tên màn) để các bước sau điền:
   `splashFragment` (start), `languageFragment`, `introFragment`, `permissionFragment`, `homeFragment`, `settingFragment`, `templateSelectFragment`, `customizeFragment`, `decorEditorFragment`, `successFragment`, `myCreationFragment`, `creationDetailFragment`, `cosplayStartFragment`, `cosplayPlayFragment`, `cosplaySuccessFragment`, `luckyRollFragment`.
   Thêm các action theo luồng ở `01_ANALYSIS.md` mục 2.
7. **Manifest**: quyền `INTERNET`, `ACCESS_NETWORK_STATE`, `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE` (maxSdkVersion 32, chỉ để chọn ảnh nền), `POST_NOTIFICATIONS`; **không** khai báo `WRITE_EXTERNAL_STORAGE` (app chỉ lưu vào internal storage); `screenOrientation="portrait"`; `FileProvider` (authority `${applicationId}.fileprovider`, file `res/xml/file_paths.xml` gồm `<cache-path>` cho `cache/` và `<files-path>` cho `creations/` và `downloads/`).
8. Theme: `Theme.FaceMaker` kế thừa `Theme.Material3.DayNight.NoActionBar` nhưng **khoá light mode** (app không có dark theme), `windowLightStatusBar` theo màn, status bar trong suốt.
9. Xoá `ExampleUnitTest`/`ExampleInstrumentedTest` mặc định nếu không compile, hoặc giữ nguyên nếu vẫn chạy.

## Tiêu chí hoàn thành

- `./gradlew :app:assembleDebug` pass.
- App chạy lên, hiện `splashFragment` placeholder.
- Hilt sinh code thành công (có `Hilt_FaceMakerApp`).
- Liệt kê version đã chọn cho từng thư viện trong câu trả lời.
