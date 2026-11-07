---
status: accepted
supersedes: 0001
---

# Dùng app Android thuần thay cho fork Brave

Người dùng đổi khỏi fork Brave Android (ADR 0001) khi chốt phạm vi Bước 1, vì fork bắt buộc build trên Linux và môi trường Linux không được chấp nhận trên máy phát triển này. App Android thuần build được bằng Android Studio trên Windows và chạy trên emulator. Đánh đổi: không còn Shields, nên chặn quảng cáo phải tự triển khai hoặc tìm cơ chế khác; PiP và phát nền phải tự cài đặt ở tầng ứng dụng; đăng nhập Google trong WebView có rủi ro chính sách embedded user-agent, nên đăng nhập là tiêu chí kiểm chứng đầu tiên. Lựa chọn WebView hay GeckoView chưa chốt và phải được quyết định sau khi kiểm chứng đăng nhập. Kết quả Brave Nightly trên emulator chỉ là bằng chứng tham khảo, không thay thế kiểm chứng trên Xiaomi Pad 7 Pro.
