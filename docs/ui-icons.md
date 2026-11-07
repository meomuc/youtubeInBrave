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

Trang YouTube bật Trusted Types nên KHÔNG gán `innerHTML`. JS dựng `<svg>`/`<path>` bằng `createElementNS` từ path data (test `QueueWebScriptTest` chặn `innerHTML`).

## Quy ước đồng bộ

- Native: `res/drawable/ic_ph_<tên>.xml` (VectorDrawable, viewport 256, màu `#FF0F0F0F`).
- JavaScript: `app/src/main/java/com/meomuc/youtubeintablet/PhosphorIcons.kt` chứa cùng path data.
- Path phải giống nhau giữa hai bên. Khi cập nhật Phosphor, tạo lại cả hai từ cùng file SVG.
- Test `PhosphorIconsTest` kiểm tra path không rỗng và SVG nội tuyến không chứa dấu nháy kép.

## Màu và kích thước (tạm thời)

- Nền nút: `#F2F2F2`; icon: `#0F0F0F`; trạng thái bật (nếu cần): `#CC0000`.
- Cỡ chạm tối thiểu: 56dp (nút native); nút trên trang YouTube: 48px (thanh xem video), 36px (cover).
- Chưa có `design-system/MASTER.md` trong repo (AGENTS.md yêu cầu). Các giá trị trên là màu tạm, cần đối chiếu khi có token chính thức.
