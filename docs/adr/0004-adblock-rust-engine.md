---
status: accepted
---

# Chặn quảng cáo bằng engine adblock-rust của Brave qua NDK

Người dùng chọn hướng này khi thấy quảng cáo vẫn hiện với danh sách tên miền (ADR 0003). Brave không cho nhúng Shields vào WebView, nên hướng gần nhất là dùng chính engine lọc mà Brave dùng: crate `adblock` (adblock-rust, giấy phép MPL-2.0), đọc danh sách lọc theo cú pháp Adblock Plus (EasyList, EasyPrivacy).

Quyết định này thay phần "Chặn quảng cáo" của ADR 0003 bằng engine lọc đầy đủ. Không đổi web engine (vẫn là WebView).

Hệ quả:
- **Mã native:** crate Rust nằm trong `native/adblock-jni/`, biên dịch thành thư viện `.so` cho `arm64-v8a` (Xiaomi Pad 7 Pro) và `x86_64` (emulator). Gọi từ Kotlin qua JNI. Thư viện `.so` không commit vào git; script build tạo ra.
- **Công cụ build:** cần Android NDK và target `aarch64-linux-android`, `x86_64-linux-android` của Rust. Không dùng WSL.
- **Danh sách lọc:** EasyList và EasyPrivacy, tải khi mở app và lưu cache, làm mới sau 7 ngày. Khi tải lỗi dùng cache cũ; chưa có cache thì chưa chặn gì. Chưa có bản đóng gói sẵn trong assets (việc còn lại). Giấy phép được ghi trong `docs/adblock-lists.md`.
- **Giới hạn:** quảng cáo chèn trong video YouTube vẫn có thể không bị chặn hết nếu chỉ dùng bộ lọc mạng. Brave xử lý phần này bằng scriptlet và bộ lọc cosmetic. Bản đầu chỉ có bộ lọc mạng; scriptlet là việc tiếp theo.
- **Đo lại:** số liệu nghiệm thu của ticket 08 phải đo lại sau khi đổi engine. Số liệu của danh sách tên miền không dùng để so sánh.

Phương án đã cân nhắc:
- Nhúng Shields của Brave: không khả thi, Shields là một phần của trình duyệt.
- Giữ danh sách tên miền: không đủ, quảng cáo trong video không bị chặn.
- Viết engine lọc riêng bằng Kotlin: tốn công, dễ sai cú pháp so với EasyList.
