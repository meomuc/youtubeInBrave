package com.meomuc.youtubeintablet

/**
 * Bốn mục của bảng hiển thị nhanh (ticket #10). Mặc định theo thiết kế đã duyệt:
 * Shorts tắt; Hàng đợi, Bình luận, Video đề xuất bật.
 *
 * Lớp này chỉ giữ và đọc/ghi lựa chọn. Việc lọc giao diện do các ticket sau làm.
 */
data class DisplayOptions(
    val showShorts: Boolean = false,
    val showQueue: Boolean = true,
    val showComments: Boolean = true,
    val showRelated: Boolean = true,
) {
    /** Ghi từng mục qua [put]; khóa cố định để không lệch khi lưu và đọc lại. */
    fun writeTo(put: (key: String, value: Boolean) -> Unit) {
        put(KEY_SHORTS, showShorts)
        put(KEY_QUEUE, showQueue)
        put(KEY_COMMENTS, showComments)
        put(KEY_RELATED, showRelated)
    }

    companion object {
        private const val KEY_SHORTS = "show_shorts"
        private const val KEY_QUEUE = "show_queue"
        private const val KEY_COMMENTS = "show_comments"
        private const val KEY_RELATED = "show_related"

        /** Đọc lựa chọn; khóa chưa có thì dùng mặc định của [DisplayOptions]. */
        fun fromStore(get: (key: String, default: Boolean) -> Boolean): DisplayOptions {
            val d = DisplayOptions()
            return DisplayOptions(
                showShorts = get(KEY_SHORTS, d.showShorts),
                showQueue = get(KEY_QUEUE, d.showQueue),
                showComments = get(KEY_COMMENTS, d.showComments),
                showRelated = get(KEY_RELATED, d.showRelated),
            )
        }
    }
}
