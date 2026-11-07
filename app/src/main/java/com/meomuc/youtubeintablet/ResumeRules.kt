package com.meomuc.youtubeintablet

import android.content.Context
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
}

/** Lưu một vị trí xem dở bằng SharedPreferences cục bộ (không dùng tài khoản). */
class ResumeStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): ResumePoint? {
        return try {
            val id = prefs.getString(KEY_VIDEO_ID, null)
            val title = prefs.getString(KEY_TITLE, null)
            val positionMs = prefs.getLong(KEY_POSITION_MS, -1L)
            if (id == null || title == null || !ResumeRules.isValidVideoId(id) || positionMs < 0) null
            else ResumePoint(id, title, positionMs)
        } catch (e: RuntimeException) {
            null
        }
    }

    fun save(point: ResumePoint) {
        prefs.edit()
            .putString(KEY_VIDEO_ID, point.videoId)
            .putString(KEY_TITLE, point.title)
            .putLong(KEY_POSITION_MS, point.positionMs)
            .apply()
    }

    /** Xóa lời nhắc khi đúng video đó đã xem hết; video khác kết thúc thì giữ nguyên. */
    fun clearIf(videoId: String?) {
        if (videoId != null && prefs.getString(KEY_VIDEO_ID, null) == videoId) {
            prefs.edit().clear().apply()
        }
    }

    private companion object {
        const val PREFS_NAME = "resume"
        const val KEY_VIDEO_ID = "video_id"
        const val KEY_TITLE = "title"
        const val KEY_POSITION_MS = "position_ms"
    }
}
