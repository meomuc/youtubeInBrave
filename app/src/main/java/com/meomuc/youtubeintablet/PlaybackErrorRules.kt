package com.meomuc.youtubeintablet

/** Việc cần làm khi một video không truy cập được (ticket #17). */
enum class UnavailableAction {
    /** Hàng đợi bật và còn mục: thông báo ngắn rồi phát mục tiếp theo. */
    SKIP_TO_NEXT,

    /** Video đang là lời nhắc tiếp tục xem: thông báo rồi về trang chủ. */
    GO_HOME,

    /** Không có mục kế tiếp: chỉ thông báo, giữ trang hiện tại. */
    STAY,
}

/**
 * Quy tắc thuần cho lỗi phát video (ticket #17). Không phụ thuộc Android nên kiểm thử được bằng unit test.
 * Phân biệt hai loại: lỗi nội dung (video xóa, riêng tư, không có sẵn) và lỗi kết nối (mất mạng).
 * Lỗi chưa rõ loại thì không làm gì, để người dùng giữ trạng thái và thử lại.
 */
object PlaybackErrorRules {

    /**
     * Nhận diện thông báo lỗi của trình phát YouTube là lỗi nội dung. Các cụm này là giả định,
     * chưa kiểm chứng trên YouTube thật; cần bổ sung khi gặp thông báo mới.
     */
    private val UNAVAILABLE_MARKERS = listOf(
        "video unavailable",
        "video is private",
        "video không khả dụng",
        "video riêng tư",
        "video không có sẵn",
    )

    fun isUnavailableText(text: String?): Boolean {
        val normalized = text?.trim()?.lowercase() ?: return false
        if (normalized.isEmpty()) return false
        return UNAVAILABLE_MARKERS.any { normalized.contains(it) }
    }

    /**
     * Mã lỗi WebViewClient thuộc nhóm kết nối: HOST_LOOKUP (-2), CONNECT (-6), IO (-7), TIMEOUT (-8).
     * Các mã khác coi là lỗi chưa rõ.
     */
    fun isNetworkError(errorCode: Int): Boolean = errorCode in NETWORK_ERROR_CODES

    private val NETWORK_ERROR_CODES = setOf(-2, -6, -7, -8)

    /**
     * Quyết định sau khi xác định video là lỗi nội dung. [isResumeVideo] là video đang nằm trong lời nhắc xem dở.
     */
    fun decideOnUnavailable(isResumeVideo: Boolean, queueOn: Boolean, queueHasItems: Boolean): UnavailableAction = when {
        isResumeVideo -> UnavailableAction.GO_HOME
        queueOn && queueHasItems -> UnavailableAction.SKIP_TO_NEXT
        else -> UnavailableAction.STAY
    }
}
