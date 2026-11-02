# Đổi giao diện

Đọc file này trước khi sửa `res/` hoặc `core/widget/`.

## Quy tắc giữ cho code không vỡ

- **Giữ view id và kiểu view** mà Fragment đang dùng qua ViewBinding. Được đổi vị trí, kích thước, ảnh nền, thêm view trang trí. Xoá hoặc đổi id thì build báo lỗi ngay; đổi kiểu view (vd. `ImageButton` sang `TextView`) có thể lỗi ở nơi khác.
- **Giữ tên resource khi chỉ thay nội dung**. Ghi đè đúng tên file ảnh và tên màu thì không phải sửa layout hay Kotlin. Các tên như `brown_primary`, `bg_tavern` hơi lệch nghĩa với chủ đề mới nhưng không ảnh hưởng người dùng; chỉ đổi tên khi người dùng muốn.
- **Không hardcode** màu, kích thước, chữ trong layout. Dùng `@color`, `@dimen`, `@string`.
- **1px Figma = 1dp**, khung thiết kế 360 × 800. Toạ độ dọc tính từ dưới status bar: `y_Figma - 44` nếu Figma có vẽ status bar cao 44.
- **Giữ các ô chức năng**: ô None luôn đứng đầu danh sách phụ kiện, ô xúc xắc ở customize, skeleton khi tải, `ErrorStateView` khi lỗi.
- Có override cho tablet ở `values-sw600dp/`. Đổi `dimens.xml` thì xem file tương ứng ở đó.

## Thứ tự làm

### 1. Token

Đổi giá trị, giữ tên. Lấy màu từ Figma theo vai trò:

| Token | Vai trò |
|---|---|
| `brown_primary` | Viền ngoài nút chính, tab đang chọn, tiêu đề dialog |
| `orange_button`, `button_highlight` | Lõi nút chính và viền sáng phía trên |
| `brown_dark` | Bóng nút, viền item, chữ trên nền sáng |
| `panel_brown`, `color_bar_bg` | Panel dưới customize / editor, thanh chọn màu |
| `cell_tan`, `editor_bg`, `cream` | Ô item, nền editor, nền My creation |
| `language_bg`, `splash_bg` | Nền language / intro / permission, nền splash hệ thống (nên trùng tông ảnh splash) |
| `orange_selected` | Item ngôn ngữ đang chọn, chữ Next, indicator |
| `tab_off_*` | Nút tab con không chọn |
| `shimmer_*` | Skeleton trên nền tối / sáng |
| `star_on`, `star_off` | Rate us |
| `home_fade_*` | Gradient đáy thẻ Home |
| `lucky_frame_*` | Khung Lucky roll |

Việc đi kèm:

- `res/values/arrays.xml`: bảng màu nền và màu chữ của decor editor.
- `res/values/styles.xml`: cỡ chữ `Text.*`.
- `res/values/themes.xml`: `android:windowLightStatusBar` phải hợp với nền mới.
- `res/font/`: đổi font chính bằng cách thay nội dung `londrina_solid*.ttf` và `londrina_solid_family.xml`, hoặc đổi tên rồi sửa `styles.xml`, `themes.xml`, `AppToast.kt`, `DecorModels.kt`. Font chữ trong editor khai báo ở enum `DecorFonts`.
- Font mới phải có giấy phép cho phép dùng; đặt bản OFL vào `docs/licenses/`.

### 2. Component vẽ bằng code

Đây là các chỗ chỉ đổi màu thì chưa đủ, nên quyết định sớm:

| File | Ảnh hưởng |
|---|---|
| `core/widget/WoodButtonDrawable.kt` | Hình nút chính: viên thuốc có vết mẻ gỗ vẽ bằng `Path`. Chủ đề không phải gỗ thì **phải sửa** (bỏ vết mẻ, thay bằng hình của thiết kế mới) |
| `core/widget/WoodButton.kt` | Variant `orange` / `gray`, viền chữ, hiệu ứng nhấn. Giữ `app:variant`, `app:buttonWidth`, `android:text` |
| `core/widget/RoundIconButton.kt`, `bg_round_icon*` | Nút tròn gồm nền + glyph |
| `core/widget/PaperDialogContainer.kt`, `view_paper_dialog.xml`, `bg_dialog_paper` | Khung dialog chung. Giữ `startButton`, `endButton`, `title`, `message` |
| `feature/decor/editor/DecorCanvasView.kt` | Khung chọn và tay nắm sticker |
| `core/widget/ShimmerFrameLayout.kt`, `SkeletonView.kt` | Thường chỉ cần đổi màu. Các khuôn `skeletonMask` lấy theo alpha của ảnh khung, khung mới khác hình thì kiểm tra lại |

Nếu thiết kế mới dùng ảnh cho nút thay vì vẽ, đổi `WoodButtonDrawable` thành nạp ảnh nhưng giữ API của `WoodButton`, để các layout đang dùng không phải sửa.

### 3. Ảnh dùng chung

Ghi đè đúng tên file:

| Nhóm | File | Thư mục |
|---|---|---|
| Nền toàn app | `bg_tavern` | `drawable-nodpi` |
| Home | `img_home_*` | `drawable-nodpi` |
| Onboarding | `img_intro_1..3`, `img_permission` | `drawable-nodpi` |
| Khung | `bg_part_item*`, `bg_template_item`, `bg_creation_card`, `bg_dialog_paper`, `bg_rate_dialog`, `bg_instruction_paper` | `drawable-xxhdpi` |
| Icon liền nền | `ic_back`, `ic_settings`, `ic_download`, `ic_archive`, `ic_info`, `ic_close`, `ic_arrow_*`, `ic_rotate_*`, `ic_zoom_*` | `drawable-xxhdpi` |
| Trạng thái | `img_empty_character`, `img_rate_0..5`, `img_star_*`, `img_dice_gift`, `img_add_image` | `drawable-nodpi` |
| Icon vector | `ic_*.xml`, `img_lucky_arrow.xml` | `drawable` |
| Logo, launcher | `logo.png`, `ic_launcher_*`, `mipmap-*` | Manifest dùng `@drawable/logo` làm icon |

Thường giữ nguyên: `ic_flag_*`, `img_color_wheel`.

Quy tắc xuất từ Figma:

| Loại | Cách xử lý | Nơi đặt |
|---|---|---|
| SVG đơn giản | Làm phẳng `<use>` / `<mask>` thành VectorDrawable | `drawable/*.xml` |
| SVG có filter (bóng trong, vân) | Render @3x (vd. resvg) thành WebP lossless, vì VectorDrawable không hỗ trợ filter | `drawable-xxhdpi/*.webp` |
| Ảnh raster | WebP q88, giới hạn cạnh dài | `drawable-nodpi/*.webp` |

Lưu ý:

- Không để URL ảnh Figma trong code: URL đó là tạm thời, app sẽ mất ảnh sau một thời gian.
- Figma có thể xuất PNG bị nền xám đục. Nếu khung hoặc icon cần trong suốt thì render lại từ SVG và kiểm tra kênh alpha.
- Nút nào trong Figma là ảnh liền khối thì dùng `ImageButton` với style `ImageIconButton`. Nút gồm nền + glyph thì dùng `RoundIconButton`.

### 4. Từng màn hình

Danh sách màn và layout nằm trong bảng ở `docs/APP_BRIEF.md`. Với mỗi màn:

1. Lấy `get_screenshot` và `get_design_context` của đúng node.
2. Sửa layout, giữ view id. Thêm giá trị vào `dimens.xml` khi cần.
3. Build. Nếu cần sửa Kotlin thì chỉ sửa phần hiển thị trong Fragment / adapter, không sửa ViewModel.
4. Chạy trên máy, đối chiếu với ảnh Figma, cập nhật trạng thái trong brief.

Thứ tự gợi ý: Home → Customize → Decor editor → Dialog → các màn còn lại. Customize và Decor editor khó nhất, làm sớm để lộ vấn đề về component.

Figma thiếu trạng thái nào (loading, lỗi, rỗng, toast) thì dựng từ token và component sẵn có, không xoá trạng thái đó.

## Đi hết luồng khi nghiệm thu

Kiểm tra cả lúc đang tải (shimmer) và khi có dữ liệu:

1. Lần đầu mở: Splash → Language → Intro 1–3 → Permission → Home.
2. Mở lại lần sau: Splash → Intro 1–3 → Home.
3. Maker: chọn template → đổi bộ phận, màu → Random → Move → Reset → Next.
4. Decor editor: nền màu / ảnh / pack, sticker, speech, text → Download → Successful (share, về Home).
5. My creation: hai tab, trạng thái rỗng, chọn nhiều, xoá, chi tiết.
6. Cosplay: Random → Instruction → chơi → điểm số → Successful.
7. Lucky roll: Random → Edit.
8. Setting: đổi ngôn ngữ, Rate us, Share, Privacy.
9. Tắt mạng rồi mở màn có dữ liệu: `ErrorStateView` hiện ra, bật mạng và bấm Retry thì tải lại được.
10. Chuyển sang ngôn ngữ có chữ dài nhất (hi, fr) để xem chữ có tràn nút không.
