# Biểu tượng giao diện (Phosphor)

## Nguồn

- Bộ: **Phosphor Icons**, kiểu **regular**, phiên bản **2.1.1** (`@phosphor-icons/core@2.1.1`).
- Lấy SVG từ CDN: `https://unpkg.com/@phosphor-icons/core@2.1.1/assets/regular/<tên>.svg`.
- Giấy phép: MIT (Copyright (c) 2020 Phosphor Icons). Bắt buộc giữ ghi chú này khi dùng lại.

## Danh sách đang dùng

| Tên Phosphor | Dùng ở đâu | File native | Cách nhúng JS |
|---|---|---|---|
| `picture-in-picture` | Nút PiP trong app | `res/drawable/ic_ph_picture_in_picture.xml` | — |
| `queue` | Nút hàng đợi góc dưới trái (có huy hiệu số) | `res/drawable/ic_ph_queue.xml` | — |
| `list` | Nút ☰ mở bảng hiển thị nhanh | `res/drawable/ic_ph_list.xml` | — |
| `skip-forward` | Phát tiếp theo (bảng hàng đợi, trang xem video) | `res/drawable/ic_ph_skip_forward.xml` | `PhosphorIcons.path` (dựng DOM) |
| `trash` | Xóa khỏi hàng đợi | `res/drawable/ic_ph_trash.xml` | — |
| `dots-six-vertical` | Tay cầm kéo đổi thứ tự | `res/drawable/ic_ph_dots_six_vertical.xml` | — |
| `play` | Tiếp tục xem dở | `res/drawable/ic_ph_play.xml` | — |
| `x` | Bỏ qua lời nhắc xem dở | `res/drawable/ic_ph_x.xml` | — |
| `list-plus` | Thêm vào hàng đợi (trang xem video, cover) | `res/drawable/ic_ph_list_plus.xml` (dự phòng) | `PhosphorIcons.path` (dựng DOM) |
| `pause` | Nút PiP khi đang phát | `res/drawable/ic_ph_pause.xml` | — |
| `video` | Bảng hiển thị nhanh: công tắc Shorts | `res/drawable/ic_ph_video.xml` | — |
| `chat-circle` | Bảng hiển thị nhanh: công tắc Bình luận | `res/drawable/ic_ph_chat_circle.xml` | — |
| `squares-four` | Bảng hiển thị nhanh: công tắc Video đề xuất | `res/drawable/ic_ph_squares_four.xml` | — |

Trang YouTube bật Trusted Types nên KHÔNG gán `innerHTML`. JS dựng `<svg>`/`<path>` bằng `createElementNS` từ path data (test `QueueWebScriptTest` chặn `innerHTML`).

## Quy ước đồng bộ

- Native: `res/drawable/ic_ph_<tên>.xml` (VectorDrawable, viewport 256, màu `#FF0F0F0F`).
- JavaScript: `app/src/main/java/com/meomuc/youtubeintablet/PhosphorIcons.kt` chứa cùng path data.
- Path phải giống nhau giữa hai bên. Khi cập nhật Phosphor, tạo lại cả hai từ cùng file SVG.
- Test `PhosphorIconsTest` kiểm tra path không rỗng và SVG nội tuyến không chứa dấu nháy kép.

## Màu và kích thước

- Native (giao diện Material 3, xem `DESIGN.md`): icon lấy màu từ theme (`colorOnSurfaceVariant`, `colorOnPrimary`). Các màu `#FF0F0F0F` trong file drawable chỉ là màu gốc, bị ghi đè khi tint.
- JavaScript trên trang YouTube (không thuộc giao diện native): nền nút `#F2F2F2`, icon `#0F0F0F`. Đây là giá trị tạm, không đổi theo theme.
- Cỡ chạm tối thiểu: 48dp cho nút và tay cầm trong bảng hàng đợi, 56dp cho nút nổi; trên trang YouTube: 48px (thanh xem video), 36px (cover).
