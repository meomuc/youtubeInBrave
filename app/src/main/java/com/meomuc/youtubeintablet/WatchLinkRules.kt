package com.meomuc.youtubeintablet

/**
 * Quy tắc đọc link video trên trang YouTube (ticket #19). Thuần Kotlin để kiểm thử bằng unit test.
 * [HREF_PATTERN_SOURCE] và [WATCH_PATH] được nhúng nguyên văn vào JavaScript ([QueueWebScript]),
 * nên hai bên luôn dùng cùng một quy tắc.
 */
object WatchLinkRules {

    const val WATCH_PATH = "/watch"

    /** Tham số `v` đúng 11 ký tự, không dính thêm ký tự id sau đó. */
    const val HREF_PATTERN_SOURCE = "[?&]v=([A-Za-z0-9_-]{11})(?![A-Za-z0-9_-])"

    private val HREF_PATTERN = Regex(HREF_PATTERN_SOURCE)

    /** Trả về id video trong href dạng `/watch?v=...` (hoặc URL tuyệt đối); null nếu không đúng dạng. */
    fun videoIdFromHref(href: String): String? =
        HREF_PATTERN.find(href)?.groupValues?.get(1)
}
