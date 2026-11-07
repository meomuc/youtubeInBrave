# Devlog — nhật ký kỹ thuật cho AI Architect

### [07/11/2026 - 17:08] - Task: Chốt WebView, nhận diện tài khoản và cách chặn quảng cáo (Cấp: M)
* **1. Thay đổi:**
  - Module/thư mục: `docs/adr/`, `docs/`, gốc repo
  - File tạo/sửa:
    - `docs/adr/0003-webview-engine.md` — tạo mới, chốt WebView (accepted)
    - `MASTER_PLAN.md` — bỏ các chỗ còn nhắc Brave; trỏ tới ADR 0002 và 0003
    - `docs/devlog.md` — mục này
    - Issue #1 (GitHub): thêm hai comment ghi quyết định
  - Dependencies: Không
* **2. Quyết định cốt lõi:**
  - Web engine: WebView (ADR 0003), thay cho GeckoView.
  - Nhận diện tài khoản: dựa trên phiên đăng nhập đã lưu trong WebView; bản đầu không phân vùng dữ liệu theo tài khoản.
  - Chặn quảng cáo: dùng danh sách request chặn để thay Shields.
  - HyperOS có thể giới hạn phát nền và PiP; kiểm chứng trên Xiaomi Pad 7 Pro.
* **3. Kiểm chứng:** Các comment trên issue #1 đã đăng (gh trả về URL). Chưa có mã nguồn nên chưa kiểm chứng hiệu quả chặn quảng cáo hay đăng nhập Google trong WebView.
* **4. Rủi ro / cần review:**
  - Chặn theo request có thể không chặn hết quảng cáo trong luồng video. Bước 1 phải đo.
  - Đăng nhập Google trong WebView có rủi ro chính sách embedded user-agent; chưa thử.
  - `docs/app-design.md` vẫn nhắc Brave và ADR 0001; chưa sửa.

### [07/11/2026 - 16:43] - Task: Thiết lập repo, quy trình và file quản lý dự án (Cấp: L)
* **1. Thay đổi:**
  - Module/thư mục: `/` (gốc repo), `.claude/`, `docs/agents/`, `docs/`
  - File tạo/sửa:
    - `AGENTS.md` — Global Agent Rules v2.2 + khối `## Agent skills`
    - `CLAUDE.md` — chỉ còn `@AGENTS.md`
    - `.gitignore` — loại trừ secret, build output, `docs/app-design.md`, `docs/design-interview.md`
    - `.claude/settings.json` — đăng ký hook `SessionEnd`
    - `.claude/hooks/auto-commit.sh` — stage, commit (nếu có thay đổi) và push `HEAD` lên `origin`
    - `docs/agents/issue-tracker.md`, `triage-labels.md`, `domain.md` — cấu hình Matt Pocock skills
    - `GLOSSARY.md` — thuật ngữ (đã có từ trước)
    - `MASTER_PLAN.md` — kim chỉ nam 7 phần
    - `docs/devlog.md` — file này
    - `CHANGELOG.md` — Keep a Changelog
    - `docs/app-design.md` — đánh dấu đã xác nhận ngày 07/11/2026
  - Dependencies: Không
* **2. Quyết định cốt lõi:**
  - Issue tracker: GitHub Issues qua `gh`, remote `origin` = `https://github.com/meomuc/youtubeInBrave`.
  - Domain docs: single-context (`GLOSSARY.md` + `docs/adr/` ở gốc repo).
  - Commit/push tự động ở sự kiện `SessionEnd` thay vì ghi nhớ trong prompt.
  - Nền tảng: [ADR 0001](adr/0001-brave-android-foundation.md) — fork Brave Android.
* **3. Kiểm chứng:**
  - `gh label list --repo meomuc/youtubeInBrave` — có đủ 5 nhãn: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`.
  - `git add -A --dry-run` — `docs/app-design.md` và `docs/design-interview.md` bị bỏ qua (`git check-ignore` xác nhận).
  - `bash -n .claude/hooks/auto-commit.sh` — cú pháp hợp lệ.
  - Chưa kiểm chứng: hook `SessionEnd` chưa chạy thật (chỉ chạy khi kết thúc phiên); chưa kiểm tra repo GitHub là public hay private.
* **4. Rủi ro / cần review:**
  - Lần kết thúc phiên đầu tiên sẽ commit và push các file theo dõi lên GitHub. Cần kiểm tra repo là public hay private trước.
  - Hook dùng `git add -A`: file bí mật mới mà không khớp `.gitignore` sẽ bị đẩy lên.
  - `AGENTS.md` đang chứa Global Rules, trái với quy ước `project_initialization` (mục này ghi không chép Global Rules vào AGENTS.md). Chờ người dùng quyết định tách file.
  - Môi trường build Bước 1 chưa xác nhận (WSL Ubuntu, dung lượng, thời gian).
