# BƯỚC 6 – Home & Setting

> Prompt cho agent. Figma nodes: home `3:438`, setting `3:468`, language `3:505`.

## Home (`HomeFragment`)
- Nền `bg_tavern` phủ `overlay_20`. Nút setting tròn (`ic_settings`) góc phải, y=60.
- Logo `img_logo` căn giữa dưới top bar.
- Lưới 2×2 thẻ (mỗi thẻ ~112×112dp, bo 12, viền trắng 2dp, ảnh full + nhãn chữ trắng viền nâu ở đáy):
  1. **D&D maker** → `templateSelectFragment`
  2. **D&D cosplay** → `cosplayStartFragment` – **NEXT VER**: bản 1.0 ẩn thẻ này (plan.xlsx: Button Cosplay = 0)
  3. **Character Random** (Figma ghi "D&D lucky roll") → `luckyRollFragment`
  4. **My creation** → `myCreationFragment`
- Ảnh thẻ là asset local (không qua API, không shimmer).
- Back ở Home → thoát app (có thể hiện Rate us dialog lần đầu thoát nếu chưa rate – logic ở bước 7).

## Setting (`SettingFragment`)
- Nền `bg_tavern`, nút back. 4 hàng (style giống item language, nền trắng, icon trái 16dp, chữ nâu):
  - **Language** → `languageFragment(fromSetting=true)`
  - **Rate us** → mở `RateUsDialog` (bước 7)
  - **Share app** → `Intent.ACTION_SEND` text chứa link Play Store `https://play.google.com/store/apps/details?id=${packageName}`
  - **Privacy & Policy** → mở Custom Tab URL hằng `AppConfig.PRIVACY_URL` (placeholder, `// TODO: thay URL thật`)

## Tiêu chí hoàn thành
- Điều hướng đủ 4 thẻ và 4 hàng setting (các màn đích có thể đang là placeholder).
- Click nhanh 2 lần không mở 2 màn (dùng single-click).
