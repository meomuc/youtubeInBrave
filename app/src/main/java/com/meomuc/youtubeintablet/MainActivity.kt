package com.meomuc.youtubeintablet

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.util.Rational
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.TextViewCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.R as MaterialR
import java.io.ByteArrayInputStream
import java.util.Locale

/**
 * Ticket #3: mở youtube.com toàn màn hình trong WebView, Back theo lịch sử.
 * Ticket #4: đăng nhập Google (UA không có dấu hiệu WebView).
 * Ticket #5: phát nền qua PlaybackService khi video đang phát.
 * Ticket #6: PiP khi bấm Home lúc đang phát, và nút PiP trong app.
 * Ticket #7: xoay màn hình không tạo lại Activity; báo lỗi mất mạng.
 * Ticket #8: chặn request quảng cáo theo AdBlocker.
 * Ticket #10: nút ☰ mở bảng hiển thị nhanh (4 mục bật/tắt, lưu lại).
 * Ticket #11, #12: ẩn Shorts, bình luận, video đề xuất theo lựa chọn (DisplayFilter).
 * Ticket #16: lưu vị trí xem dở và nhắc tiếp tục khi mở app (không tự phát).
 * Ticket #18: không còn xin audio focus từ app (WebView tự quản lý); việc pause khi có cuộc gọi chưa kiểm chứng.
 * Ticket #13: hàng đợi cục bộ (PlayQueue) lưu trong SharedPreferences; nút "Thêm vào hàng đợi" trên trang xem video.
 * Ticket #14: video kết thúc khi hàng đợi bật thì phát mục tiếp theo, hết thì dừng (PlayQueue.takeNextOnEnd).
 * Ticket #17: video không truy cập được thì bỏ qua sang mục tiếp theo, hoặc về trang chủ nếu là lời nhắc xem dở;
 * mất mạng giữ vị trí và hàng đợi, trang lỗi có liên kết thử lại đúng trang đang lỗi (PlaybackErrorRules).
 */
class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var root: FrameLayout
    private lateinit var pipButton: MaterialButton
    /** Nút ☰ góc dưới bên trái mở bảng hiển thị nhanh (ticket #10). */
    private lateinit var menuButton: MaterialButton
    private lateinit var quickPanel: QuickPanel
    /** Hàng đợi phát cục bộ (ticket #13), tách khỏi playlist YouTube; khôi phục khi mở app. */
    private lateinit var queue: PlayQueue
    /** Nút mở bảng hàng đợi, nằm phía trên nút ☰ (ticket #13). Chỉ có icon; số mục hiện ở [queueBadge]. */
    private lateinit var queueButton: MaterialButton
    /** Huy hiệu số mục trên nút hàng đợi (ticket #19). */
    private lateinit var queueBadge: TextView
    /** Khung chứa nút hàng đợi và huy hiệu; ẩn/hiện cả khung. */
    private lateinit var queueContainer: FrameLayout
    private lateinit var queuePanel: QueuePanel
    /** Lựa chọn hiển thị hiện tại; đọc từ SharedPreferences khi mở app (ticket #10). */
    private var displayOptions = DisplayOptions()
    private lateinit var resumeStore: ResumeRepository
    /** Script JS tĩnh, đọc từ assets/js khi tạo Activity. */
    private lateinit var toggleJs: String
    private lateinit var playbackHookJs: String

    /** Thanh nhắc tiếp tục xem; null khi không hiện. */
    private var resumeBanner: View? = null

    /** Video vừa bấm "Tiếp tục": cần tua tới vị trí này khi trang watch tải xong. */
    @Volatile private var pendingResume: ResumePoint? = null
    /** Id video đang được tải từ hàng đợi; chỉ bỏ mục khỏi hàng đợi khi trang tải xong (ticket #17). */
    private var queueLoadingId: String? = null
    /** Engine chặn quảng cáo (ADR 0004); null cho tới khi nạp xong danh sách lọc. */
    @Volatile private var adEngine: AdEngine? = null

    /** URL trang đang mở, dùng làm sourceUrl khi kiểm tra request. */
    @Volatile private var currentPageUrl: String = ""

    /** true khi là bản debug (dùng cho chế độ học phím và gỡ lỗi WebView). */
    private val isDebuggable: Boolean
        get() = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    private val handler = Handler(Looper.getMainLooper())
    private val stopServiceRunnable = Runnable {
        stopService(Intent(this, PlaybackService::class.java))
    }

    /** View video khi YouTube bấm nút toàn màn hình (null nếu không ở chế độ đó). */
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    /** Trạng thái phát do trang báo qua cầu JavaScript; dùng cho service và PiP. */
    private var isPlaying = false

    /** Phiên MediaSession nhận nút vô lăng và điều khiển hệ thống (kế hoạch điều khiển vô lăng). */
    private lateinit var mediaSession: MediaSession

    private val toggleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            webView.evaluateJavascript(toggleJs, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DiagLog.start(this)
        // Material You (Android 12+): lấy bảng màu từ hình nền; máy cũ dùng bảng màu tĩnh của theme.
        DynamicColors.applyToActivityIfAvailable(this)

        // Chỉ bản debug: cho phép gỡ lỗi WebView qua Chrome DevTools (kiểm tra selector trên YouTube).
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        // Google chặn đăng nhập khi UA có dấu hiệu WebView ("; wv", "Version/4.0").
        webView.settings.userAgentString = webView.settings.userAgentString
            .replace("; wv", "")
            .replace("Version/4.0 ", "")
        // Cookie được lưu trên đĩa, nên phiên đăng nhập còn sau khi đóng và mở lại app.
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        resumeStore = ResumeStore(this)
        toggleJs = JsAssets.read(this, "toggle.js")
        playbackHookJs = JsAssets.read(this, "playback_hook.js")
        webView.addJavascriptInterface(PlaybackBridge(), "AndroidPlayback")
        webView.addJavascriptInterface(QueueBridge(), "AndroidQueue")
        setupMediaSession()
        requestNotificationPermission()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // Chỉ tải web trong WebView. Scheme lạ (intent://, market://) bị bỏ qua
                // thay vì hiện trang lỗi ERR_UNKNOWN_URL_SCHEME.
                val scheme = request.url.scheme
                return scheme != "http" && scheme != "https"
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                // Không chặn trang chính (trang chính không phải quảng cáo); engine chưa sẵn sàng thì cho qua.
                if (request.isForMainFrame) return null
                val engine = adEngine ?: return null
                val url = request.url.toString()
                val type = RequestTypeMapper.typeOf(url, request.requestHeaders?.get("Accept"))
                if (!engine.shouldBlock(url, currentPageUrl, type)) return null
                // Đo: lọc log theo tag AdBlock (adb logcat -s AdBlock) để đếm số request đã chặn.
                Log.i(TAG_AD_BLOCK, "blocked host=${request.url.host} type=$type path=${request.url.encodedPath?.take(80)}")
                return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                currentPageUrl = url
            }

            override fun onPageFinished(view: WebView, url: String) {
                // Chỉ gắn hook phát nền cho YouTube, không cho trang bên thứ ba.
                if (Uri.parse(url).host?.endsWith("youtube.com") == true) {
                    view.evaluateJavascript(playbackHookJs, null)
                    view.evaluateJavascript(QueueWebScript.SCRIPT, null)
                    applyDisplayFilter()
                    seekPendingResume(view, url)
                    completeQueueLoad(url)
                }
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                // Chỉ báo lỗi khi chính trang chính thất bại; lỗi ảnh/script thì để trang tự xử lý.
                if (request.isForMainFrame) {
                    onMainFrameFailed(request.url.toString(), PlaybackErrorRules.isNetworkError(error.errorCode))
                }
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) {
                    callback.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback
                root.addView(view, FrameLayout.LayoutParams(MATCH, MATCH))
                webView.visibility = View.GONE
                applyImmersiveMode()
            }

            override fun onHideCustomView() {
                hideCustomView()
            }
        }

        root = FrameLayout(this)
        root.addView(webView, FrameLayout.LayoutParams(MATCH, MATCH))
        // Nút PiP trong app (ticket #6): vào PiP theo yêu cầu.
        val density = resources.displayMetrics.density
        val buttonSize = (BUTTON_SIZE_DP * density).toInt()
        pipButton = iconButton(R.drawable.ic_ph_picture_in_picture, "Thu nhỏ video (PiP)") { enterPip() }
        root.addView(
            pipButton,
            FrameLayout.LayoutParams(buttonSize, buttonSize, Gravity.BOTTOM or Gravity.END).apply {
                setMargins(0, 0, 24, 24)
            }
        )
        // Bảng hiển thị nhanh (ticket #10): lựa chọn lưu trong SharedPreferences.
        val prefs = getSharedPreferences(PREFS_DISPLAY, MODE_PRIVATE)
        displayOptions = DisplayOptions.fromStore { key, default -> prefs.getBoolean(key, default) }
        quickPanel = QuickPanel(this, displayOptions) { options ->
            displayOptions = options
            prefs.edit().apply { options.writeTo { key, value -> putBoolean(key, value) } }.apply()
            applyDisplayFilter()
        }
        menuButton = iconButton(R.drawable.ic_ph_list, "Mở bảng hiển thị nhanh") {
            if (quickPanel.isOpen) quickPanel.hide() else quickPanel.show()
        }
        val menuSize = buttonSize
        val menuMargin = (MENU_MARGIN_DP * density).toInt()
        root.addView(
            menuButton,
            FrameLayout.LayoutParams(menuSize, menuSize, Gravity.BOTTOM or Gravity.START).apply {
                setMargins(menuMargin, 0, 0, menuMargin)
            }
        )
        // Hàng đợi (ticket #13): đọc lại từ SharedPreferences. Mở app không tự phát.
        queue = PlayQueue(
            QueueCodec.decode(getSharedPreferences(PREFS_QUEUE, MODE_PRIVATE).getString(KEY_QUEUE_ITEMS, "") ?: "")
        )
        queuePanel = QueuePanel(
            context = this,
            queue = queue,
            onChanged = { onQueueChanged() },
            onPlayNow = { item ->
                // Đưa mục đã chọn lên đầu hàng đợi rồi tải như mục tiếp theo: chỉ xóa khỏi hàng đợi khi trang tải xong.
                queue.playNext(item)
                onQueueChanged()
                playNextFromQueue()
            },
        )
        queueButton = iconButton(R.drawable.ic_ph_queue, "Mở hàng đợi") {
            if (queuePanel.isOpen) queuePanel.hide() else queuePanel.show()
        }
        queueBadge = TextView(this).apply {
            setTextColor(MaterialColors.getColor(this, MaterialR.attr.colorOnPrimary))
            setBackgroundColor(MaterialColors.getColor(this, MaterialR.attr.colorPrimary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            setPadding((BADGE_PADDING_DP * density).toInt(), 0, (BADGE_PADDING_DP * density).toInt(), 0)
            minWidth = (BADGE_SIZE_DP * density).toInt()
            minHeight = (BADGE_SIZE_DP * density).toInt()
        }
        queueContainer = FrameLayout(this).apply {
            addView(queueButton, FrameLayout.LayoutParams(buttonSize, buttonSize, Gravity.TOP or Gravity.START))
            addView(
                queueBadge,
                FrameLayout.LayoutParams(WRAP, (BADGE_SIZE_DP * density).toInt(), Gravity.TOP or Gravity.END)
            )
        }
        root.addView(
            queueContainer,
            FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM or Gravity.START).apply {
                setMargins(menuMargin, 0, 0, menuMargin + menuSize + (QUEUE_GAP_DP * density).toInt())
            }
        )
        root.addView(queuePanel, FrameLayout.LayoutParams(MATCH, MATCH))
        refreshQueueUi()
        // Thêm sau cùng để bảng nằm trên nút ☰ và lớp mờ.
        root.addView(quickPanel, FrameLayout.LayoutParams(MATCH, MATCH))
        setContentView(root)
        applyImmersiveMode()
        // Mở app chỉ hỏi có tiếp tục không; không tự phát (ticket #16).
        resumeStore.load()?.takeIf { ResumeRules.shouldPrompt(it) }?.let { showResumeBanner(it) }

        val filter = IntentFilter(ACTION_TOGGLE)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(toggleReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(toggleReceiver, filter)
        }

        // Nạp danh sách lọc (có cache) và tạo engine ngoài luồng chính.
        Thread {
            val rules = FilterListLoader.load(cacheDir)
            adEngine = AdEngine.create(rules)
            Log.i(TAG_AD_BLOCK, "engine ready=${adEngine != null} chars=${rules.length}")
        }.start()

        webView.loadUrl(START_URL)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        when {
            customView != null -> hideCustomView()
            quickPanel.isOpen -> quickPanel.hide()
            queuePanel.isOpen -> queuePanel.hide()
            webView.canGoBack() -> webView.goBack()
            else -> super.onBackPressed()
        }
    }

    /**
     * Chế độ học phím (chỉ bản debug): hiện tên phím và ghi log mỗi lần nhấn xuống.
     * Dùng để biết mã phím của nút vô lăng trên xe (plan-android-box.md, bước 1).
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && isDebuggable) {
            val name = KeyEvent.keyCodeToString(event.keyCode)
            val text = "Phím: $name (${event.keyCode}) scan=${event.scanCode} src=${event.source}"
            Log.i(TAG_KEY_LEARN, text)
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
        return super.dispatchKeyEvent(event)
    }

    /** Bấm Home hoặc chuyển app khi đang phát thì vào PiP (ticket #6). */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isPlaying) enterPip()
    }

    override fun onPause() {
        super.onPause()
        DiagLog.log("activity onPause (pip=$isInPictureInPictureMode)")
        webView.evaluateJavascript(backgroundFlagJs(true), null)
    }

    override fun onResume() {
        super.onResume()
        DiagLog.log("activity onResume (pip=$isInPictureInPictureMode)")
        webView.evaluateJavascript(backgroundFlagJs(false), null)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Hệ thống có thể hiện lại thanh trạng thái sau khi người dùng chạm vào cạnh màn hình.
        if (hasFocus) applyImmersiveMode()
    }

    /** Ẩn nút PiP trong app khi đã ở cửa sổ PiP (nút này sẽ hiện trong cửa sổ nhỏ). */
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        DiagLog.log("PiP=$isInPictureInPictureMode")
        pipButton.visibility = if (isInPictureInPictureMode) View.GONE else View.VISIBLE
        // Cửa sổ PiP nhỏ: ẩn nút ☰ và đóng bảng nếu đang mở.
        menuButton.visibility = pipButton.visibility
        queueContainer.visibility =
            if (isInPictureInPictureMode || !displayOptions.showQueue) View.GONE else View.VISIBLE
        if (isInPictureInPictureMode) {
            quickPanel.hide()
            queuePanel.hide()
        }
    }

    override fun onDestroy() {
        setPlaying(false)
        unregisterReceiver(toggleReceiver)
        adEngine?.close()
        mediaSession.release()
        adEngine = null
        super.onDestroy()
    }

    /** Áp bộ lọc giao diện theo [displayOptions] vào trang YouTube (ticket #11, #12). */
    private fun applyDisplayFilter() {
        webView.evaluateJavascript(DisplayFilter.applyJs(displayOptions), null)
        // Hàng đợi tắt: ẩn nút thêm vào hàng đợi trên trang và đóng bảng; danh sách vẫn được giữ.
        webView.evaluateJavascript(queueFlagJs(displayOptions.showQueue), null)
        if (!displayOptions.showQueue) queuePanel.hide()
        refreshQueueUi()
    }

    /** Sau khi hàng đợi đổi: lưu, cập nhật nút và bảng đang mở. */
    private fun onQueueChanged() {
        getSharedPreferences(PREFS_QUEUE, MODE_PRIVATE).edit()
            .putString(KEY_QUEUE_ITEMS, QueueCodec.encode(queue.items))
            .apply()
        refreshQueueUi()
        if (queuePanel.isOpen) queuePanel.render()
    }

    private fun refreshQueueUi() {
        val count = queue.items.size
        queueButton.contentDescription = "Mở hàng đợi, $count mục"
        queueButton.tooltipText = "Hàng đợi ($count)"
        queueBadge.text = count.toString()
        queueBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
        queueContainer.visibility = if (displayOptions.showQueue) View.VISIBLE else View.GONE
    }

    /**
     * Nút chỉ có icon Phosphor theo kiểu icon tonal của Material 3, cỡ chạm 56dp.
     * Có nhãn đọc màn hình và tooltip (AGENTS.md, mục UI).
     */
    private fun iconButton(iconRes: Int, label: String, onClick: () -> Unit): MaterialButton {
        val density = resources.displayMetrics.density
        return MaterialButton(this, null, MaterialR.attr.materialIconButtonFilledTonalStyle).apply {
            setIconResource(iconRes)
            iconSize = (ICON_SIZE_DP * density).toInt()
            iconPadding = 0
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            text = null
            contentDescription = label
            tooltipText = label
            minimumWidth = (BUTTON_SIZE_DP * density).toInt()
            minimumHeight = (BUTTON_SIZE_DP * density).toInt()
            setOnClickListener { onClick() }
        }
    }

    /** Nhận mục từ nút trên trang xem video (ticket #13). Gọi từ luồng WebView. */
    private inner class QueueBridge {
        @JavascriptInterface
        fun add(videoId: String, title: String, next: Boolean) {
            runOnUiThread {
                if (!isValidVideoId(videoId)) return@runOnUiThread
                val item = QueueItem(videoId, title.trim().take(MAX_TITLE_CHARS))
                if (next) queue.playNext(item) else queue.append(item)
                onQueueChanged()
                Toast.makeText(
                    this@MainActivity,
                    if (next) "Sẽ phát tiếp theo" else "Đã thêm vào hàng đợi",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Trang chính lỗi. Mất mạng: giữ vị trí (tua lại theo lời nhắc đã lưu nếu đúng video) và hàng đợi,
     * trang lỗi có nút thử lại tới đúng URL vừa lỗi. Lỗi chưa rõ: cũng giữ trạng thái và cho thử lại.
     */
    private fun onMainFrameFailed(failedUrl: String, network: Boolean) {
        val retryUrl = if (failedUrl.startsWith("https://") || failedUrl.startsWith("http://")) failedUrl else START_URL
        val failedId = Uri.parse(retryUrl).getQueryParameter("v")
        resumeStore.load()?.takeIf { it.videoId == failedId }?.let { pendingResume = it }
        webView.loadDataWithBaseURL(null, offlineHtml(retryUrl, network), "text/html", "UTF-8", null)
    }

    /** Đánh dấu mục hàng đợi đã tải xong: lúc này mới bỏ khỏi hàng đợi (ticket #17). */
    private fun completeQueueLoad(url: String) {
        val loadingId = queueLoadingId ?: return
        val uri = Uri.parse(url)
        if (uri.path != "/watch" || uri.getQueryParameter("v") != loadingId) return
        queueLoadingId = null
        if (queue.peekHead()?.videoId == loadingId) {
            queue.pollHead()
            onQueueChanged()
        }
    }

    /** Video không truy cập được (ticket #17): thông báo ngắn rồi bỏ qua hoặc về trang chủ. Gọi từ luồng WebView. */
    private fun handleUnavailable(videoId: String) {
        if (!isValidVideoId(videoId)) return
        val isResume = resumeStore.load()?.videoId == videoId || pendingResume?.videoId == videoId
        when (PlaybackErrorRules.decideOnUnavailable(isResume, displayOptions.showQueue, !queue.isEmpty)) {
            UnavailableAction.GO_HOME -> {
                resumeStore.clearIf(videoId)
                pendingResume = null
                Toast.makeText(this, "Video đang xem dở không khả dụng, về trang chủ", Toast.LENGTH_SHORT).show()
                webView.loadUrl(START_URL)
            }
            UnavailableAction.SKIP_TO_NEXT -> {
                Toast.makeText(this, "Video không khả dụng, chuyển mục tiếp theo", Toast.LENGTH_SHORT).show()
                playNextFromQueue()
            }
            UnavailableAction.STAY -> {
                Toast.makeText(this, "Video không khả dụng", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Thanh nhắc "Xem dở: tên video" với nút Tiếp tục và Bỏ qua (ticket #16). */
    private fun showResumeBanner(point: ResumePoint) {
        if (resumeBanner != null) return
        val density = resources.displayMetrics.density
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((12 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())
        }
        val label = TextView(this).apply {
            text = "Xem dở: ${point.title} (${formatPosition(point.positionMs)})"
            TextViewCompat.setTextAppearance(this, MaterialR.style.TextAppearance_Material3_BodyMedium)
            setTextColor(MaterialColors.getColor(this, MaterialR.attr.colorOnSurface))
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        actions.addView(label, LinearLayout.LayoutParams(0, WRAP, 1f))
        // Nút có nhãn chữ để người dùng hiểu ngay, không chỉ dựa vào icon.
        actions.addView(MaterialButton(this, null, MaterialR.attr.materialButtonStyle).apply {
            text = "Bỏ qua"
            contentDescription = "Bỏ qua lời nhắc xem dở"
            setOnClickListener { hideResumeBanner() }
        })
        actions.addView(MaterialButton(this).apply {
            text = "Tiếp tục"
            setIconResource(R.drawable.ic_ph_play)
            iconSize = (ICON_SIZE_DP * density).toInt()
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            contentDescription = "Tiếp tục xem ${point.title}"
            setOnClickListener { resumeVideo(point) }
        })
        val card = MaterialCardView(this).apply {
            radius = 16 * density
            addView(actions)
        }
        root.addView(
            card,
            FrameLayout.LayoutParams(MATCH, WRAP, Gravity.TOP).apply {
                setMargins((16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt(), 0)
            }
        )
        resumeBanner = card
    }

    /** Định dạng vị trí cho người dùng đọc: m:ss, hoặc h:mm:ss nếu từ một giờ trở lên. */
    private fun formatPosition(positionMs: Long): String {
        val total = positionMs / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.ROOT, "%d:%02d", m, s)
    }

    private fun hideResumeBanner() {
        resumeBanner?.let { root.removeView(it) }
        resumeBanner = null
    }

    /** Mở đúng trang video; trang tải xong thì tua tới vị trí đã lưu và phát (xem seekPendingResume). */
    private fun resumeVideo(point: ResumePoint) {
        hideResumeBanner()
        pendingResume = point
        webView.loadUrl("${START_URL}watch?v=${point.videoId}")
    }

    /** Tua và phát tại vị trí đã lưu khi đúng trang watch của video đó vừa tải xong. */
    private fun seekPendingResume(view: WebView, url: String) {
        val point = pendingResume ?: return
        val uri = Uri.parse(url)
        if (uri.path != "/watch" || uri.getQueryParameter("v") != point.videoId) return
        pendingResume = null
        view.evaluateJavascript(
            String.format(Locale.ROOT, SEEK_JS_TEMPLATE, point.positionMs / 1000.0), null
        )
    }

    /** Nhận trạng thái phát và tiến độ từ trang. Gọi từ luồng WebView, nên chuyển sang luồng chính khi cần. */
    private inner class PlaybackBridge {
        @JavascriptInterface
        fun onState(playing: Boolean) {
            runOnUiThread { setPlaying(playing) }
        }

        /** Log chẩn đoán từ trang (DiagLog). Gọi từ luồng WebView. */
        @JavascriptInterface
        fun debugLog(message: String) {
            DiagLog.log("web: $message")
        }

        /** Ghi vị trí định kỳ khi đang phát (ticket #16). */
        @JavascriptInterface
        fun onProgress(videoId: String?, title: String?, positionSec: Double, durationSec: Double) {
            ResumeRules.toSave(videoId, title, positionSec, durationSec)?.let { resumeStore.save(it) }
        }

        /** Xem hết thì xóa lời nhắc của đúng video đó, không nhắc lại (ticket #16). */
        @JavascriptInterface
        fun onEnded(videoId: String?) {
            resumeStore.clearIf(videoId)
        }

        /** Video kết thúc khi hàng đợi bật (ticket #14). Chỉ được gọi khi trang đã chặn autoplay của YouTube. */
        @JavascriptInterface
        fun onQueueEnded() {
            runOnUiThread { playNextFromQueue() }
        }

        /**
         * Trình phát báo lỗi (ticket #17). [text] là thông báo nguyên văn; chỉ thông báo nội dung mới xử lý,
         * lỗi chưa rõ thì giữ trạng thái.
         */
        @JavascriptInterface
        fun onUnavailable(videoId: String?, text: String?) {
            if (videoId == null || !PlaybackErrorRules.isUnavailableText(text)) return
            runOnUiThread { handleUnavailable(videoId) }
        }
    }

    /**
     * Phát mục đầu hàng đợi sau khi video kết thúc hoặc bị bỏ qua; hết mục thì dừng (ticket #14).
     * Mục chỉ bị bỏ khỏi hàng đợi khi trang tải xong (completeQueueLoad), để mất mạng không làm mất mục.
     */
    private fun playNextFromQueue() {
        if (!displayOptions.showQueue) return
        val next = queue.peekHead() ?: return
        queueLoadingId = next.videoId
        webView.loadUrl("https://www.youtube.com/watch?v=${next.videoId}")
    }

    /** Bật/tắt service phát nền theo trạng thái phát (ticket #5). */
    private fun setPlaying(playing: Boolean) {
        val effects = PlaybackStateRules.effectsFor(wasPlaying = isPlaying, playing = playing)
        if (effects.isEmpty()) return
        isPlaying = playing
        Log.i(TAG_PLAYBACK, "state=$playing")
        effects.forEach { applyEffect(it) }
    }

    /** Thực hiện một hiệu ứng do [PlaybackStateRules] quyết định. */
    private fun applyEffect(effect: PlaybackEffect) {
        when (effect) {
            PlaybackEffect.HIDE_RESUME_BANNER -> hideResumeBanner()
            PlaybackEffect.START_SERVICE -> {
                // Phát lại trong vòng chờ thì hủy lệnh dừng; tránh dừng rồi bật lại service.
                handler.removeCallbacks(stopServiceRunnable)
                startForegroundService(Intent(this, PlaybackService::class.java))
            }
            PlaybackEffect.SCHEDULE_STOP -> handler.postDelayed(stopServiceRunnable, STOP_DELAY_MS)
            PlaybackEffect.REFRESH_PIP -> {
                // Cập nhật nút phát/tạm dừng trong PiP nếu đang ở PiP.
                if (isInPictureInPictureMode) setPictureInPictureParams(buildPipParams())
            }
            PlaybackEffect.UPDATE_MEDIA_SESSION -> updateMediaSession(isPlaying)
        }
    }

    /** Đăng ký lệnh từ MediaSession (nút next/previous trên vô lăng, điều khiển hệ thống) và chạy trên trang. */
    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "VF8tube")
        mediaSession.setCallback(object : MediaSession.Callback() {
            override fun onPlay() = runMediaCommand(MediaCommand.PLAY)
            override fun onPause() = runMediaCommand(MediaCommand.PAUSE)
            override fun onStop() = runMediaCommand(MediaCommand.PAUSE)
            override fun onSkipToNext() = skipToNext()
            override fun onSkipToPrevious() = runMediaCommand(MediaCommand.PREVIOUS)
        })
        mediaSession.isActive = true
        updateMediaSession(isPlaying)
    }

    private fun runMediaCommand(command: MediaCommand) {
        webView.evaluateJavascript(MediaSessionRules.scriptFor(command), null)
    }

    /**
     * Nút next: ưu tiên mục đầu hàng đợi (khi hàng đợi đang bật); không có thì bấm nút tiếp theo của YouTube.
     * Trang xem mobile không có nút tiếp theo khi không có danh sách phát, khi đó nút không làm gì.
     */
    private fun skipToNext() {
        if (displayOptions.showQueue && queue.peekHead() != null) playNextFromQueue()
        else runMediaCommand(MediaCommand.NEXT)
    }

    /** Báo trạng thái phát cho hệ thống; nút vô lăng chỉ được chuyển tới app khi phiên có trạng thái. */
    private fun updateMediaSession(playing: Boolean) {
        val actions = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_STOP or
            PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS
        val state = if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        mediaSession.setPlaybackState(
            PlaybackState.Builder()
                .setActions(actions)
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build()
        )
    }

    /** Android 13 trở lên: xin quyền thông báo để thông báo phát nền hiện được. */
    private fun requestNotificationPermission() {
        val permission = "android.permission.POST_NOTIFICATIONS"
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), REQUEST_NOTIFICATIONS)
        }
    }

    private fun enterPip() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return
        enterPictureInPictureMode(buildPipParams())
    }

    /** Tham số PiP: tỉ lệ 16:9 và một nút phát/tạm dừng khớp với trạng thái hiện tại. */
    private fun buildPipParams(): PictureInPictureParams {
        val label = if (isPlaying) "Tạm dừng" else "Phát"
        val iconRes = if (isPlaying) R.drawable.ic_ph_pause else R.drawable.ic_ph_play
        val pending = PendingIntent.getBroadcast(
            this, 0, Intent(ACTION_TOGGLE).setPackage(packageName), PendingIntent.FLAG_IMMUTABLE
        )
        val action = RemoteAction(Icon.createWithResource(this, iconRes), label, label, pending)
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(listOf(action))
            .build()
    }

    private fun hideCustomView() {
        val view = customView ?: return
        root.removeView(view)
        customView = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        webView.visibility = View.VISIBLE
        applyImmersiveMode()
    }

    /** Ẩn thanh trạng thái và thanh điều hướng để trang chiếm toàn màn hình. */
    @Suppress("DEPRECATION")
    private fun applyImmersiveMode() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }

    private companion object {
        const val START_URL = "https://www.youtube.com/"
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
        const val ACTION_TOGGLE = "com.meomuc.youtubeintablet.TOGGLE_PLAYBACK"
        const val TAG_AD_BLOCK = "AdBlock"
        const val TAG_KEY_LEARN = "KeyLearn"
        const val TAG_PLAYBACK = "Playback"
        const val STOP_DELAY_MS = 1000L
        const val PREFS_DISPLAY = "display_options"
        const val PREFS_QUEUE = "play_queue"
        const val KEY_QUEUE_ITEMS = "items"
        const val MAX_TITLE_CHARS = 200
        const val QUEUE_GAP_DP = 12
        const val REQUEST_NOTIFICATIONS = 1001

        /** Bật/tắt khả năng hàng đợi trên trang (ticket #13, #14). */
        fun queueFlagJs(on: Boolean) =
            "window.__ytTabletQueueOn=$on;if(window.__ytTabletUpdateQueueBar){window.__ytTabletUpdateQueueBar();}"

        /** Cỡ chạm tối thiểu của nút điều khiển (ticket #20). */
        const val BUTTON_SIZE_DP = 56
        const val ICON_SIZE_DP = 24
        const val BADGE_SIZE_DP = 20
        const val BADGE_PADDING_DP = 5
        const val MENU_MARGIN_DP = 24

        /** Đánh dấu app đang ở nền (true) hoặc hiển thị (false) để trang tự phát lại khi bị tạm dừng nền. */
        fun backgroundFlagJs(background: Boolean) = "window.__ytTabletBg=$background;"


        /** Tua tới vị trí đã lưu (giây) rồi phát; chờ tới khi có thẻ video. */
        const val SEEK_JS_TEMPLATE = "(function(p){var tries=0;function go(){var v=document.querySelector('video');" +
            "if(!v){if(++tries<100){setTimeout(go,200);}return;}" +
            "var seek=function(){try{v.currentTime=p;}catch(e){}v.play().catch(function(){});};" +
            "if(v.readyState>=1){seek();}else{v.addEventListener('loadedmetadata',seek,{once:true});}}" +
            "go();})(%.3f);"


        /** Trang lỗi chính; [retryUrl] là URL vừa lỗi để "thử lại" quay về đúng mục (ticket #17). */
        fun offlineHtml(retryUrl: String, network: Boolean): String {
            val title = if (network) "Không có kết nối mạng" else "Không tải được trang"
            val hint = if (network) "Kiểm tra Wi-Fi hoặc dữ liệu di động" else "Trang gặp lỗi không rõ nguyên nhân"
            val href = TextUtils.htmlEncode(retryUrl)
            return """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body style="font-family:sans-serif;text-align:center;padding:48px;color:#222">
<h2>$title</h2>
<p>$hint, rồi <a href="$href">thử lại</a>.</p>
</body></html>"""
        }
    }
}
