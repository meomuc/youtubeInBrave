---
status: accepted
---

# Dùng WebView làm web engine cho app Android thuần

Người dùng chọn WebView thay cho GeckoView khi chốt câu hỏi mở của Bước 1, bổ sung cho ADR 0002. WebView là thành phần có sẵn của Android, không cần đóng gói engine riêng và build được bằng Android Studio trên Windows.

Hệ quả:
- **Đăng nhập Google:** rủi ro chính sách embedded user-agent vẫn còn. Đăng nhập là tiêu chí kiểm chứng đầu tiên của Bước 1. Nếu thất bại, phải viết ADR mới trước khi đổi engine.
- **Chặn quảng cáo:** dùng danh sách request chặn (block request theo danh sách) để thay Shields. Người dùng chọn hướng này. Chưa kiểm chứng: quảng cáo trong luồng video có thể không bị chặn hết; Bước 1 phải đo mức quảng cáo còn sót lại trên video thật.
- **PiP và phát nền:** ứng dụng tự triển khai, không kế thừa từ trình duyệt.
- **Phiên đăng nhập:** do cookie của WebView quản lý. Nhận diện tài khoản dựa trên phiên đăng nhập đã lưu; bản đầu không có phân vùng dữ liệu riêng theo tài khoản.
- **HyperOS:** có thể giới hạn phát nền hoặc PiP. Phải kiểm chứng trên Xiaomi Pad 7 Pro.
