# BƯỚC 5 – Splash, Language (lần đầu), Intro 1-3, Permission

> Prompt cho agent. Figma nodes: splash `3:572`, language `3:605`, intro `3:668` `3:692` `3:717`, permission `3:741`.

## Splash (`SplashFragment`)
- Dùng `core-splashscreen` cho icon hệ thống, sau đó hiển thị fragment splash: nền `bg_tavern`, logo `img_logo` căn giữa (y≈180), text "Loading..." + progress bar ngang (track trắng viền nâu, progress là các ô vuông cam – custom drawable `progress_splash.xml` dạng layer-list/clip), text nhỏ "This action can contain ads" ở đáy.
- Progress chạy 0→100 trong ~2.5s (ValueAnimator). Không chờ API.
- Kết thúc: đọc `SettingsRepository.onboardingDone` → `true` → `introFragment` (arg `onboardingDone=true`); `false` → `languageFragment` (arg `fromSetting=false`). `popUpTo splash inclusive`. **Intro hiện mỗi lần mở app**, Language + Permission chỉ ở lần đầu.

## Language (`LanguageFragment`, dùng chung cho cả 2 ngữ cảnh)
- Arg `fromSetting: Boolean`.
  - `false` (lần đầu – `3:605`): nền `language_bg`, tiêu đề "Language" 32sp góc trái, nút ✓ góc phải.
  - `true` (`3:505`): nền `bg_tavern` + nút back tròn trái, ✓ phải, không tiêu đề.
- RecyclerView 6 item (Hindi `hi`, Spanish `es`, French `fr`, English `en`, Portuguese `pt`, Indonesian `in`) theo thứ tự Figma: Hindi, Spanish, French, English, Portuguese, Indonesian. Item: cờ tròn 20dp + tên + radio 16dp; item chọn nền `orange_selected`, chữ trắng, radio filled.
- Mặc định chọn ngôn ngữ hiện tại của máy nếu nằm trong danh sách, ngược lại English.
- Nhấn ✓: lưu `SettingsRepository.setLanguage(code)` + `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))`; lần đầu → `introFragment`; từ setting → popBackStack.
- `LanguageViewModel` (@HiltViewModel) giữ `selectedCode` trong `StateFlow`, sống sót xoay/recreate do đổi locale.

## Intro (`IntroFragment`)
- `ViewPager2` 3 trang, layout trang: ảnh trên chiếm 560dp (fade dần xuống nền `language_bg` bằng gradient overlay), text 2 dòng căn giữa trắng (y≈576), hàng dưới: indicator 3 chấm (active là thanh dài 16×8 màu `orange_selected`, inactive chấm trắng 8×8) bên trái, "Next" màu `orange_selected` bên phải.
- Trang 1 ảnh `img_intro_1` "Create your own avatar however you like"; trang 2 `img_intro_2` "Unlock your stylish & classy look"; trang 3 `img_intro_3` "Express yourself through style".
- Arg `onboardingDone: Boolean` (mặc định `false`). Next ở trang cuối: `false` (lần đầu) → `permissionFragment`; `true` → `homeFragment`, `popUpTo nav_main inclusive`.

## Permission (`PermissionFragment`)
- Nền `language_bg`, tiêu đề "Permission", ảnh `img_permission`, 2 hàng giống item language: "Media 1" (quyền ảnh), "Media 2" (quyền thông báo) – radio hiển thị trạng thái đã cấp. Chạm hàng → `ActivityResultContracts.RequestPermission` với permission tương ứng theo API level (xem `01_ANALYSIS.md` mục 5; API < 33 thì Media 2 coi như đã cấp).
- "Continue" (góc phải dưới, màu cam): `setOnboardingDone(true)` → `homeFragment`, `popUpTo nav_main inclusive`.

## Tiêu chí hoàn thành
- Cài mới: Splash → Language → Intro x3 → Permission → Home. Mở lại app: Splash → Intro x3 → Home.
- Đổi ngôn ngữ → toàn bộ chuỗi đổi ngay, không crash, không quay lại màn trước sai.
