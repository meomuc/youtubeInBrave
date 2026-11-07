package com.meomuc.youtubeintablet

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

/** Quy tắc thuần: khi nào phải tạm dừng theo thay đổi audio focus (ticket #18). */
object AudioFocusRules {

    /**
     * Mất focus (kể cả tạm thời vì cuộc gọi) thì tạm dừng.
     * Bị app khác "ducking" thì vẫn phát, để hệ thống tự giảm âm lượng.
     */
    fun shouldPause(focusChange: Int): Boolean =
        focusChange == AudioManager.AUDIOFOCUS_LOSS ||
            focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
}

/**
 * Giữ audio focus khi video đang phát. Khi mất focus, gọi [onLost] để tạm dừng video.
 * Không bao giờ tự phát lại khi focus quay về; người dùng phải bấm Play.
 */
class PlaybackAudioFocus(context: Context, private val onLost: () -> Unit) {

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var request: AudioFocusRequest? = null

    private val listener = AudioManager.OnAudioFocusChangeListener { change ->
        if (AudioFocusRules.shouldPause(change)) {
            release()
            onLost()
        }
    }

    /** Xin focus khi bắt đầu phát. Nếu hệ thống từ chối thì vẫn để video phát, không chặn. */
    fun acquire() {
        if (request != null) return
        val newRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            .setOnAudioFocusChangeListener(listener, mainHandler)
            .build()
        if (audioManager.requestAudioFocus(newRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            request = newRequest
        }
    }

    /** Trả focus khi không còn phát. */
    fun release() {
        request?.let { audioManager.abandonAudioFocusRequest(it) }
        request = null
    }
}
