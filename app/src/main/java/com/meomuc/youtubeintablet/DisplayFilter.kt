package com.meomuc.youtubeintablet

/**
 * Sinh CSS ẩn các phần tử theo [DisplayOptions] và đưa vào trang YouTube (ticket #11, #12).
 *
 * Chỉ ẩn bằng CSS, không xóa dữ liệu YouTube. Các selector là giả định theo cấu trúc
 * hiện tại của youtube.com, CHƯA kiểm chứng trên thiết bị: cần kiểm tra trên emulator.
 */
object DisplayFilter {

    /** id của thẻ style do app tạo; dùng để cập nhật thay vì thêm thẻ mới mỗi lần. */
    const val STYLE_ID = "yt-tablet-filter"

    /**
     * Các mục Shorts ở trang chủ, tìm kiếm, lịch sử và thanh bên.
     * Nhóm đầu: giao diện m.youtube.com (đã kiểm chứng trên DOM qua DevTools):
     * mục Shorts ở thanh dưới có class pivot-shorts; kệ Shorts là ytm-rich-section-renderer
     * chứa ytm-shorts-lockup-view-model (kệ đã kiểm tra chỉ chứa thẻ Shorts).
     * Nhóm sau: giao diện desktop (ytd-*), giữ để dự phòng.
     */
    private val SHORTS_SELECTORS = listOf(
        "ytm-pivot-bar-item-renderer:has(.pivot-shorts)",
        "ytm-rich-section-renderer:has(ytm-shorts-lockup-view-model)",
        "ytm-shorts-lockup-view-model",
        "ytd-reel-shelf-renderer",
        "ytd-rich-shelf-renderer[is-shorts]",
        "ytd-reel-item-renderer",
        "ytd-rich-item-renderer:has(a[href^=\"/shorts/\"])",
        "ytd-video-renderer:has(a[href^=\"/shorts/\"])",
        "ytd-compact-video-renderer:has(a[href^=\"/shorts/\"])",
        "ytd-guide-entry-renderer:has(a[href=\"/shorts\"])",
        "ytd-mini-guide-entry-renderer[aria-label=\"Shorts\"]",
        "a[href^=\"/shorts\"]",
    )

    /** Khu bình luận dưới video (chỉ trên trang xem video). */
    private val COMMENTS_SELECTORS = listOf(
        "ytd-watch-flexy #comments",
        "ytd-engagement-panel-section-list-renderer[target-id=\"engagement-panel-comments-section\"]",
    )

    /**
     * Đề xuất cạnh/dưới trình phát. Chỉ nhắm vào trang xem video, không nhắm lưới trang chủ,
     * nên trang chủ vẫn hiện video đề xuất khi mục này tắt.
     */
    private val RELATED_SELECTORS = listOf(
        "ytd-watch-flexy #related",
        "ytd-watch-flexy ytd-watch-next-secondary-results-renderer",
    )

    /** Danh sách selector cần ẩn theo lựa chọn hiện tại. */
    private fun hiddenSelectors(options: DisplayOptions): List<String> = buildList {
        if (!options.showShorts) addAll(SHORTS_SELECTORS)
        if (!options.showComments) addAll(COMMENTS_SELECTORS)
        if (!options.showRelated) addAll(RELATED_SELECTORS)
    }

    /** CSS đầy đủ: mỗi selector một luật display:none. Rỗng nếu không cần ẩn gì. */
    fun css(options: DisplayOptions): String =
        hiddenSelectors(options).joinToString("\n") { "$it{display:none!important}" }

    /**
     * Đoạn JavaScript cập nhật thẻ style của app với CSS hiện tại.
     * Chuỗi CSS được escape để nhúng an toàn vào JavaScript.
     */
    fun applyJs(options: DisplayOptions): String {
        val literal = "\"" + css(options)
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n") + "\""
        return "(function(){" +
            "var s=document.getElementById('$STYLE_ID');" +
            "if(!s){s=document.createElement('style');s.id='$STYLE_ID';" +
            "(document.head||document.documentElement).appendChild(s);}" +
            "s.textContent=$literal;})();"
    }
}
