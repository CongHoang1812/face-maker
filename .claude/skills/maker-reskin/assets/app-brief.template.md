# App brief: {{app_name}}

Nguồn sự thật của app này. Skill `maker-reskin` đọc file này trước khi làm bất cứ việc gì.

## Định danh (script đã điền)

| Mục | Giá trị |
|---|---|
| Tên app | {{app_name}} |
| Mã dự án | {{project_code}} |
| Package / applicationId | `{{app_id}}` |
| Tiền tố class / theme | `{{class_prefix}}` |
| APP_KEY trên server | `{{app_key}}` |
| Máy chủ chính | {{base_url}} |
| Máy chủ dự phòng | {{backup_url}} |

## Bạn cần điền

### Thiết kế

- Figma URL:
- Page chứa màn hình:
- Chủ đề app (vd. anime, mèo, công chúa):
- Danh từ thay cho "D&D" trong chữ hiển thị:

### API

- Định dạng: `giống app mẫu` / `khác` (chạy `probe_api.py` nếu chưa chắc)
- Tài liệu API (đường dẫn file hoặc link):
- Có cần token / header không:

### Phát hành

- URL chính sách bảo mật:
- Email góp ý:
- Ngôn ngữ (mặc định en, hi, es, fr, pt, in):

### Khác biệt so với app mẫu

Ghi rõ mọi thay đổi ngoài giao diện và API. Để trống nghĩa là tính năng giữ nguyên.

-

## Bảng màn hình ↔ Figma

Điền node id khi bắt đầu giai đoạn giao diện.

| Màn | Layout | Figma node | Trạng thái |
|---|---|---|---|
| Splash | `fragment_splash` | | |
| Language | `fragment_language` | | |
| Intro | `fragment_intro`, `item_intro_page` | | |
| Permission | `fragment_permission` | | |
| Home | `fragment_home` | | |
| Setting | `fragment_setting` | | |
| Chọn template | `fragment_template_select`, `item_template` | | |
| Customize + Move | `fragment_customize`, `view_move_panel`, `item_part`, `item_category_tab` | | |
| Decor editor | `fragment_decor_editor`, `item_decor_cell`, `item_font_chip` | | |
| Successful | `fragment_success` | | |
| My creation + Detail | `fragment_my_creation`, `fragment_creation_detail`, `item_creation` | | |
| Cosplay | `fragment_cosplay_start`, `dialog_cosplay_score`, `dialog_instruction` | | |
| Lucky roll | `fragment_lucky_roll`, `item_dice_part` | | |
| Dialog | `view_paper_dialog`, `dialog_confirm`, `dialog_color_picker`, `dialog_rate_us`, `dialog_loading` | | |
| Trạng thái lỗi | `view_error_state` | | |

## Quyết định đã chốt

Ghi lại quyết định không suy ra được từ code, để phiên làm việc sau không phải hỏi lại.

-
