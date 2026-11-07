package com.meomuc.youtubeintablet

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

/**
 * Ticket #3: mở youtube.com toàn màn hình trong WebView.
 * Back quay lại trang trước trong lịch sử WebView; hết lịch sử thì thoát app.
 */
class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var root: FrameLayout

    /** View video khi YouTube bấm nút toàn màn hình (null nếu không ở chế độ đó). */
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

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
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // Chỉ tải web trong WebView. Scheme lạ (intent://, market://) bị bỏ qua
                // thay vì hiện trang lỗi ERR_UNKNOWN_URL_SCHEME.
                val scheme = request.url.scheme
                return scheme != "http" && scheme != "https"
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
        setContentView(root)
        applyImmersiveMode()

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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Hệ thống có thể hiện lại thanh trạng thái sau khi người dùng chạm vào cạnh màn hình.
        if (hasFocus) applyImmersiveMode()
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
    }
}
