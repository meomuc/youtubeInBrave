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
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
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
import android.widget.LinearLayout
import android.widget.TextView
import java.io.ByteArrayInputStream
import java.util.Locale

/**
 * Ticket #3: mở youtube.com toàn màn hình trong WebView, Back theo lịch sử.
 * Ticket #4: đăng nhập Google (UA không có dấu hiệu WebView).
 * Ticket #5: phát nền qua PlaybackService khi video đang phát.
 * Ticket #6: PiP khi bấm Home lúc đang phát, và nút PiP trong app.
 * Ticket #7: xoay màn hình không tạo lại Activity; báo lỗi mất mạng.
 * Ticket #8: chặn request quảng cáo theo AdBlocker.
 * Ticket #16: lưu vị trí xem dở và nhắc tiếp tục khi mở app (không tự phát).
 * Ticket #18: mất audio focus (cuộc gọi, app khác) thì tạm dừng; không tự phát lại.
 */
class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var root: FrameLayout
    private lateinit var pipButton: Button
    private lateinit var resumeStore: ResumeStore

    /** Giữ audio focus khi đang phát; mất focus (cuộc gọi, app khác) thì tạm dừng (ticket #18). */
    private lateinit var audioFocus: PlaybackAudioFocus

    /** Thanh nhắc tiếp tục xem; null khi không hiện. */
    private var resumeBanner: View? = null

    /** Video vừa bấm "Tiếp tục": cần tua tới vị trí này khi trang watch tải xong. */
    @Volatile private var pendingResume: ResumePoint? = null
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
        resumeStore = ResumeStore(this)
        audioFocus = PlaybackAudioFocus(this) { webView.evaluateJavascript(PAUSE_JS, null) }
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
                    seekPendingResume(view, url)
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
    }

    override fun onDestroy() {
        setPlaying(false)
        unregisterReceiver(toggleReceiver)
        adEngine?.close()
        adEngine = null
        super.onDestroy()
    }

    /** Trang thông báo mất mạng; liên kết "thử lại" tải lại youtube.com. */
    private fun showOfflinePage() {
        webView.loadDataWithBaseURL(null, OFFLINE_HTML, "text/html", "UTF-8", null)
    }

    /** Thanh nhắc "Xem dở: tên video" với nút Tiếp tục và Bỏ qua (ticket #16). */
    private fun showResumeBanner(point: ResumePoint) {
        if (resumeBanner != null) return
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(BANNER_BG)
            setPadding(BANNER_PADDING, BANNER_PADDING / 2, BANNER_PADDING, BANNER_PADDING / 2)
        }
        val label = TextView(this).apply {
            text = "Xem dở: ${point.title} (${formatPosition(point.positionMs)})"
            setTextColor(Color.WHITE)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        bar.addView(label, LinearLayout.LayoutParams(0, WRAP, 1f))
        bar.addView(Button(this).apply {
            text = "Tiếp tục"
            setOnClickListener { resumeVideo(point) }
        })
        bar.addView(Button(this).apply {
            text = "Bỏ qua"
            setOnClickListener { hideResumeBanner() }
        })
        root.addView(bar, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.TOP))
        resumeBanner = bar
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
    }

    /** Bật/tắt service phát nền theo trạng thái phát (ticket #5). */
    private fun setPlaying(playing: Boolean) {
        if (playing == isPlaying) return
        isPlaying = playing
        if (playing) hideResumeBanner()
        Log.i(TAG_PLAYBACK, "state=$playing")
        if (playing) audioFocus.acquire() else audioFocus.release()
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
  // Ticket #16: ghi vị trí mỗi 5 giây khi đang phát, và ngay khi tạm dừng hoặc kết thúc.
  function currentVideoId(){
    try { return new URL(location.href).searchParams.get('v'); } catch (e) { return null; }
  }
  function pageTitle(){
    return document.title.replace(/^\(\d+\)\s*/, '').replace(/\s*-\s*YouTube$/, '');
  }
  function saveProgress(){
    var v = document.querySelector('video');
    if (!v || location.pathname !== '/watch') return;
    var id = currentVideoId();
    if (v.ended) { AndroidPlayback.onEnded(id); return; }
    AndroidPlayback.onProgress(id, pageTitle(), v.currentTime, v.duration);
  }
  setInterval(function(){
    var v = document.querySelector('video');
    if (v && !v.paused && !v.ended) saveProgress();
  }, 5000);
  ['pause', 'ended'].forEach(function(e){ document.addEventListener(e, saveProgress, true); });
})();"""

        /**
         * Tạm dừng khi mất audio focus (ticket #18). Đặt cờ người dùng tạm dừng trước để hook nền
         * không tự phát lại; sau cuộc gọi video chỉ phát khi người dùng bấm Play.
         */
        const val PAUSE_JS =
            "(function(){var v=document.querySelector('video');if(v&&!v.paused){window.__ytTabletUserPaused=true;v.pause();}})();"

        /** Tua tới vị trí đã lưu (giây) rồi phát; chờ tới khi có thẻ video. */
        const val SEEK_JS_TEMPLATE = "(function(p){var tries=0;function go(){var v=document.querySelector('video');" +
            "if(!v){if(++tries<100){setTimeout(go,200);}return;}" +
            "var seek=function(){try{v.currentTime=p;}catch(e){}v.play().catch(function(){});};" +
            "if(v.readyState>=1){seek();}else{v.addEventListener('loadedmetadata',seek,{once:true});}}" +
            "go();})(%.3f);"

        val BANNER_BG: Int = 0xEE222222.toInt()
        const val BANNER_PADDING = 32

        const val OFFLINE_HTML = """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body style="font-family:sans-serif;text-align:center;padding:48px;color:#222">
<h2>Không có kết nối mạng</h2>
<p>Kiểm tra Wi-Fi hoặc dữ liệu di động, rồi <a href="https://www.youtube.com/">thử lại</a>.</p>
</body></html>"""
    }
}
