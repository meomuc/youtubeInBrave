# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

Greenfield, đã xác nhận: app Android native bằng **Kotlin**, lớp web engine là **WebView** (theo ADR 0003), build bằng Android Studio/SDK trên Windows. Không dùng fork Brave (ADR 0002). Chưa có mã nguồn.

## Users

Chỉ người sở hữu dự án, dùng cá nhân trên máy tính bảng. Không phát hành công khai (app-design.md mục 3). Thiết bị kiểm thử và mục tiêu: Xiaomi Pad 7 Pro (phiên bản Android/HyperOS chưa xác nhận).

## Product Purpose

Mở website YouTube như một ứng dụng chuyên dụng trên máy tính bảng Android, với phát nền và Picture-in-Picture (PiP) ổn định, hàng đợi phát cục bộ tách khỏi playlist YouTube, và bảng hiển thị nhanh để ẩn các thành phần không cần thiết. Thành công là dùng được đầy đủ trải nghiệm YouTube, đăng nhập và giữ phiên, và các hành vi nền tảng hoạt động đúng trên thiết bị thật.

## Positioning

Lớp vỏ Android quanh website YouTube thật (không dùng iframe player thay thế), bổ sung đúng những thứ trình duyệt trên tablet thiếu: phát nền, PiP, hàng đợi độc lập, và lọc giao diện theo lựa chọn cá nhân.

## Operating Context

- Dùng trên Xiaomi Pad 7 Pro, chế độ ngang và dọc.
- Đăng nhập tài khoản Google; dùng lịch sử, kênh đăng ký và playlist của tài khoản YouTube.
- Điều khiển bằng cảm ứng. Không có thanh địa chỉ/tab; có nút Quay lại, Trang chủ, PiP và bảng hiển thị nhanh ở góc trên bên phải.
- Bằng chứng từ thử nghiệm trước đây: Android box Carlinkit (Android 13), Brave đã chặn quảng cáo và phát nền được sau khi thử lại. Chưa thấy PiP trên thiết bị đó. Không suy diễn kết quả sang tablet.

## Capabilities and Constraints

- Bắt buộc trong bản đầu: đăng nhập và giữ phiên; phát nền (kể cả khi khóa màn hình); PiP khi Home hoặc chuyển ứng dụng, kèm nút PiP chủ động; chặn quảng cáo (cơ chế chưa chốt, không còn dựa vào Shields); hàng đợi cục bộ (Phát tiếp theo, Thêm cuối, kéo đổi thứ tự, xóa, khôi phục sau đóng/mở); lời nhắc tiếp tục xem (không tự phát); bảng hiển thị nhanh gồm Shorts (mặc định tắt), Hàng đợi, Bình luận và Video đề xuất (mặc định bật).
- Hành vi khi gián đoạn: cuộc gọi hoặc app khác chiếm âm thanh thì tạm dừng; sau đó chỉ phát lại khi người dùng bấm Play. Mất mạng giữ nguyên video, vị trí và hàng đợi.
- Ràng buộc: nội dung website YouTube có thể thay đổi, cần bảo trì lớp tích hợp.
- Thuật ngữ: "Hàng đợi", "Lời nhắc tiếp tục xem", "Bảng hiển thị nhanh", "PiP" (xem GLOSSARY.md).
- Chưa quyết: chặn quảng cáo bằng cơ chế nào; ngôn ngữ giao diện ngoài tiếng Việt (chưa xác nhận); phiên bản Android/HyperOS tối thiểu.
- Ngoài phạm vi bản đầu: đồng bộ giữa các thiết bị, tiện ích máy tính, tích hợp TV, tìm bằng giọng nói riêng, phím vô-lăng, bản đồ/chỉ đường, bộ đếm dislike, tải offline, audio-only (Phase 2).

## Brand Commitments

Chưa có. Tên và icon cuối cùng chưa chốt và có thể đổi sau; không ràng buộc nhận diện nào được xác nhận.

## Evidence on Hand

- `docs/app-design.md`: thiết kế đã xác nhận bởi người dùng ngày 2026-11-07 (Q1–Q19).
- `docs/adr/0002-pure-android-app.md`, `docs/adr/0003-webview-engine.md`: quyết định nền tảng.
- Không có ảnh chụp, mẫu thiết kế, tài liệu thương hiệu hay số liệu người dùng. Không được bịa các nội dung này.

## Product Principles

- Giữ website YouTube thật làm nguồn nội dung; app chỉ thêm điều khiển bên ngoài, không thay thế trình phát bằng iframe.
- Giao diện tối giản: không thanh địa chỉ, không tab; mọi thứ thêm vào phải có lý do dùng hằng ngày trên tablet.
- Dữ liệu ở trên máy: hàng đợi và vị trí xem dở lưu cục bộ; không xây máy chủ đồng bộ riêng.
- Lọc giao diện không xóa dữ liệu YouTube; lựa chọn người dùng được giữ qua lần mở lại.
- Không hứa điều chưa đo được: khôi phục vị trí và chặn quảng cáo phải được kiểm chứng trên thiết bị thật.
