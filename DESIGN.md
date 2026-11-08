# DESIGN.md — Giao diện native của YouTube Tablet

Ghi lại hệ thiết kế đang áp dụng cho các thành phần native Android quanh WebView. Nội dung YouTube bên trong WebView không thuộc phạm vi này.

## Hướng thiết kế đã chốt

- **Material 3 chuẩn** (lựa chọn của chủ dự án). Thay thế hướng "lưới kỹ thuật" trong surface brief `.impeccable/surfaces/main-screen.md` trước đó. Surface brief giữ lại làm lịch sử, không còn là nguồn quyết định màu và hình.
- **Thiết bị đích:** màn hình xe 1280x800 (android box, Android 13) và Xiaomi Pad 7 Pro. Bố cục phải dùng được cả hai; thao tác chính bằng cảm ứng.
- **Sáng/tối:** theo hệ thống (`Theme.Material3.DayNight`). Cả hai chế độ đã được kiểm tra trên emulator.
- **Màu:** Dynamic Color trên Android 12 trở lên (lấy từ hình nền); máy cũ dùng bảng màu tĩnh mặc định của Material 3.

## Thành phần

| Thành phần | Kiểu Material 3 | Ghi chú |
|---|---|---|
| Bảng hiển thị nhanh (`QuickPanel`) | `MaterialCardView` (bo góc 28dp) + `MaterialSwitch` | Mỗi hàng có icon Phosphor, nhãn, mô tả. Cả hàng là vùng chạm 56dp. |
| Bảng hàng đợi (`QueuePanel`) | `MaterialCardView` (bo góc 28dp) + nút icon tonal | Nút phát tiếp theo và xóa 48dp; tay cầm kéo 48dp. |
| Nút nổi ☰, hàng đợi, PiP | `MaterialButton`, kiểu icon tonal | 56dp, icon Phosphor, có `contentDescription` và tooltip. |
| Banner tiếp tục xem | `MaterialCardView` (bo góc 16dp) + nút chữ | Nút có nhãn "Tiếp tục" và "Bỏ qua" để không phụ thuộc icon. |
| Lớp phủ khi mở bảng | Màu đen alpha 32% | Chạm ra ngoài để đóng. |

## Kiểu chữ, khoảng cách, hình khối

- Kiểu chữ theo vai trò Material: `TitleLarge` cho tiêu đề bảng, `BodyLarge` cho nhãn chính, `BodyMedium` cho mô tả và thông báo trống. Không đặt cỡ chữ cố định theo từng màn hình.
- Lưới 8dp: lề trong thẻ 24dp, khoảng cách giữa các hàng 8dp, lề ngoài bảng 24dp.
- Bo góc: thẻ lớn 28dp, banner 16dp.
- Vùng chạm tối thiểu 48dp (Material). Nút điều khiển chính dùng 56dp.

## Icon

- Bộ Phosphor, kiểu regular, phiên bản 2.1.1, giấy phép MIT. Danh sách và cách lấy nằm trong `docs/ui-icons.md`.
- Icon hệ thống (`android.R.drawable.*`) không còn dùng trong giao diện và thông báo.

## Đã kiểm chứng và chưa kiểm chứng

- Đã kiểm chứng trên emulator (1280x800): bảng hiển thị nhanh, bảng hàng đợi, banner tiếp tục xem, nút nổi; chế độ sáng và tối.
- Chưa kiểm chứng: trên Xiaomi Pad 7 Pro, cỡ chữ hệ thống lớn, Dynamic Color thực trên hình nền, hiệu ứng gợn trên màn hình xe bằng cảm ứng.
