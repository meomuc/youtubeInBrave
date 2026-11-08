package com.meomuc.youtubeintablet

import kotlin.math.min
import kotlin.math.roundToLong

/** Vị trí xem dở của một video, lưu cục bộ trên máy (ticket #16). */
data class ResumePoint(
    val videoId: String,
    val title: String,
    val positionMs: Long,
)

/** Quy tắc thuần cho lời nhắc tiếp tục xem; không phụ thuộc Android nên kiểm thử được bằng unit test. */
object ResumeRules {
    private val VIDEO_ID_PATTERN = Regex("^[A-Za-z0-9_-]{11}$")

    /** Dưới ngưỡng này không nhắc: xem vài giây đầu không đáng để hỏi. */
    const val MIN_PROMPT_MS = 5_000L
    const val MAX_TITLE_LENGTH = 200

    fun isValidVideoId(videoId: String?): Boolean = videoId != null && VIDEO_ID_PATTERN.matches(videoId)

    /**
     * Chuẩn hóa dữ liệu báo từ trang trước khi lưu; trả null nếu không nên lưu.
     * Livestream (duration vô hạn) và video chưa rõ thời lượng không lưu vị trí theo giây.
     */
    fun toSave(videoId: String?, title: String?, positionSec: Double, durationSec: Double): ResumePoint? {
        if (videoId == null || !isValidVideoId(videoId)) return null
        if (!positionSec.isFinite() || positionSec < 0.0) return null
        if (!durationSec.isFinite() || durationSec <= 0.0) return null
        val cleanTitle = title?.trim()?.take(MAX_TITLE_LENGTH)?.takeIf { it.isNotEmpty() } ?: videoId
        val positionMs = (min(positionSec, durationSec) * 1000).roundToLong()
        return ResumePoint(videoId, cleanTitle, positionMs)
    }

    fun shouldPrompt(point: ResumePoint?): Boolean = point != null && point.positionMs >= MIN_PROMPT_MS

    /** Chuẩn hóa dữ liệu đọc lại từ bộ nhớ; trả null nếu thiếu hoặc hỏng. */
    fun sanitizeLoaded(videoId: String?, title: String?, positionMs: Long): ResumePoint? {
        if (videoId == null || title == null || !isValidVideoId(videoId) || positionMs < 0) return null
        return ResumePoint(videoId, title, positionMs)
    }

    /**
     * Chỉ xóa lời nhắc khi đúng video đang lưu theo id; video khác kết thúc thì giữ nguyên.
     * Dùng id thô trong bộ nhớ, không yêu cầu title hợp lệ, để giữ hành vi cũ.
     */
    fun shouldClear(storedVideoId: String?, videoId: String?): Boolean =
        storedVideoId != null && videoId != null && storedVideoId == videoId
}
