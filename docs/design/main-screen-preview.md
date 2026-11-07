# Bản mô tả thiết kế xem trước: màn hình chính (Lưới Crouwel)

Trạng thái: bản xem trước, chưa có mã nguồn, chưa kiểm chứng bằng ảnh chụp. Direction contract đầy đủ nằm ở `.impeccable/surfaces/main-screen.md`.

## 1. Ý tưởng

Lưới kỹ thuật kiểu Wim Crouwel lộ ra làm hệ thống căn chỉnh. Đường lưới mảnh có thể bật/tắt; các ô lưới căn cho navigation rail và bảng nhanh. Trên màn hình xem video, lưới chỉ là khung, không thay thế nội dung YouTube.

## 2. Bố cục tablet ngang (xem trước, 1280 × 800 dp)

```
┌────┬──────────────────────────────────────────────┬────────────────┐
│ ☰  │  [ WebView: youtube.com ]                    │ BẢNG NHANH     │
│    │                                              │ ─────────────  │
│ ⌂  │                                              │ Shorts    [ ] │
│Home│                                              │ Hàng đợi  [■] │
│    │                                              │ Bình luận [■] │
│ ≡  │                                              │ Đề xuất   [■] │
│Hàng│                                              │                │
│đợi │                                              │                │
│    │                                              │                │
│ ⚙  │                                              │                │
│Cài │                                              │                │
│đặt │                                              │                │
│────│                                              │                │
│ ⧉  │                                              │                │
│ PiP│                                              │                │
└────┴──────────────────────────────────────────────┴────────────────┘
```

- **Navigation rail (cột trái, ~80 dp):** Trang chủ, Hàng đợi, Cài đặt. Nút PiP là hành động chính, đặt ở đáy rail.
- **Vùng nội dung:** WebView chiếm phần lớn màn hình. Không có thanh địa chỉ hay tab.
- **Bảng nhanh (góc trên phải, ~240 dp):** card trên lưới với bốn switch. Shorts mặc định tắt; ba mục còn lại mặc định bật.

## 3. Bố cục điện thoại dọc (xem trước)

Navigation bar dưới cùng thay rail. Bảng nhanh mở bằng bottom sheet từ nút góc trên phải, không chiếm chỗ của WebView.

## 4. Màu và kiểu chữ

Ánh xạ sang vai trò Material 3, không dùng mã màu cố định trong code:

| Vai trò | Dùng cho |
|---|---|
| surface | nền ứng dụng và vùng WebView |
| surface-variant | card bảng nhanh, rail |
| outline | đường lưới mảnh (bật/tắt) |
| primary | switch đang bật, nút PiP |
| on-surface | chữ và nhãn |

- Kiểu chữ: Material type scale; tiêu đề tab dùng chữ hoa lớn, nhãn dùng Label. Font hệ thống Roboto, không đổi font trong bản đầu.
- Không có gradient, không có bóng đổ kiểu mềm, không có kính mờ trang trí.

## 5. Trạng thái

- Switch bật/tắt: đổi màu primary; không đổi kích thước viền.
- Nút PiP: trạng thái bình thường, đang vào PiP, không khả dụng khi không có video đang phát.
- Mất mạng: thông báo Snackbar ngắn trên vùng WebView, không thay toàn bộ màn hình.

## 6. Tương tác

- System Back: quay lại trang trước trong WebView; hết lịch sử thì thoát app.
- Chuyển mục trên rail: không tải lại WebView nếu đang ở Trang chủ.
- Không có chuyển động trang trí. Chỉ dùng chuyển động chuẩn Material (fade-through) khi mở bottom sheet, và tôn trọng cài đặt giảm chuyển động của hệ thống.

## 7. Điều còn bỏ ngỏ

- Jetpack Compose hay Android Views cho lớp bọc ngoài (giả định: Compose).
- Icon chuẩn cho PiP và các mục rail; chưa có tên và icon app chính thức.
- Chưa kiểm chứng: Dynamic Color, chế độ tối, cỡ chữ hệ thống, chạm/vuốt trên thiết bị thật. Mọi kết quả thị giác phải chụp từ emulator hoặc thiết bị, không dựa vào bản vẽ này.
