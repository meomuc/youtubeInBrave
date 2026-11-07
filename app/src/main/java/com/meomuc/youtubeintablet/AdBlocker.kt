package com.meomuc.youtubeintablet

import java.net.URI

/**
 * Danh sách chặn request theo tên miền (ticket #8, ADR 0003).
 * Chặn cả tên miền con. Chỉ chặn tên miền quảng cáo đã biết; không chặn
 * googlevideo.com hay youtube.com vì đó là nơi phát video và trang chính.
 */
object AdBlocker {

    private val BLOCKED_DOMAINS = setOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "google-analytics.com",
        "imasdk.googleapis.com",
    )

    fun isBlocked(url: String): Boolean {
        val host = try {
            URI(url).host?.lowercase()
        } catch (e: java.net.URISyntaxException) {
            null
        } ?: return false
        return BLOCKED_DOMAINS.any { host == it || host.endsWith(".$it") }
    }
}
