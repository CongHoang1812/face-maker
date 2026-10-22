# BƯỚC 14 – D&D Lucky roll

> Prompt cho agent. Figma section `3:10`: default `3:11`, play `3:24`.

## UI (`LuckyRollFragment`)
- Nền `bg_tavern`. Top: back trái; **Edit** (WoodButton nhỏ) bên phải – chỉ hiện khi đã có kết quả.
- Khung 328×328 căn giữa y=116:
  - **Idle** (`3:11`): nền xám `#666666` bo 4, text trắng "Click the random button below to see what your character is!" + ảnh mũi tên xoắn (`ic_curly_arrow`, export từ Figma).
  - **Loading** (sau khi bấm Random, chờ API): skeleton shimmer cùng kích thước.
  - **Result** (`3:24`): ảnh nhân vật nền trắng bo 4.
  - **Error**: error view + Retry.
- WoodButton **Random** (~160×40) căn giữa dưới khung.

## Logic
- `LuckyRollViewModel` state: `Idle | Resource<LuckyRollResult>`.
- Random → `luckyRollRepository.roll()` – **hiện tại treo Loading** (data source `TODO(API)`), nên UI chuyển sang shimmer và ở đó. Không tự random bằng code local.
- Có thể yêu cầu xem quảng cáo trước khi random: dùng Random dialog (Watch Ad stub) – để cờ `AppConfig.LUCKY_ROLL_REQUIRES_AD = false`.
- Edit → `customizeFragment` với `ARG_CONFIG_JSON = result.config`.

## Tiêu chí hoàn thành
- Không mock: Idle → bấm Random → shimmer chạy liên tục, Edit ẩn.
- Mock: hiện ảnh kết quả, Edit mở customize đúng config.
