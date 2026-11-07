package com.meomuc.youtubeintablet

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.view.Gravity
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
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import java.io.ByteArrayInputStream

/**
 * Ticket #3: mở youtube.com toàn màn hình trong WebView, Back theo lịch sử.
 * Ticket #4: đăng nhập Google (UA không có dấu hiệu WebView).
 * Ticket #5: phát nền qua PlaybackService khi video đang phát.
 * Ticket #6: PiP khi bấm Home lúc đang phát, và nút PiP trong app.
 * Ticket #7: xoay màn hình không tạo lại Activity; báo lỗi mất mạng.
 * Ticket #8: chặn request quảng cáo theo AdBlocker.
 * Ticket #10: nút ☰ mở bảng hiển thị nhanh (4 mục bật/tắt, lưu lại).
 * Ticket #11, #12: ẩn Shorts, bình luận, video đề xuất theo lựa chọn (DisplayFilter).
 */
class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var root: FrameLayout
    private lateinit var pipButton: Button
    /** Nút ☰ góc dưới bên trái mở bảng hiển thị nhanh (ticket #10). */
    private lateinit var menuButton: ImageButton
    private lateinit var quickPanel: QuickPanel
    /** Lựa chọn hiển thị hiện tại; đọc từ SharedPreferences khi mở app (ticket #10). */
    private var displayOptions = DisplayOptions()
    /** Engine chặn quảng cáo (ADR 0004); null cho tới khi nạp xong danh sách lọc. */
    @Volatile private var adEngine: AdEngine? = null

    /** URL trang đang mở, dùng làm sourceUrl khi kiểm tra request. */
    @Volatile private var currentPageUrl: String = ""

    private val handler = Handler(Looper.getMainLooper())
    private val stopServiceRunnable = Runnable {
        stopService(Intent(this, PlaybackService::class.java))
    }

    /** View video khi YouTube bấm nút toàn màn hình (null nếu không ở chế độ đó). */
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    /** Trạng thái phát do trang báo qua cầu JavaScript; dùng cho service và PiP. */
    private var isPlaying = false

    private val toggleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            webView.evaluateJavascript(TOGGLE_JS, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        webView.addJavascriptInterface(PlaybackBridge(), "AndroidPlayback")
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
                    view.evaluateJavascript(PLAYBACK_HOOK_JS, null)
                    applyDisplayFilter()
                }
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                // Chỉ báo lỗi khi chính trang chính thất bại; lỗi ảnh/script thì để trang tự xử lý.
                if (request.isForMainFrame) showOfflinePage()
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
        pipButton = Button(this).apply {
            text = "PiP"
            setOnClickListener { enterPip() }
        }
        root.addView(
            pipButton,
            FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM or Gravity.END).apply {
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
        menuButton = ImageButton(this).apply {
            setImageResource(R.drawable.ic_menu)
            setBackgroundColor(MENU_BG_COLOR)
            contentDescription = "Mở bảng hiển thị nhanh"
            tooltipText = "Hiển thị nhanh"
            setOnClickListener { if (quickPanel.isOpen) quickPanel.hide() else quickPanel.show() }
        }
        val density = resources.displayMetrics.density
        val menuSize = (MENU_SIZE_DP * density).toInt()
        val menuMargin = (MENU_MARGIN_DP * density).toInt()
        root.addView(
            menuButton,
            FrameLayout.LayoutParams(menuSize, menuSize, Gravity.BOTTOM or Gravity.START).apply {
                setMargins(menuMargin, 0, 0, menuMargin)
            }
        )
        // Thêm sau cùng để bảng nằm trên nút ☰ và lớp mờ.
        root.addView(quickPanel, FrameLayout.LayoutParams(MATCH, MATCH))
        setContentView(root)
        applyImmersiveMode()

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
            webView.canGoBack() -> webView.goBack()
            else -> super.onBackPressed()
        }
    }

    /** Bấm Home hoặc chuyển app khi đang phát thì vào PiP (ticket #6). */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isPlaying) enterPip()
    }

    override fun onPause() {
        super.onPause()
        webView.evaluateJavascript(backgroundFlagJs(true), null)
    }

    override fun onResume() {
        super.onResume()
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
        pipButton.visibility = if (isInPictureInPictureMode) View.GONE else View.VISIBLE
        // Cửa sổ PiP nhỏ: ẩn nút ☰ và đóng bảng nếu đang mở.
        menuButton.visibility = pipButton.visibility
        if (isInPictureInPictureMode) quickPanel.hide()
    }

    override fun onDestroy() {
        setPlaying(false)
        unregisterReceiver(toggleReceiver)
        adEngine?.close()
        adEngine = null
        super.onDestroy()
    }

    /** Áp bộ lọc giao diện theo [displayOptions] vào trang YouTube (ticket #11, #12). */
    private fun applyDisplayFilter() {
        webView.evaluateJavascript(DisplayFilter.applyJs(displayOptions), null)
    }

    /** Trang thông báo mất mạng; liên kết "thử lại" tải lại youtube.com. */
    private fun showOfflinePage() {
        webView.loadDataWithBaseURL(null, OFFLINE_HTML, "text/html", "UTF-8", null)
    }

    /** Nhận trạng thái phát từ trang. Gọi từ luồng WebView, nên chuyển sang luồng chính. */
    private inner class PlaybackBridge {
        @JavascriptInterface
        fun onState(playing: Boolean) {
            runOnUiThread { setPlaying(playing) }
        }
    }

    /** Bật/tắt service phát nền theo trạng thái phát (ticket #5). */
    private fun setPlaying(playing: Boolean) {
        if (playing == isPlaying) return
        isPlaying = playing
        Log.i(TAG_PLAYBACK, "state=$playing")
        val serviceIntent = Intent(this, PlaybackService::class.java)
        if (playing) {
            // Phát lại trong vòng chờ thì hủy lệnh dừng; tránh dừng rồi bật lại service.
            handler.removeCallbacks(stopServiceRunnable)
            startForegroundService(serviceIntent)
        } else {
            handler.postDelayed(stopServiceRunnable, STOP_DELAY_MS)
        }
        // Cập nhật nút phát/tạm dừng trong PiP nếu đang ở PiP.
        if (isInPictureInPictureMode) setPictureInPictureParams(buildPipParams())
    }

    private fun enterPip() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return
        enterPictureInPictureMode(buildPipParams())
    }

    /** Tham số PiP: tỉ lệ 16:9 và một nút phát/tạm dừng khớp với trạng thái hiện tại. */
    private fun buildPipParams(): PictureInPictureParams {
        val label = if (isPlaying) "Tạm dừng" else "Phát"
        val iconRes = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
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
        const val TAG_PLAYBACK = "Playback"
        const val STOP_DELAY_MS = 1000L
        const val PREFS_DISPLAY = "display_options"
        const val MENU_SIZE_DP = 56
        const val MENU_MARGIN_DP = 24
        // Màu tạm, chờ design-system/MASTER.md (chưa có trong repo).
        const val MENU_BG_COLOR = 0xFFF2F2F2.toInt()

        /** Bấm phát/tạm dừng video đầu tiên trên trang (dùng cho nút PiP). */
        const val TOGGLE_JS =
            "(function(){var v=document.querySelector('video');if(!v){return;}" +
                "if(v.paused){window.__ytTabletUserPaused=false;v.play();}" +
                "else{window.__ytTabletUserPaused=true;v.pause();}})();"

        /** Đánh dấu app đang ở nền (true) hoặc hiển thị (false) để trang tự phát lại khi bị tạm dừng nền. */
        fun backgroundFlagJs(background: Boolean) = "window.__ytTabletBg=$background;"

        /** Báo trạng thái phát về app qua AndroidPlayback. Hook một lần cho cả phiên SPA. */
        const val PLAYBACK_HOOK_JS = """(function(){
  if (window.__ytTabletHooked) return;
  window.__ytTabletHooked = true;
  // YouTube tự tạm dừng khi trang bị ẩn. Giả lập trang luôn hiển thị để video tiếp tục khi xuống nền.
  try {
    Object.defineProperty(document, 'hidden', {configurable: true, get: function(){ return false; }});
    Object.defineProperty(document, 'visibilityState', {configurable: true, get: function(){ return 'visible'; }});
  } catch (e) {}
  document.addEventListener('visibilitychange', function(e){ e.stopImmediatePropagation(); }, true);
  // Khi app xuống nền, WebView có thể tự tạm dừng video. Nếu không phải do người dùng bấm tạm dừng
  // (cờ __ytTabletUserPaused từ nút PiP), phát lại ngay.
  document.addEventListener('pause', function(e){
    var v = e.target;
    if (window.__ytTabletBg && !window.__ytTabletUserPaused && v && v.tagName === 'VIDEO') {
      v.play().catch(function(){});
    }
  }, true);
  function report(){
    var v = document.querySelector('video');
    var playing = !!(v && !v.paused && !v.ended);
    // Đang phát thì xóa cờ "người dùng tạm dừng" để lần sau tự phát lại ở nền vẫn hoạt động.
    if (playing) window.__ytTabletUserPaused = false;
    // Hệ thống tạm dừng khi xuống nền: không báo, để trang tự phát lại (tránh dừng rồi bật lại service).
    if (!playing && window.__ytTabletBg && !window.__ytTabletUserPaused) return;
    AndroidPlayback.onState(playing);
  }
  ['play', 'playing', 'pause', 'ended'].forEach(function(e){ document.addEventListener(e, report, true); });
})();"""

        const val OFFLINE_HTML = """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body style="font-family:sans-serif;text-align:center;padding:48px;color:#222">
<h2>Không có kết nối mạng</h2>
<p>Kiểm tra Wi-Fi hoặc dữ liệu di động, rồi <a href="https://www.youtube.com/">thử lại</a>.</p>
</body></html>"""
    }
}
