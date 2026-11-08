package com.meomuc.youtubeintablet

import com.meomuc.youtubeintablet.PlaybackEffect.HIDE_RESUME_BANNER
import com.meomuc.youtubeintablet.PlaybackEffect.REFRESH_PIP
import com.meomuc.youtubeintablet.PlaybackEffect.SCHEDULE_STOP
import com.meomuc.youtubeintablet.PlaybackEffect.START_SERVICE
import org.junit.Assert.assertEquals
import org.junit.Test

/** Bước 3: quyết định hiệu ứng khi trạng thái phát đổi, tách khỏi MainActivity. */
class PlaybackStateRulesTest {

    @Test
    fun noEffectWhenStateDoesNotChange() {
        assertEquals(emptyList<PlaybackEffect>(), PlaybackStateRules.effectsFor(wasPlaying = false, playing = false))
        assertEquals(emptyList<PlaybackEffect>(), PlaybackStateRules.effectsFor(wasPlaying = true, playing = true))
    }

    @Test
    fun startingPlaybackHidesBannerStartsServiceAndRefreshesPip() {
        assertEquals(
            listOf(HIDE_RESUME_BANNER, START_SERVICE, REFRESH_PIP),
            PlaybackStateRules.effectsFor(wasPlaying = false, playing = true),
        )
    }

    @Test
    fun pausingSchedulesStopAndRefreshesPip() {
        assertEquals(
            listOf(SCHEDULE_STOP, REFRESH_PIP),
            PlaybackStateRules.effectsFor(wasPlaying = true, playing = false),
        )
    }
}
