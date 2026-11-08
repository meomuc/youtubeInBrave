package com.meomuc.youtubeintablet

/** Việc app phải làm khi trạng thái phát đổi, tách khỏi MainActivity. */
enum class PlaybackEffect {
    /** Ẩn lời nhắc tiếp tục xem vì video đã phát. */
    HIDE_RESUME_BANNER,
    /** Hủy lệnh dừng đang chờ và khởi động service phát nền. */
    START_SERVICE,
    /** Hẹn dừng service sau một khoảng chờ ngắn. */
    SCHEDULE_STOP,
    /** Cập nhật nút phát/tạm dừng trong PiP nếu đang ở PiP. */
    REFRESH_PIP,
    /** Cập nhật trạng thái phát cho MediaSession (nút vô lăng, điều khiển hệ thống). */
    UPDATE_MEDIA_SESSION,
}

/** Quy tắc thuần: trạng thái phát đổi thì cần làm những việc gì, theo thứ tự nào. */
object PlaybackStateRules {

    /** Trả về danh sách hiệu ứng; rỗng nếu trạng thái không đổi. */
    fun effectsFor(wasPlaying: Boolean, playing: Boolean): List<PlaybackEffect> {
        if (wasPlaying == playing) return emptyList()
        return if (playing) {
            listOf(
                PlaybackEffect.HIDE_RESUME_BANNER,
                PlaybackEffect.START_SERVICE,
                PlaybackEffect.REFRESH_PIP,
                PlaybackEffect.UPDATE_MEDIA_SESSION,
            )
        } else {
            listOf(PlaybackEffect.SCHEDULE_STOP, PlaybackEffect.REFRESH_PIP, PlaybackEffect.UPDATE_MEDIA_SESSION)
        }
    }
}
