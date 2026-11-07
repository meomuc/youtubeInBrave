package com.meomuc.youtubeintablet

import java.net.URI

/**
 * Suy ra loại request theo quy ước của adblock-rust (ADR 0004).
 * WebView trên API 26 không cho biết loại request, nên suy ra từ đuôi đường dẫn và header Accept.
 */
object RequestTypeMapper {

    fun typeOf(url: String, accept: String?): String {
        val path = try {
            URI(url).path?.lowercase().orEmpty()
        } catch (e: java.net.URISyntaxException) {
            ""
        }
        val a = accept?.lowercase().orEmpty()
        return when {
            path.endsWith(".js") -> "script"
            path.endsWith(".woff") || path.endsWith(".woff2") || path.endsWith(".ttf") -> "font"
            a.contains("text/css") -> "stylesheet"
            a.contains("image/") -> "image"
            a.contains("video/") || a.contains("audio/") -> "media"
            a.contains("json") -> "xmlhttprequest"
            else -> "other"
        }
    }
}
