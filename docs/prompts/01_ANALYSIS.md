# 01 – Phân tích thiết kế Figma

> Tài liệu tham chiếu, không phải prompt code. Các bước sau đều dựa vào đây.

## 1. Danh sách màn hình (page `ui`)

| Section | Frame (node id) | Mô tả |
|---|---|---|
| **intro** | `3:572` splash/loading | Nền quán rượu D&D, logo "D&D CHARACTER MAKER", thanh loading cam, text "This action can contain ads" |
| | `3:605` language (lần đầu) | Nền `#B28A71`, tiêu đề "Language", nút ✓ góc phải, 6 ngôn ngữ: Hindi, Spanish, French, English, Portuguese, Indonesian. Item chọn: nền cam `#FF8D1A`, chữ trắng |
| | `3:668` intro 1 | Ảnh lưới phụ kiện, "Create your own avatar however you like", indicator 3 chấm, "Next" |
| | `3:692` intro 2 | Ảnh nhân vật + thanh màu + lưới trang phục, "Unlock your stylish & classy look" |
| | `3:717` intro 3 | Ảnh chiến binh, "Express yourself through style" |
| | `3:741` permission | "Permission", ảnh nhân vật, 2 toggle "Media 1", "Media 2", nút "Continue" |
| **home** | `3:438` home | Logo, nút setting (bánh răng), 4 thẻ: **D&D maker**, **D&D cosplay**, **D&D lucky roll**, **My creation** |
| | `3:468` setting | Back, các dòng: Language, Rate us, Share app, Privacy & Policy |
| | `3:505` language (từ setting) | Giống language nhưng nền quán rượu + nút back |
| **customize** | `3:278` cata | Back + lưới 2 cột × 3 hàng khung giấy cũ – chọn nhân vật gốc (template) |
| | `3:335` customize | Nhân vật lớn 328dp, trái: reset / flip / move; phải trên: "Next"; góc phải dưới ảnh: nút xúc xắc random-all; panel nâu: thanh màu, lưới item 5 cột (ô 1 = none, ô 2 = xúc xắc random part), dưới cùng: tab category dạng "tab giấy" cuộn ngang |
| | `8:24` move | Panel nâu: "Exit", xoay trái/phải, 4 mũi tên di chuyển, zoom +/− |
| **background** | `3:773` add color | Nền `#FFD9B2`, top: back + download. Panel: 4 icon tab (Background, Sticker, Speech, Text), tab con (Color / Image / Pack…), lưới ô trắng 56dp (ô 1 none, ô 2 bảng màu tùy chọn) |
| | `3:843` add image | Tab Image: ô 1 none, ô 2 chọn ảnh từ thư viện |
| | `3:913` sticker | Canvas trắng, sticker đang chọn có khung nét đứt + 3 handle (flip, delete, resize/rotate). Tab con Pack 1/2/3… |
| | `3:993` speech | Bong bóng "Hello". Ngoài pack còn có hàng **font** và hàng **màu chữ** |
| | `3:1155` text | Ô nhập "Hello" + nút ✓, hàng font, hàng màu (ô đầu là bánh xe màu tùy chọn) |
| | `3:1109` successful | Top: re-edit (xoay), share, my creation (hộp), home. Ảnh kết quả, nút "Download" |
| **cosplay** | `3:41` cosplay | Ảnh tham chiếu trắng, nút "Random", "Cosplay", nút (i) |
| | `3:77` inf | Overlay tối + thẻ "Instruction" (nội dung luật chơi) + nút X |
| | `3:132` cosplay play | Back + đồng hồ "10:00" + thanh thời gian; nhân vật; thumbnail ảnh tham chiếu góc phải; panel customize như màn customize |
| | (popup) | Thẻ giấy: ảnh + "50%" + nút X |
| | `3:223` successful | Như successful nhưng có huy hiệu 3 sao trên ảnh |
| **lucky roll** | `3:11` default | Khung xám "Click the random button below to see what your character is!" + mũi tên, nút "Random" |
| | `3:24` play | Ảnh nhân vật, nút "Random", nút "Edit" góc phải |
| **creation** | `3:1229` / `3:1243` empty | Tab "Character" / "Design", ảnh + "No character here" |
| | `3:1257` / `3:1359` list | Lưới 2 cột khung giấy. Character: nút xoá + sửa. Design: chỉ nút xoá |
| | `3:1437` / `3:1515` select | Chế độ chọn nhiều: checkbox, top bar xoá/share/download. Character: thêm nút "Add to Telegram", "Add to Whatsapp" |
| | `3:1583` / `3:1608` detail | Nền `#FFD9B2`, top bar back/xoá/share/download, ảnh lớn, nút "Edit" (chỉ Character) |
| **dialog** | `69:34` Exit | "Haven't saved it yet, do you want to exit?" Yes/No |
| | `69:59` Reset | "Do you want to reset all ?" Yes/No |
| | `69:73` Random | "Watch a short ad to generate a random cat." Cancel/Watch Ad |
| | `69:87` Delete | "Are you sure want to delete this item?" Yes/No |
| | `69:101` Choose color | Ô saturation/value + thanh hue, Cancel/Done |
| **RATE US** | `69:339` | 6 trạng thái theo số sao 0–5 (emoji + tiêu đề + mô tả), nút "Rate", "Exit" |

Trang `lfs` (`3:1645`) chỉ là ảnh store listing → **không code**.

## 2. Luồng điều hướng

> Các nhánh đánh dấu **(NEXT VER)** không làm ở bản 1.0 – xem phạm vi trong `00_README.md`.

```
Splash ──(lần đầu)──> Language ─> Intro1 ─> Intro2 ─> Intro3 ─> Permission ─> Home
   └──(đã onboard)──────────────> Intro1 ─> Intro2 ─> Intro3 ──────────────> Home

Home
 ├─ Setting ─> Language (setting) | Rate us (dialog) | Share app (intent) | Privacy (WebView/Browser)
 ├─ D&D maker ─> TemplateSelect(cata) ─> Customize ─(Next)─> DecorEditor ─(Download)─> Successful
 │                                         └─ Move mode (panel thay thế, cùng màn)
 ├─ D&D cosplay (NEXT VER) ─> CosplayStart ─(i)─> Instruction overlay
 │                     └─(Cosplay)─> CosplayPlay ─(Done/hết giờ)─> Score popup ─> CosplaySuccessful
 ├─ Character Random (lucky roll) ─> nhân vật random ngay khi vào ─(Edit)─> Customize (với config random)
 └─ My creation ─> [Character | Design] ─> Detail ─(Edit, NEXT VER)─> Customize
                                └─ long-press ─> Selection mode

Successful: re-edit (về editor), share, my creation, home (popUpTo Home)
```

## 3. Phân loại chức năng: cần API vs local

| Chức năng | Nguồn | Trạng thái bây giờ |
|---|---|---|
| Danh sách template nhân vật (cata) | API | Shimmer + `TODO(API)` |
| Category, part item, bảng màu của part | API | Shimmer + `TODO(API)` |
| Random 1 part (ô xúc xắc) / random cả nhân vật (nút xúc xắc) | API (logic từ server/dữ liệu) | Hàm rỗng `TODO(API)` |
| Pack background / sticker / speech bubble | API | Shimmer + `TODO(API)` |
| Cosplay: ảnh tham chiếu random + đáp án | API | Shimmer + `TODO(API)` |
| Cosplay: tính % giống | Local (so sánh config) – cần đáp án từ API | Viết hàm thuần, test unit; data rỗng |
| Lucky roll random nhân vật | API | Shimmer + `TODO(API)` |
| Watch Ad (dialog Random) | Ads SDK | Stub `TODO(ADS)` |
| Màu nền, màu tuỳ chọn, ảnh từ thư viện, text, font | Local | **Làm đầy đủ** |
| Di chuyển/xoay/zoom/flip nhân vật, sticker | Local | **Làm đầy đủ** |
| Lưu ảnh, My creation (Room + file), share, download vào **bộ nhớ trong của app** (internal storage, không ra Gallery) | Local | **Làm đầy đủ** |
| Ngôn ngữ, onboarding, rate, share app | Local | **Làm đầy đủ** |
| Add to Telegram / WhatsApp sticker | SDK ngoài | Stub `TODO(STICKER_EXPORT)` |

## 4. Màn hình/trạng thái bị thiếu trong Figma (tự thiết kế theo design system)

1. **Dialog loading/saving** – khi render & lưu ảnh (thẻ giấy + progress cam).
2. **Error / No internet state** – thay shimmer khi `Resource.Error`: khung giấy + text + nút "Retry".
3. **Toast/Snackbar "Saved"** sau khi Download (ảnh lưu vào internal storage của app).
4. **Privacy & Policy** – mở URL bằng Custom Tab/Browser (không cần màn riêng).
5. **Cosplay – nút hoàn thành** (Figma không có): thêm nút "Done" thay vị trí "Next" ở top bar màn play; hết giờ cũng tự nộp.
6. **Cosplay – Time's up**: dùng chung score popup.
7. **Exit dialog trong Cosplay**: dùng lại dialog Exit.
8. **Tab category trống** khi template chưa load: hiển thị shimmer tab.
9. **Empty state Design tab** dùng lại layout empty của Character ("No design here").
10. **Text editor**: Figma có ô nhập, cần thêm hành vi: nhấn ✓ → thêm text lên canvas; chạm text trên canvas → sửa lại.

## 5. Giả định (có thể sửa)

- Permission "Media 1" = quyền đọc ảnh (`READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` ≤ API 32) – **chỉ dùng để chọn ảnh nền từ thư viện**, "Media 2" = thông báo (`POST_NOTIFICATIONS`, API 33+). Màn permission có thể bỏ qua bằng "Continue".
- **Lưu trữ**: mọi ảnh (My creation và Download) chỉ lưu trong **internal storage** của app (`context.filesDir`). **Không** ghi ra external storage / MediaStore / Gallery, **không** cần quyền `WRITE_EXTERNAL_STORAGE`. Ảnh bị xoá khi gỡ app.
- Dialog Random nói "random cat" là lỗi copy → dùng chuỗi "Watch a short ad to generate a random character."
- Đồng hồ cosplay 10:00 phút đếm ngược.
- Số sao ở Cosplay successful: ≥90% = 3 sao, ≥60% = 2, ≥30% = 1, còn lại 0.
