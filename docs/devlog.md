# Devlog — nhật ký kỹ thuật cho AI Architect

### [07/11/2026 - 18:10] - Task: Tickets #5, #6, #8 — Phát nền, PiP, chặn quảng cáo theo danh sách (Cấp: L)
* **1. Thay đổi:**
  - Module/thư mục: `app/src/main/java/com/meomuc/youtubeintablet/`, `app/src/test/`
  - File tạo/sửa:
    - `AdBlocker.kt` — danh sách tên miền quảng cáo, `isBlocked(url)` (ticket #8).
    - `AdBlockerTest.kt` — 6 unit test viết trước (đỏ do chưa có `AdBlocker`), sau đó xanh.
    - `PlaybackService.kt` — foreground service loại `mediaPlayback` (ticket #5).
    - `MainActivity.kt` — cầu `AndroidPlayback` nhận trạng thái phát; PiP khi `onUserLeaveHint`; nút PiP trong app; `RemoteAction` phát/tạm dừng; chặn request trong `shouldInterceptRequest`; bù tạm dừng khi xuống nền.
    - `AndroidManifest.xml` — quyền `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`; `supportsPictureInPicture`; khai báo service.
    - `app/build.gradle.kts` — `testImplementation("junit:junit:4.13.2")`.
  - Dependencies: JUnit 4.13.2 (chỉ test). Lý do: ticket #8 cần seam kiểm thử được cho logic chặn; dự án chưa có hạ tầng test.
* **2. Quyết định cốt lõi:**
  - Phát nền: service foreground chỉ chạy khi `isPlaying`; dừng khi tạm dừng, khi thoát (`onDestroy`).
  - YouTube tự tạm dừng khi trang bị ẩn. Xử lý bằng hai lớp: (a) giả lập `document.hidden`/`visibilityState` và chặn `visibilitychange`; (b) khi app xuống nền (`onPause` đặt cờ `__ytTabletBg`), trang tự `play()` lại nếu video bị tạm dừng, trừ khi người dùng bấm tạm dừng từ nút PiP (cờ `__ytTabletUserPaused`).
  - PiP: tỉ lệ 16:9, một nút phát/tạm dừng; cập nhật icon khi trạng thái đổi.
  - Chặn quảng cáo: chặn theo tên miền (ADR 0003); không chặn `googlevideo.com` hay `youtube.com`. Quảng cáo chèn trong video dùng cùng host với video nên danh sách này không chặn được; đây là giới hạn đã biết, chờ số đo.
  - Mỗi request bị chặn được ghi log tag `AdBlock` để đếm.
  - Log tag `Playback` ghi khi trạng thái phát đổi (phục vụ kiểm chứng).
* **3. Kiểm chứng (Pad_Android15, emulator):**
  - `./gradlew.bat :app:testDebugUnitTest`: 6/6 pass (`tests="6" failures="0"`).
  - `./gradlew.bat :app:assembleDebug`: BUILD SUCCESSFUL.
  - #6, Home khi đang phát: vào PiP (ảnh chụp: video thu nhỏ trên màn hình chính).
  - #6, nút phát/tạm dừng trong PiP: tạm dừng → log `state=false`, service dừng (`service=0`); phát lại → `state=true`, service chạy lại (`service=3`).
  - #5, phát nền: khi ở nền, thời gian phát tăng (0:28 → 0:47 trên PiP).
  - #5, khóa màn hình: service vẫn chạy; Chromium giữ audio focus `USAGE_MEDIA`.
  - #8: log `AdBlock` ghi nhận request bị chặn khi xem trang (5 dòng trong lần thử đầu).
  - Chưa kiểm chứng: thoát app (service phải dừng qua `onDestroy`); nút PiP trong app; quay lại app từ PiP; nghe âm thanh thật khi khóa; số liệu quảng cáo còn sót (cần đăng nhập và xem video thật).
  - Lưu ý thử nghiệm: broadcast từ adb không tới receiver của nút PiP vì receiver đăng ký `RECEIVER_NOT_EXPORTED`. Đó là hành vi đúng, không phải lỗi app; thử qua nút PiP thật thì được.
* **4. Rủi ro / cần review:**
  - Cầu `AndroidPlayback` được gắn cho mọi trang trong WebView; trang ngoài youtube.com cũng gọi được `onState`. Hậu quả chỉ là bật/tắt service, nhưng nên giới hạn theo origin nếu thêm chức năng khác.
  - Cờ `__ytTabletBg` đặt từ `onPause`; nếu WebView chưa kịp nhận JS, video có thể dừng vài trăm ms. Chưa đo thời gian.
  - Log `state=` và `AdBlock` ghi trong build debug; cân nhắc tắt ở release.
  - `onBackPressed` còn deprecated (xem mục ticket #3).
  - Ticket #8 chỉ hoàn thành phần chặn và cách đo; số liệu đo nghiệm thu thuộc ticket #9 (thiết bị thật).

### [07/11/2026 - 17:46] - Task: Ticket #3 — Mở youtube.com toàn màn hình trong WebView (Cấp: S)
* **1. Thay đổi:**
  - Module/thư mục: `app/src/main/java/com/meomuc/youtubeintablet/`
  - File tạo/sửa:
    - `MainActivity.kt` — thay khung chữ bằng WebView tải `https://www.youtube.com/`; bật JavaScript và DOM storage; ẩn thanh hệ thống (immersive); Back quay lại lịch sử WebView, hết lịch sử thì thoát; `WebChromeClient` xử lý video toàn màn hình (chưa kiểm chứng, xem mục 3). Đã gỡ lưu/khôi phục trạng thái WebView và nhánh `else` không dùng của hàm ẩn thanh hệ thống, theo nhận xét review Spec/Standards.
    - `CHANGELOG.md` — mục Added cho bản Unreleased.
    - `docs/devlog.md` — mục này.
  - Dependencies: Không (chỉ dùng API `android.webkit` có sẵn).
* **2. Quyết định cốt lõi:**
  - Dùng `Activity` thuần, không thêm AppCompat hay Jetpack, để giữ app nhỏ.
  - Back dùng `onBackPressed` (đã deprecated nhưng vẫn hoạt động trên minSdk 26); khi có video toàn màn hình thì Back thoát chế độ đó trước.
  - Không tự xử lý rotation (`configChanges`); để ticket #7.
  - Không có unit test: chặn Back và WebView cần Robolectric hoặc instrumented test (thư viện mới). Kiểm chứng bằng emulator thay thế.
* **3. Kiểm chứng:**
  - `./gradlew.bat :app:assembleDebug`: BUILD SUCCESSFUL (lỗi đầu tiên do trùng tên `setImmersive` với `Activity`, đã đổi thành `applyImmersiveMode`).
  - Cài trên `Pad_Android15` (`adb install -r`): Success; mở app không có `AndroidRuntime` lỗi.
  - Ảnh chụp: trang youtube.com tải được, không có thanh địa chỉ (`ticket3-home.png`).
  - Back: mở tab Shorts rồi bấm Back → quay về trang chủ, app vẫn chạy; Back tiếp → app thoát về launcher.
  - Đã thử nút toàn màn hình của video (3 lần bấm trên trình phát): không thấy app vào chế độ toàn màn hình, ảnh không rõ thanh điều khiển. Coi là CHƯA kiểm chứng; không ghi vào CHANGELOG.
  - Chưa kiểm chứng: đăng nhập Google (ticket #4).
* **4. Rủi ro / cần review:**
  - `onBackPressed` là API cũ; nếu nâng targetSdk lên 36+ thì chuyển sang `OnBackPressedDispatcher`.
  - Nút toàn màn hình của video là việc còn mở; cần kiểm chứng riêng (cách bấm, hoặc đo bằng `adb shell uiautomator dump`).
  - Commit `898bdfc` có mô tả tiếng Việt; AGENTS.md yêu cầu commit message tiếng Anh. Không viết lại commit đã push (cần force push); sẽ dùng tiếng Anh từ commit sau.
  - Mục này ghi cấp S, nhưng diff ~100 dòng và đổi hành vi; đáng lẽ là M (cần `/ecc:plan`).
  - Đăng nhập Google có thể bị chặn vì WebView; kiểm tra ở ticket #4.

### [07/11/2026 - 17:46] - Task: Ticket #2 — Khung app Android chạy được trên emulator (Cấp: S) — ghi bù
* **1. Thay đổi:**
  - Module/thư mục: `app/`, gốc repo
  - File tạo/sửa: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`, `MainActivity.kt` (khung TextView), `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/wrapper/*`, `gradlew`, `gradlew.bat` — commit `4c703e6`.
  - Dependencies: Không (chỉ Android SDK; Gradle wrapper 9.9.0, AGP 9.5.1).
* **2. Quyết định cốt lõi:** Mẫu tối thiểu: một Activity hiển thị tên app; chỉ quyền INTERNET; `allowBackup=false`.
* **3. Kiểm chứng:** build thành công; cài và mở trên `Pad_Android15` không crash; ảnh chụp xác nhận. Issue #2 đã đóng kèm comment bằng chứng.
* **4. Rủi ro / cần review:** Chưa có mục nhật ký này lúc commit; được ghi bù khi bắt đầu ticket #3.

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
