# MASTER PLAN — YouTube Tablet (Android app, WebView)

## 1. Metadata & Overview

- **Mã dự án:** youtubeInBrave
- **Tên làm việc:** Ứng dụng YouTube cho Xiaomi Pad 7 Pro (tên và icon cuối cùng chưa chốt, có thể đổi sau)
- **Trạng thái:** Giai đoạn thiết kế đã xác nhận; chưa có mã nguồn
- **Tầm nhìn:** APK dùng cá nhân, mở website YouTube như một ứng dụng chuyên dụng trên máy tính bảng Android, có phát nền, PiP, hàng đợi cục bộ và bảng hiển thị nhanh.
- **Nỗi đau người dùng:** Website YouTube trên trình duyệt thiếu phát nền và PiP ổn định trên tablet; giao diện nhiều thành phần không cần thiết; không có hàng đợi tách khỏi playlist.
- **Tài liệu gốc:** [docs/app-design.md](docs/app-design.md) (đã xác nhận 2026-11-07); quyết định kiến trúc: [ADR 0002](docs/adr/0002-pure-android-app.md) (app Android thuần, thay ADR 0001), [ADR 0003](docs/adr/0003-webview-engine.md) (WebView)

## 2. Tech Stack & Architecture

- **Nền tảng:** app Android thuần, không fork Brave (ADR 0002). Web engine: WebView (ADR 0003).
- **Ngôn ngữ giao diện Android:** chưa chốt (Java hoặc Kotlin).
- **Môi trường build:** Android Studio/SDK trên Windows (đã có SDK, emulator, adb). Không dùng WSL Ubuntu.
- **Thiết bị kiểm thử:** Xiaomi Pad 7 Pro (phiên bản Android/HyperOS chưa xác nhận). Không suy diễn từ Carlinkit hay emulator.

**Kiến trúc 4 lớp** (theo app-design.md):
1. Web engine (WebView): tải trang, cookie/phiên đăng nhập, phát media. Chặn quảng cáo: chưa có giải pháp (không còn Shields).
2. Giao diện Android: điều hướng, nút PiP, bảng hiển thị nhanh, bảng hàng đợi. PiP và phát nền do ứng dụng tự triển khai.
3. Lớp tích hợp YouTube: nhận diện video, vị trí, trạng thái phát; lọc thành phần giao diện. Phần cần bảo trì khi website đổi.
4. Lưu trữ cục bộ: cài đặt hiển thị, hàng đợi, vị trí xem dở. Phiên đăng nhập do cookie của WebView quản lý.

Không dùng iframe player thay website. Không xây máy chủ đồng bộ.

## 3. Phased Roadmap

**Phase 1 — MVP**
- [ ] Bước 1: APK chứng minh nền tảng — đăng nhập/giữ phiên, chặn quảng cáo, phát nền, PiP trên Xiaomi Pad 7 Pro
- [ ] Bước 2: Giao diện chuyên dụng, bảng hiển thị nhanh (Shorts, hàng đợi, bình luận, video đề xuất), hàng đợi độc lập, lời nhắc tiếp tục xem, xử lý gián đoạn và lỗi

**Phase 2 — Expansion**
- [ ] Audio-only: giữ video/vị trí/hàng đợi, nghe nền không hiện PiP, chỉ đạt khi đo được giảm tải dữ liệu thật

**Phase 3 — Optimization**
- [ ] Cập nhật tương thích khi YouTube/Brave/Chromium thay đổi
- [ ] Tối ưu hiệu năng và pin trên tablet

## 4. Current Active Focus

- **Phase:** 1 — MVP
- **Việc đang làm:** Chuẩn bị Bước 1 — chờ xác nhận môi trường build (WSL Ubuntu, dung lượng, thời gian) và thông tin Android/HyperOS của thiết bị.
- **Ticket:** Chưa tạo (chưa qua `/to-spec` và `/to-tickets`).

## 5. Progress Log

* [07/11/2026 - 17:46] | Phase: 1 — MVP | Status: Ticket #3 (youtube.com toàn màn hình trong WebView) xong
  - Hoàn thành: WebView tải youtube.com, ẩn thanh hệ thống, Back theo lịch sử WebView, hết lịch sử thì thoát; kiểm chứng trên Pad_Android15.
  - Tiếp theo: Ticket #4 (đăng nhập Google trong WebView). Chưa có issue, chờ tạo.

* [07/11/2026] | Phase: Thiết kế | Status: Xác nhận thiết kế app-design.md; thiết lập repo, quy trình issue và các file quản lý
  - Hoàn thành: Git repo + remote GitHub; cấu hình issue tracker (GitHub) và 5 label triage; hook tự commit/push khi kết thúc phiên.
  - Tiếp theo: Xác nhận môi trường build và bắt đầu Bước 1.

## 6. Out of Scope (Non-Goals)

Không làm trong bản đầu (Phase 1). Yêu cầu mới rơi vào đây → hỏi trước khi làm.

- Đồng bộ tiếp tục xem giữa các thiết bị; tiện ích máy tính; tích hợp TV
- Bộ đếm dislike (tiêu đề cũ có chữ "True Dislike" nhưng không có yêu cầu chức năng này)
- Tải video offline; hệ thống phát hành công khai
- Tìm kiếm bằng giọng nói riêng; phím vô-lăng; bản đồ/chỉ đường, âm thanh chỉ đường/cuộc gọi
- Audio-only (thuộc Phase 2)
- Máy chủ đồng bộ riêng

## 7. Global Acceptance Criteria

**Phase 1:**
- Bước 1 đạt đủ bốn điều kiện trên thiết bị thật: đăng nhập và giữ phiên; chặn quảng cáo (kiểm với video/tài khoản thực); phát nền khi khóa màn hình; PiP khi Home/chuyển ứng dụng. Ghi rõ phiên bản Brave, Android/HyperOS và thiết bị.
- Bước 2 đạt toàn bộ bảng kiểm thử trong app-design.md (đăng nhập, PiP, khóa màn hình, chặn quảng cáo, checklist, Shorts, sắp hàng đợi, tắt hàng đợi, hết hàng đợi, lỗi video/mạng, lời nhắc, cuộc gọi, xoay màn hình).

**Phase 2:** Audio-only đo được giảm tải dữ liệu thật (không chỉ ẩn hình); giữ đúng vị trí khi quay lại video.

**Phase 3:** Có quy trình cập nhật khi upstream thay đổi; không hồi quy các tiêu chí Phase 1.
