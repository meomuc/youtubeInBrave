# Changelog

Mọi thay đổi đáng chú ý của dự án được ghi tại đây. Định dạng theo [Keep a Changelog](https://keepachangelog.com/vi/1.1.0/).

## [Unreleased]

### Changed
- Giao diện native theo Material 3: bảng hiển thị nhanh và bảng hàng đợi dùng thẻ và công tắc Material; nút nổi và banner tiếp tục xem đổi kiểu; sáng/tối và Dynamic Color theo hệ thống.
- Icon của app dùng bộ Phosphor thay cho icon hệ thống (nút PiP, thông báo phát nền).

### Fixed
- Video không phát được (phát xong bị tạm dừng ngay): app không còn tự xin audio focus, để WebView tự quản lý. Đã kiểm chứng trên emulator; chưa kiểm chứng trên box trong xe.

### Added
- Mở youtube.com toàn màn hình trong app, không có thanh địa chỉ hay tab.
- Nút Back quay lại trang trước; khi hết lịch sử thì thoát app.
- Đăng nhập Google trong app (chưa nghiệm thu đăng nhập đầy đủ).
- Video tiếp tục phát khi app xuống nền hoặc khóa màn hình; dừng khi tạm dừng.
- Bấm Home khi đang phát thì vào chế độ thu nhỏ (PiP); nút phát/tạm dừng trong PiP hoạt động. Nút PiP trong app đã có mã nhưng chưa kiểm chứng.
- Xoay màn hình không làm mất trang đang xem; báo rõ khi không có mạng.
- Chặn quảng cáo và theo dõi bằng engine adblock-rust của Brave, với danh sách EasyList và EasyPrivacy (chưa đo được mức quảng cáo còn sót).

Xem thêm [MASTER_PLAN.md](MASTER_PLAN.md).
