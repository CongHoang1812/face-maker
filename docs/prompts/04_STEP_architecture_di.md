# BƯỚC 2 – Kiến trúc, model, repository, API placeholder, Hilt module

> Prompt cho agent. Đọc `00_README.md` và `01_ANALYSIS.md` mục 3 trước.

## Cấu trúc package

```
com.conght.facemakerapp
├─ FaceMakerApp.kt, MainActivity.kt
├─ core/
│  ├─ common/        Resource.kt, AppConfig.kt, DispatcherProvider.kt, Event helpers
│  ├─ ui/            base/BaseFragment.kt (ViewBinding), extensions (collectWhenStarted, dp, setOnSingleClick)
│  └─ widget/        (bước 3-4: WoodButton, RoundIconButton, ShimmerFrameLayout…)
├─ data/
│  ├─ model/         domain model (data class thuần)
│  ├─ remote/        ApiService.kt, dto/, RemoteDataSource.kt (placeholder), MockDataSource.kt
│  ├─ local/         db/ (Room), datastore/ (SettingsDataStore), file/ (ImageFileStore)
│  └─ repository/    interface + impl
├─ di/               NetworkModule, DatabaseModule, RepositoryModule, DataSourceModule, DispatcherModule
└─ feature/
   ├─ splash/ language/ intro/ permission/ home/ setting/
   ├─ maker/ (template, customize, move)
   ├─ decor/ (editor, background, sticker, speech, text)
   ├─ success/ creation/ cosplay/ luckyroll/
   └─ dialog/
```

## Model (data/model)

```kotlin
data class CharacterTemplate(val id: String, val name: String, val thumbnailUrl: String)

data class TemplateDetail(
    val templateId: String,
    val categories: List<PartCategory>,
    val defaultConfig: CharacterConfig,
)

data class PartCategory(
    val id: String,
    val name: String,
    val iconUrl: String,
    val zIndex: Int,              // thứ tự vẽ layer
    val isRequired: Boolean,      // vẫn cho phép "None" nếu false; nếu true None = về default
    val items: List<PartItem>,
)

data class PartItem(
    val id: String,
    val thumbnailUrl: String,
    val colors: List<PartColor>,  // rỗng = không đổi màu
)

data class PartColor(val id: String, val hex: String, val layerUrl: String)

data class PartSelection(val itemId: String?, val colorId: String?)   // itemId null = None

data class CharacterTransform(
    val offsetX: Float = 0f, val offsetY: Float = 0f,
    val scale: Float = 1f, val rotation: Float = 0f, val flipped: Boolean = false,
)

data class CharacterConfig(
    val templateId: String,
    val selections: Map<String, PartSelection>, // key = categoryId
    val transform: CharacterTransform = CharacterTransform(),
)

enum class DecorType { BACKGROUND, STICKER, SPEECH }
data class DecorPack(val id: String, val name: String, val type: DecorType, val items: List<DecorItem>)
data class DecorItem(val id: String, val thumbnailUrl: String, val imageUrl: String)

data class CosplayChallenge(val id: String, val referenceImageUrl: String, val templateId: String, val answer: CharacterConfig)
data class LuckyRollResult(val imageUrl: String, val config: CharacterConfig)

enum class CreationType { CHARACTER, DESIGN }
data class Creation(val id: Long, val type: CreationType, val imagePath: String, val configJson: String?, val createdAt: Long)
```

## Resource

```kotlin
sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val throwable: Throwable) : Resource<Nothing>
}
```

## Remote – CHƯA CÓ API

1. `ApiService` (Retrofit) – **chỉ khai báo dạng comment**, không bịa endpoint:
   ```kotlin
   interface ApiService {
       // TODO(API): khai báo endpoint khi có tài liệu API
       // @GET("...") suspend fun getTemplates(): List<TemplateDto>
   }
   ```
2. `MakerRemoteDataSource` interface với các hàm `suspend`:
   `getTemplates()`, `getTemplateDetail(templateId)`, `getDecorPacks(type)`, `getRandomCosplayChallenge()`, `getLuckyRoll()`, `randomizeCategory(templateId, categoryId)`, `randomizeCharacter(templateId)`.
3. `PendingRemoteDataSource : MakerRemoteDataSource` – **implementation hiện tại**: mọi hàm `awaitCancellation()` (treo vô hạn, giả lập chờ API) kèm comment `// TODO(API): gọi apiService.xxx()`. Riêng `randomizeCategory` / `randomizeCharacter` trả `null` ngay lập tức và log `TODO(API)` (UI không làm gì).
4. `MockDataSource : MakerRemoteDataSource` – chỉ dùng khi `AppConfig.USE_MOCK_DATA = true`: `delay(1500)` rồi trả dữ liệu giả (6 template, 6 category × 20 item, 8 màu, 3 pack mỗi loại × 20 item). URL dùng chuỗi `"mock://color/#RRGGBB"` để UI vẽ ô màu thay ảnh (viết `MockImageLoader` helper: nếu url bắt đầu `mock://color/` thì set `ColorDrawable`, ngược lại dùng Glide). Random trong mock vẫn **không** làm (giữ `TODO(API)`).
5. `NetworkModule` cung cấp `OkHttpClient`, `Retrofit` (BASE_URL từ BuildConfig), `ApiService` – đã sẵn sàng nhưng chưa ai gọi.
6. `DataSourceModule`: `@Provides MakerRemoteDataSource` = `if (AppConfig.USE_MOCK_DATA) MockDataSource else PendingRemoteDataSource`.

## Repository (interface + impl, bind bằng `@Binds` trong `RepositoryModule`)

| Interface | Hàm | Impl hiện tại |
|---|---|---|
| `MakerRepository` | `templates(): Flow<Resource<List<CharacterTemplate>>>`, `templateDetail(id): Flow<Resource<TemplateDetail>>`, `suspend randomizeCategory(...)`: `PartSelection?`, `suspend randomizeCharacter(...)`: `CharacterConfig?` | emit `Loading` rồi gọi data source; bắt exception → `Error` |
| `DecorRepository` | `packs(type): Flow<Resource<List<DecorPack>>>` | như trên |
| `CosplayRepository` | `randomChallenge(): Flow<Resource<CosplayChallenge>>` | như trên |
| `LuckyRollRepository` | `roll(): Flow<Resource<LuckyRollResult>>` | như trên |
| `CreationRepository` | `observe(type)`, `get(id)`, `save(type, bitmap, config?)`, `delete(ids)`, `download(ids): Result<List<File>>` | **Thật**: Room + file nội bộ (internal storage) |
| `SettingsRepository` | `languageCode: Flow<String?>`, `setLanguage`, `onboardingDone: Flow<Boolean>`, `setOnboardingDone`, `rated`, `setRated` | **Thật**: DataStore |

## Local

- Room: `AppDatabase` (v1), `CreationEntity`, `CreationDao` (`observeByType`, `getById`, `insert`, `deleteByIds`). `DatabaseModule` cung cấp.
- `ImageFileStore` (chỉ dùng **internal storage**, không MediaStore/external):
  - `saveCreation(bitmap)`: lưu PNG vào `filesDir/creations/{uuid}.png`.
  - `delete(path)`: xoá file.
  - `copyToDownloads(srcPath)`: copy file sang `filesDir/downloads/DnD_{yyyyMMdd_HHmmss}_{n}.png` (tạo thư mục nếu chưa có), chạy trên `Dispatchers.IO`, trả về `File`.
  - Không cần runtime permission nào cho việc lưu.
- `SettingsDataStore` (Preferences DataStore).

## Utility thuần (có unit test)

- `CosplayScorer.score(answer: CharacterConfig, user: CharacterConfig): Int` – % = số category khớp (itemId và colorId nếu answer có colorId) / tổng category của answer × 100. Viết test JUnit cho: khớp hết = 100, không khớp = 0, khớp một nửa = 50, None vs None = khớp.
- `StarRating.fromPercent(p)` theo giả định ở `01_ANALYSIS.md`.

## Tiêu chí hoàn thành

- Build pass, unit test `CosplayScorer` pass.
- Mọi chỗ chờ API có comment `TODO(API)` – chạy `grep -rn "TODO(API)" app/src` liệt kê được.
- Không ViewModel/Fragment nào tạo repository thủ công.
