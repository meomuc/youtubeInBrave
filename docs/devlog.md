# Devlog — nhật ký kỹ thuật cho AI Architect

### [08/11/2026 - 12:56] - Task: Sửa app thoát khi thao tác hàng đợi; chọn video để phát từ hàng đợi (Cấp: M)
* **1. Thay đổi:**
  - Module/thư mục: `app/src/main/java/com/meomuc/youtubeintablet/`
  - File sửa: `QueuePanel.kt` — thêm `deferChange` (đẩy việc đổi danh sách ra sau khi sự kiện chạm kết thúc); nút phát tiếp theo, xóa và kéo dùng `deferChange`; tên mục là vùng chạm "Phát ngay" và tham số `onPlayNow`. `MainActivity.kt` — truyền `onPlayNow`: đưa mục lên đầu hàng đợi (`playNext`) rồi gọi `playNextFromQueue()`. `CHANGELOG.md`, `docs/devlog.md`.
  - Dependencies: không.
* **2. Quyết định cốt lõi:**
  - Nguyên nhân crash: `render()` gọi `removeAllViews()` ngay trong lúc hàng đang xử lý sự kiện chạm của chính nó; ViewRootImpl gặp view đã tách khỏi cây trong lúc đo, `dispatchCollectViewAttributes` ném NPE. Sửa: đẩy thay đổi qua `post`.
  - Chọn để phát: dùng lại luồng hàng đợi có sẵn thay vì tạo đường mới. Mục được đưa lên đầu và chỉ bị xóa khi `completeQueueLoad` xác nhận trang đã tải, nên mất mạng không làm mất mục.
* **3. Kiểm chứng:**
  - Tái hiện trước sửa: kéo "Video B" → app crash, `NullPointerException` trong `dispatchCollectViewAttributes`, stack trace từ `ViewRootImpl.performTraversals`.
  - Sau sửa (emulator, APK mới): kéo đổi thứ tự thành công, app không crash (0 FATAL); chạm "Video A" → URL `watch?v=HGgGgMDqszw`; hàng đợi giảm còn 2 mục (Video A đã rời).
  - `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug --rerun`: BUILD SUCCESSFUL; 81 test, 0 failures, 0 errors.
  - Chưa kiểm chứng: xóa và "Phát tiếp theo" sau sửa (cùng đường `deferChange`, chưa thao tác bằng tay); trên box và Pad.
* **4. Rủi ro / cần review:**
  - Hàng đợi bị bật tắt (`showQueue` = false): `playNextFromQueue` trả về sớm, nhưng panel đã ẩn nên không gặp trường hợp này từ UI.
  - Mục không tải được (video không tồn tại): trang vẫn hoàn tất tải, mục bị xóa khỏi hàng đợi như mọi mục khác.

### [08/11/2026 - 12:29] - Task: Làm nốt việc còn lại: MediaSession vô lăng, quyền thông báo, dọn code, bảng hàng đợi (Cấp: M)
* **1. Thay đổi:**
  - Module/thư mục: `app/src/main/java/com/meomuc/youtubeintablet/`, `app/src/test/...`, `app/src/main/AndroidManifest.xml`, `CHANGELOG.md`
  - File tạo: `MediaSessionRules.kt` (enum `MediaCommand`, `scriptFor`), `MaxHeightScrollView.kt`, `MediaSessionRulesTest.kt`.
  - File sửa: `MainActivity.kt` — `MediaSession` (callback play/pause/stop/next/previous), `updateMediaSession`, `skipToNext`, xin `POST_NOTIFICATIONS` (API 33+), giải phóng session trong `onDestroy`; `PlaybackStateRules.kt` — thêm hiệu ứng `UPDATE_MEDIA_SESSION`; `ResumeRules.kt`/`ResumeStore.kt` — `StoredResume` gom ba tham số; `QueuePanel.kt` — danh sách cao theo nội dung, tối đa 70%; bỏ các ghi chú "Bước N" trong KDoc.
  - Dependencies: không thêm.
* **2. Quyết định cốt lõi:**
  - Next: ưu tiên mục đầu hàng đợi khi hàng đợi bật; không có thì bấm `.ytp-next-button`. Trang xem mobile không có nút này khi không có danh sách phát, nên không có mục nào thì next không làm gì.
  - Previous: quá 3 giây thì tua về đầu, gần đầu thì `history.back()`.
  - Không có play/pause trên vô lăng xe (xác nhận từ ảnh); play/pause vẫn đăng ký trong MediaSession cho nguồn khác.
* **3. Kiểm chứng:**
  - `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug --rerun`: BUILD SUCCESSFUL; 81 test, 0 failures, 0 errors.
  - Emulator: phiên MediaSession active, PLAYING, `actions=567`; `KEYCODE_MEDIA_PAUSE` → log `state=false`; `KEYCODE_MEDIA_NEXT` → URL đổi sang mục đầu hàng đợi; `KEYCODE_MEDIA_PREVIOUS` khi đang phát → `currentTime` về gần 0, không đổi video; quyền thông báo được cấp (`granted=true`).
  - Chưa kiểm chứng: nút vô lăng thật trên box; app ở nền khi bấm nút; hiển thị điều khiển trên màn hình xe.
* **4. Rủi ro / cần review:**
  - Lần cài đầu, dialog xin quyền thông báo hiện trên màn hình; người dùng từ chối thì app vẫn chạy nhưng thông báo phát nền không hiện.
  - Lệnh previous khi video vừa quá 3 giây chỉ tua về đầu; nếu muốn chuyển video trước đó cần lịch sử riêng.

### [08/11/2026 - 12:17] - Task: Thiết kế lại giao diện native theo Material 3, icon Phosphor (Cấp: L)
### [08/11/2026 - 12:17] - Task: Thiết kế lại giao diện native theo Material 3, icon Phosphor (Cấp: L)
* **1. Thay đổi:**
  - Module/thư mục: `app/src/main/java/com/meomuc/youtubeintablet/`, `app/src/main/res/`, thư mục gốc (`DESIGN.md`), `design-system/`, `docs/`
  - File tạo/sửa:
    - `QuickPanel.kt` — viết lại: `MaterialCardView` + `MaterialSwitch`, icon Phosphor theo hàng, vùng chạm 56dp, màu từ theme.
    - `QueuePanel.kt` — viết lại: `MaterialCardView`, nút icon tonal (`materialIconButtonFilledTonalStyle`), giữ nguyên logic kéo thả, phát tiếp theo, xóa.
    - `MainActivity.kt` — nút nổi thành `MaterialButton`; banner tiếp tục xem thành `MaterialCardView` với nút chữ "Tiếp tục" và "Bỏ qua"; PiP dùng `ic_ph_pause`/`ic_ph_play`; bật `DynamicColors.applyToActivityIfAvailable`; bỏ hằng màu cứng và import thừa.
    - `PlaybackService.kt` — icon thông báo `ic_ph_play` thay cho `android.R.drawable`.
    - `res/values/themes.xml` (tạo) — `Theme.YoutubeTablet` kế thừa `Theme.Material3.DayNight.NoActionBar`; `AndroidManifest.xml` khai báo theme.
    - `res/drawable/ic_ph_pause.xml`, `ic_ph_video.xml`, `ic_ph_chat_circle.xml`, `ic_ph_squares_four.xml` (tạo, lấy từ Phosphor 2.1.1 qua CDN).
    - `app/build.gradle.kts` — thêm `com.google.android.material:material:1.12.0`.
    - `DESIGN.md` (tạo), `design-system/MASTER.md` (tạo), `docs/ui-icons.md`, `CHANGELOG.md`.
  - Dependencies: `com.google.android.material:material:1.12.0`. Lý do: chủ dự án chọn Material 3 chuẩn; Views thuần không có switch, card, button Material 3.
* **2. Quyết định cốt lõi:**
  - Giữ Android Views (không chuyển sang Compose): code hiện tại là Views, không cần thêm Compose để đổi giao diện.
  - Thế giới thị giác là Material 3 chuẩn, thay cho "lưới kỹ thuật" trong surface brief. Ghi trong `DESIGN.md`.
  - UI UX Pro Max không có trên máy (không có `search.py`), dùng skill `impeccable` và quy tắc `AGENTS.md`; `design-system/MASTER.md` viết tay.
  - Chữ trên các nút quan trọng (Tiếp tục, Bỏ qua) để không phụ thuộc icon (AGENTS.md: icon đứng một mình phải có nhãn).
* **3. Kiểm chứng:**
  - `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug --rerun`: BUILD SUCCESSFUL; 77 test, 0 failures, 0 errors.
  - Emulator 1280x800, chế độ sáng và tối (`cmd uimode night`): chụp `ui-home-light.png`, `ui-quick-light.png`, `ui-queue-light.png`, `ui-quick-dark.png` trong scratchpad. Quan sát: card, switch, icon và nút đúng theo M3; banner có nhãn chữ; chế độ tối tương phản đủ.
  - Chưa kiểm chứng: Xiaomi Pad 7 Pro; cỡ chữ hệ thống lớn (`font_scale`); Dynamic Color trên hình nền thật; hiệu ứng gợn bằng cảm ứng trên màn hình xe; PiP icon trên thiết bị thật.
* **4. Rủi ro / cần review:**
  - Bảng hàng đợi cao 70% màn hình, nhiều khoảng trống khi ít mục; có thể thu gọn theo số mục.
  - Màu mặc định của Material 3 (tím) là bảng màu chuẩn; chưa có màu thương hiệu, cần chủ dự án chọn nếu muốn khác.
  - Viền và màu nút trên trang YouTube (JS) vẫn theo bảng màu tạm trong `docs/ui-icons.md`; không đồng bộ với theme native.

### [08/11/2026 - 12:05] - Task: Tái cấu trúc MainActivity theo Clean Architecture, bước 1–3 (Cấp: M)
### [08/11/2026 - 12:05] - Task: Tái cấu trúc MainActivity theo Clean Architecture, bước 1–3 (Cấp: M)
* **1. Thay đổi:**
  - Module/thư mục: `app/src/main/assets/js/`, `app/src/main/java/com/meomuc/youtubeintablet/`, `app/src/test/java/com/meomuc/youtubeintablet/`
  - File tạo/sửa:
    - Bước 1: `assets/js/playback_hook.js`, `assets/js/toggle.js` (tạo; nội dung lấy từ raw string cũ); `JsAssets.kt` (tạo); `MainActivity.kt` — bỏ `PLAYBACK_HOOK_JS`, `TOGGLE_JS`, đọc từ assets trong `onCreate`; `JsAssetsTest.kt` (tạo).
    - Bước 2: `ResumeRules.kt` — thêm `sanitizeLoaded`, `shouldClear`, interface `ResumeRepository`, `InMemoryResumeRepository`; `ResumeStore` implement interface; `MainActivity.kt` dùng kiểu `ResumeRepository`; `ResumeRepositoryTest.kt` (tạo).
    - Bước 3: `PlaybackStateRules.kt` (tạo: enum `PlaybackEffect`, `effectsFor`); `MainActivity.kt` — `setPlaying` dùng quy tắc, `applyEffect` thực hiện; `PlaybackStateRulesTest.kt` (tạo).
  - Dependencies: Không.
* **2. Quyết định cốt lõi:**
  - Bước 1: chỉ chuyển JS tĩnh; `QueueWebScript.SCRIPT` giữ nguyên vì được nội suy từ `WatchLinkRules` và `PhosphorIcons`.
  - Bước 2: `ResumeStore.clearIf` nay dựa trên `load()`. Khác biệt nhỏ: bản ghi thiếu title trước đây vẫn bị xóa khi trùng video, nay không bị xóa, nhưng `load()` vẫn trả null nên không ảnh hưởng hiển thị.
  - Bước 3: quyết định hiệu ứng (ẩn banner, khởi động service, hẹn dừng, làm mới PiP) nằm trong hàm thuần có test; thứ tự hiệu ứng được khóa bằng test.
  - Bước 3 chưa đổi phần MediaSession (bước 3 của kế hoạch phát vô lăng); đó là việc riêng.
* **3. Kiểm chứng:**
  - Sau mỗi bước: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug --rerun`: BUILD SUCCESSFUL. Tổng test: 66 → 69 (bước 1) → 74 (bước 2) → 77 (bước 3), 0 failures, 0 errors.
  - Test đỏ quan sát được trước mỗi bước: `JsAssetsTest` (file chưa có), `ResumeRepositoryTest` (lỗi biên dịch: thiếu hàm), `PlaybackStateRulesTest` (thiếu `PlaybackEffect`).
  - Emulator sau bước 1: `video` phát (t tăng), cờ `__ytTabletHooked=true` xác nhận script từ assets đã chạy.
  - Emulator sau bước 3: logcat `Playback: state=true` lúc phát, `state=false` lúc tạm dừng.
  - `MainActivity.kt`: 761 → 693 dòng.
  - Chưa kiểm chứng: PiP, banner tiếp tục xem, service phát nền trên box thật.
* **4. Rủi ro / cần review:**
  - `auto-commit.sh` (SessionEnd) tự `git add -A`, commit và push `origin HEAD`. Hai commit `297ff75` và `321d01b` đã được push lên `integration/buoc-1` mà chưa qua test trên box.
  - Commit bước 1–3 (`d9ec964`, `8a603fd`, `7b7034f`) sẽ bị push cùng lúc khi kết thúc phiên.
  - Chưa có ViewModel hay tầng domain đầy đủ; các bước này chỉ tách logic thuần khỏi Activity.

### [08/11/2026 - 11:56] - Task: Sửa video không phát (audio focus tự pause) và gỡ PlaybackAudioFocus (Cấp: S)
* **1. Thay đổi:**
  - Module/thư mục: `app/src/main/java/com/meomuc/youtubeintablet/`, `app/src/test/java/com/meomuc/youtubeintablet/`
  - File tạo/sửa: `MainActivity.kt` — bỏ `audioFocus` (lateinit, khởi tạo, gọi acquire/release trong `setPlaying`) và hằng `PAUSE_JS`; `PlaybackAudioFocus.kt` — xóa; `AudioFocusRulesTest.kt` — xóa; `CHANGELOG.md` — mục Fixed; `docs/devlog.md` — mục này.
  - Dependencies: Không.
* **2. Quyết định cốt lõi:**
  - Nguyên nhân: Chromium (WebView) xin audio focus khi bắt đầu phát, listener của app nhận `AUDIOFOCUS_LOSS` từ chính app, gọi `release()` và `PAUSE_JS` nên video bị pause ngay. Lỗi không phụ thuộc mạng (tái hiện trên Wi-Fi emulator); chưa thử 5G.
  - Chọn để WebView tự quản lý focus, không giữ focus từ app. Hệ quả: tính năng pause khi mất focus của ticket #18 không còn từ app; chưa kiểm chứng Chromium có tự pause khi có cuộc gọi hay không.
  - Không thể "bỏ qua LOSS do chính app gây ra": khi Chromium chiếm focus, app mất khả năng nhận sự kiện focus.
* **3. Kiểm chứng:**
  - Trước sửa: `video.readyState=4`, `play()` resolve nhưng `paused=true` sau vài giây; logcat có `requestAudioFocus` từ AudioFocusDelegate rồi `abandonAudioFocus` từ `PlaybackAudioFocus`.
  - Sau sửa (emulator, APK debug cuối): `paused=false`, `currentTime` tăng 336.47 → 341.53 trong 5 giây, `error=null`.
  - `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug --rerun`: BUILD SUCCESSFUL; 66 test, 0 failures, 0 errors.
  - Chưa kiểm chứng: trên box trong xe và trên 5G; pause khi có cuộc gọi.
* **4. Rủi ro / cần review:**
  - Mất pause khi mất focus từ app (xem quyết định trên).
  - Bản release `v0.1.0-debug` trên GitHub chứa lỗi phát video; APK mới chưa được publish.
  - Đính chính: mục trước ghi 62 test; con số đúng của lần chạy đó là 70 (10 class test, gồm `AudioFocusRulesTest` 4 test đã xóa ở mục này).

### [08/11/2026 - 11:43] - Task: Ignore thư mục nháp/worktree, push và publish bản build debug v0.1.0-debug (Cấp: S)
* **1. Thay đổi:**
  - Module/thư mục: thư mục gốc, `.gitignore`, GitHub Release `v0.1.0-debug`
  - File tạo/sửa: `.gitignore` — thêm `.scratch/` và `.claude/worktrees/` (commit `4628390`, đã push `integration/buoc-1`); `docs/devlog.md` — mục này.
  - Dependencies: Không.
* **2. Quyết định cốt lõi:**
  - Không tạo branch `main`: repo chưa có `main` ở local lẫn GitHub; chờ người dùng quyết định.
  - Bản build đưa lên dạng pre-release đính kèm APK, không commit APK (`*.apk` đã bị ignore). APK debug ký bằng debug key; `buildTypes.release` chưa có signing config nên không dùng bản release.
  - Chạy lại test với `--rerun` để tránh kết quả cache `UP-TO-DATE`.
* **3. Kiểm chứng:**
  - `git ls-remote --heads origin`: `integration/buoc-1` = `4628390`, khớp HEAD local.
  - `./gradlew.bat :app:testDebugUnitTest --rerun`: 62 test, 0 failures, 0 errors (10 class test).
  - `./gradlew.bat :app:assembleDebug`: BUILD SUCCESSFUL; APK `app-debug.apk`, SHA-256 `5fe25ad42a0c66c45e71ee53ea45c7b11377a4d42a709a2bc875aec7ff57633f` (bản đính kèm release).
  - `gh release view v0.1.0-debug`: pre-release, 1 asset `youtube-in-tablet-0.1.0-debug.apk`. URL: https://github.com/meomuc/youtubeInBrave/releases/tag/v0.1.0-debug
* **4. Rủi ro / cần review:**
  - Release là bề mặt công khai; repo `meomuc/youtubeInBrave` (chưa kiểm tra visibility) — APK debug có log `Log.i` đường dẫn request (xem mục ADR 0004), không nên phân phối rộng.
  - Các branch `worktree-agent-*` chỉ ở local, chưa push.
  - Git cảnh báo LF → CRLF khi chạm `.gitignore`; chưa có `.gitattributes`.

### [07/11/2026 - 18:40] - Task: Chặn quảng cáo bằng engine adblock-rust của Brave qua NDK (ADR 0004) (Cấp: L)
* **1. Thay đổi:**
  - Module/thư mục: `native/adblock-jni/` (crate Rust), `native/build-adblock.sh`, `app/src/main/java/.../AdEngine.kt`, `AdEngineNative.kt`, `FilterListLoader.kt`, `RequestTypeMapper.kt`, `MainActivity.kt`; xóa `AdBlocker.kt` và test danh sách tên miền.
  - File tạo/sửa: `docs/adr/0004-adblock-rust-engine.md` (accepted), `docs/adblock-lists.md`, `.gitignore` (bỏ qua `target/` và `jniLibs/`).
  - Dependencies: crate `adblock` 0.13.5 (MPL-2.0), `jni` 0.21.1 (Rust); không thêm thư viện Android.
* **2. Quyết định cốt lõi:**
  - Brave không nhúng được Shields; dùng engine `adblock-rust` của Brave, đọc EasyList và EasyPrivacy (ADR 0004).
  - `.so` build bằng `native/build-adblock.sh` cho arm64-v8a và x86_64; không commit binary.
  - `Engine` có `RefCell` nên `AdEngine` đồng bộ hóa mọi lần gọi.
  - Không chặn main frame. Loại request suy ra từ đuôi đường dẫn và Accept (WebView API 26 không cung cấp).
  - Danh sách tải khi mở app, cache 7 ngày; chưa có bản đóng gói sẵn.
* **3. Kiểm chứng:**
  - `cargo test` (native/adblock-jni): 4/4 pass. Lưu ý: tôi viết triển khai cùng lúc với test nên không quan sát được giai đoạn đỏ cho crate này.
  - `./gradlew.bat :app:testDebugUnitTest`: 7/7 pass (`RequestTypeMapperTest`); test đỏ quan sát được (lỗi biên dịch, chưa có lớp) với mapper.
  - Emulator Pad_Android15: tải EasyList+EasyPrivacy (3,5 triệu ký tự), `engine ready=true`, nạp `libadblock_jni.so` không lỗi, không crash.
  - Request bị chặn trong một phiên ngắn (21 lần): `/api/stats/ads`, `/ptracking`, `/pagead/`, `/instream/ad_status.js`, `/sodar`, `/pcs/activeview`. Các endpoint này là quảng cáo và theo dõi.
  - Video vẫn phát sau khi chặn (service phát chạy).
  - Chưa kiểm chứng: số liệu quảng cáo còn sót (cần đăng nhập và đo theo `docs/adblock-measurement.md`, đo lại vì engine đã đổi); quảng cáo chèn trong video (giới hạn của bộ lọc mạng).
* **4. Rủi ro / cần review:**
  - Chưa có scriptlet và bộ lọc cosmetic. Brave dùng các phần này cho quảng cáo YouTube; đây là việc tiếp theo nếu số liệu đo còn cao.
  - `Log.i` ghi đường dẫn request (tối đa 80 ký tự) để đo; có thể chứa ID video. Tắt hoặc chỉ giữ ở bản debug trước khi phát hành.
  - Trong lúc chưa tải xong danh sách, request không bị chặn.
  - Số liệu đo ở ticket #8 cũ (danh sách tên miền) không dùng để so sánh.

### [07/11/2026 - 18:30] - Task: Ticket #4 và #7 — Đăng nhập Google (một phần) và xoay màn hình, báo mất mạng (Cấp: S/M)
* **1. Thay đổi:**
  - Module/thư mục: `app/src/main/java/com/meomuc/youtubeintablet/`, `app/src/main/AndroidManifest.xml`
  - File tạo/sửa:
    - `MainActivity.kt` — gỡ `; wv` và `Version/4.0` khỏi User-Agent; bật cookie (commit `cfb3bd0`); chặn scheme không phải http/https; `onReceivedError` hiện trang mất mạng (#7).
    - `AndroidManifest.xml` — `configChanges="orientation|screenSize|screenLayout|smallestScreenSize"` để xoay không tạo lại Activity (commit `9b97446`).
  - Dependencies: Không.
* **2. Quyết định cốt lõi:** Không đổi engine (ADR 0003 giữ nguyên) vì trang đăng nhập Google hiện đúng. Xoay màn hình xử lý bằng `configChanges`, không lưu trạng thái WebView.
* **3. Kiểm chứng:**
  - #4: trang đăng nhập Google hiện form email, không có lỗi "trình duyệt không an toàn" (ảnh `ticket4-google.png`). Chưa đăng nhập tài khoản thật; chưa kiểm chứng giữ phiên sau khi mở lại.
  - #4: trước khi sửa, nút "Open App" hiện `ERR_UNKNOWN_URL_SCHEME`; sau khi chặn scheme lạ, trang chủ hiện bình thường.
  - #7: mất mạng (tắt Wi-Fi) hiện trang thông báo tiếng Việt (ảnh `ticket7-offline.png`).
  - #7: xoay màn hình ROTATION_0 ↔ ROTATION_90; PID app không đổi (3739), không có log relaunch.
* **4. Rủi ro / cần review:**
  - #4 chưa xong: cần tài khoản thật để xác nhận đăng nhập và giữ phiên.
  - Trang mất mạng dùng `loadDataWithBaseURL` nên có thể thêm một mục lịch sử; Back có thể quay về trang lỗi.
  - Ghi chú: các commit `cfb3bd0` và `9b97446` đã push; mục này ghi bù.

### [07/11/2026 - 18:10] - Task: Tickets #5, #6, #8 — Phát nền, PiP, chặn quảng cáo theo danh sách (Cấp: L)
* Lưu ý: commit `aa9aaf7` gộp cả ba ticket; không tách được. Ghi nhận như một lỗi quy trình ("1 ticket = 1 commit").
* **Sửa theo review (commit kế tiếp):**
  - Cờ `__ytTabletUserPaused` được xóa khi video đang phát, nên tự phát lại ở nền vẫn hoạt động sau một lần tạm dừng bằng nút PiP.
  - Trang không báo trạng thái "dừng" do hệ thống khi đang ở nền, và việc dừng service được trì hoãn 1 giây (hủy nếu phát lại). Tránh dừng rồi bật lại service (Android 12+ có thể chặn `startForegroundService` ở nền). Kiểm chứng: sau Home, log không còn cặp false/true, service vẫn chạy.
  - Hook JS chỉ gắn trên host youtube.com.
  - Nút PiP trong app ẩn khi đang ở cửa sổ PiP.
  - Bỏ `google-analytics.com` khỏi danh sách chặn (không phải quảng cáo, ngoài phạm vi #8).
  - Bỏ biến `blockedCount` không dùng; sửa tên test.
  - Còn rủi ro: `AndroidPlayback` vẫn gắn cho mọi frame (hạn chế của `addJavascriptInterface`); cần `WebViewCompat.addWebMessageListener` để giới hạn origin nếu thêm chức năng.

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
