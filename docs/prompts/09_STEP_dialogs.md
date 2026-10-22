# BƯỚC 7 – Dialog dùng chung & Rate us

> Prompt cho agent. Figma: section `69:137` (Exit `69:34`, Reset `69:59`, Random `69:73`, Delete `69:87`, Choose color `69:101`), section `69:339` (Rate us).

## ConfirmDialog (`feature/dialog/ConfirmDialogFragment : DialogFragment`)
- Một class cho Exit / Reset / Delete / Random. Tham số: `title`, `message`, `positiveText`, `negativeText`, `requestKey`.
- Layout: `PaperDialogContainer` rộng 312dp, nền giấy rách `cell_tan`, tiêu đề 24sp `brown_primary` căn giữa, message 14sp `brown_primary` căn giữa, 2 `WoodButton` 142×40 cạnh nhau.
- Trả kết quả qua `setFragmentResult(requestKey, bundleOf("confirmed" to Boolean))`.
- Factory: `ConfirmDialogs.exit()`, `.reset()`, `.delete()`, `.random()` với chuỗi:
  - Exit: "Haven't saved it yet, do you want to exit?" – Yes/No
  - Reset: "Do you want to reset all ?" – Yes/No
  - Delete: "Are you sure want to delete this item?" – Yes/No
  - Random: "Watch a short ad to generate a random character." – Cancel/Watch Ad
- **Watch Ad**: chưa có Ads SDK → trả `confirmed=true` ngay, comment `// TODO(ADS): show rewarded ad trước khi trả kết quả`.

## ColorPickerDialog
- Tiêu đề "Choose color"; ô saturation/value 230×94 bo 8 (custom View `SaturationValueView`: gradient trắng→hue ngang, trong suốt→đen dọc, con trỏ tròn trắng viền); thanh hue 230×18 bo 9 (`HueBarView`, cầu vồng, con trỏ tròn). Cancel/Done.
- Input `initialColor`, output `color` (Int) qua Fragment Result.
- Logic hoàn toàn local – làm đầy đủ.

## RateUsDialog
- Thẻ giấy rách; emoji 64dp; tiêu đề 18sp `brown_primary`; mô tả 14sp nhạt; 5 sao (`ic_star`, tint `star_on`/`star_off`); nút "Rate" (nền `brown_primary` bo 20, chữ trắng, rộng 232); text "Exit".
- Trạng thái theo số sao:
  - 0: 😊 "Do you like the app?" / "Let us know your experience"
  - 1: 😭 "Oh, no!" / "Please give us some feedback"
  - 2: 😢 "Oh, no!" / ...
  - 3: ☹️ "Oh, no!" / ...
  - 4: ☺️ "We love you too!" / "Thanks for your feedback"
  - 5: 😍 "We love you too!" / ...
  (emoji dùng ảnh export từ Figma hoặc emoji unicode trong TextView)
- Rate: 0 sao → disable; 1–3 sao → mở email feedback (`ACTION_SENDTO mailto:` – địa chỉ placeholder `AppConfig.FEEDBACK_EMAIL`), 4–5 sao → Play In-App Review hoặc mở Play Store; sau đó `setRated(true)`.

## Tiêu chí hoàn thành
- Tất cả dialog hiển thị đúng Figma, sống sót khi xoay/recreate, kết quả trả qua Fragment Result API.
