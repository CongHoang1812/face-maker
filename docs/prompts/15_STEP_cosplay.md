# BƯỚC 13 – D&D Cosplay (game mode)

> Prompt cho agent. Figma section `3:40`: start `3:41`, instruction `3:77`, play `3:132`, score popup (khung giấy "50%"), successful `3:223`.

## Luật chơi
Người chơi nhận một ảnh tham chiếu ngẫu nhiên, phải dựng lại nhân vật bằng cách chọn đúng kiểu tóc, trang phục, màu, phụ kiện. Mỗi lựa chọn đúng tăng % điểm.

## CosplayStartFragment (`3:41`)
- Nền `bg_tavern`. Top: back trái, **(i)** phải.
- Khung ảnh tham chiếu 252×252 nền trắng bo 4 (y≈90).
- 2 WoodButton dưới ảnh: **Random** và **Cosplay**.
- Dữ liệu: `CosplayViewModel.challenge = cosplayRepository.randomChallenge()` → hiện tại **Loading mãi** ⇒ khung ảnh là skeleton shimmer, nút Cosplay disable.
- **Random**: gọi lại `randomChallenge()` – thân hàm repository/data source là `TODO(API)`; UI chỉ cần reset về shimmer.
- **(i)** → Instruction overlay.

## Instruction overlay (`3:77`)
- `DialogFragment` full-screen dim `overlay_dim`; thẻ giấy rách 240dp rộng: tiêu đề "Instruction" 20sp `brown_primary`; đoạn luật chơi (chuỗi ở trên) 12sp căn giữa; nút tròn X bên dưới thẻ.
- Tự hiện lần đầu vào Cosplay (lưu cờ trong DataStore).

## CosplayPlayFragment (`3:132`)
- Tái sử dụng layout/logic Customize (bước 9) với `mode = COSPLAY`:
  - Bỏ cột tool trái và nút random-all; **ô xúc xắc trong lưới vẫn giữ** (random part → hàm rỗng `TODO(API)`); ô **None** đứng đầu.
  - Top bar: back + **thanh thời gian** (pill nền `cell_tan` viền `brown_primary`: nhãn "10:00" nền nâu + progress bar nâu giảm dần) + nút **Done** (WoodButton nhỏ bên phải – màn thiếu trong Figma).
  - Thumbnail ảnh tham chiếu 64×64 viền trắng ở góc phải dưới canvas.
- Template = `challenge.templateId`; config khởi đầu = default.
- Đếm ngược 10 phút bằng coroutine trong ViewModel (giữ khi xoay màn). Hết giờ hoặc Done → `CosplayScorer.score(challenge.answer, userConfig)` → score popup.
- Back → Exit dialog.

## Score popup
- Thẻ giấy rách 240dp: ảnh nhân vật người chơi, "{percent}%" 20sp `brown_primary`; nút tròn X bên dưới.
- Đóng → lưu ảnh vào My creation (CHARACTER, kèm config) → `successFragment(ARG_SOURCE=COSPLAY, ARG_STARS=StarRating.fromPercent(p))`.

## Tiêu chí hoàn thành
- Không mock: start hiển thị shimmer, (i) và Random hoạt động, Cosplay disable.
- Mock: chơi được, timer chạy, Done ra %, successful hiện số sao đúng.
