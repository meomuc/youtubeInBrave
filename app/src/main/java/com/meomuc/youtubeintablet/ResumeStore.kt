package com.meomuc.youtubeintablet

import android.content.Context

/** Lưu một vị trí xem dở bằng SharedPreferences cục bộ (không dùng tài khoản). */
class ResumeStore(context: Context) : ResumeRepository {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): ResumePoint? {
        return try {
            ResumeRules.sanitizeLoaded(
                StoredResume(
                    videoId = prefs.getString(KEY_VIDEO_ID, null),
                    title = prefs.getString(KEY_TITLE, null),
                    positionMs = prefs.getLong(KEY_POSITION_MS, -1L),
                )
            )
        } catch (e: RuntimeException) {
            null
        }
    }

    override fun save(point: ResumePoint) {
        prefs.edit()
            .putString(KEY_VIDEO_ID, point.videoId)
            .putString(KEY_TITLE, point.title)
            .putLong(KEY_POSITION_MS, point.positionMs)
            .apply()
    }

    /** Xóa lời nhắc khi đúng video đó đã xem hết; video khác kết thúc thì giữ nguyên. */
    override fun clearIf(videoId: String?) {
        if (ResumeRules.shouldClear(prefs.getString(KEY_VIDEO_ID, null), videoId)) {
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
