package com.meomuc.youtubeintablet

import android.media.AudioManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFocusRulesTest {

    @Test
    fun pausesOnPermanentLoss() {
        assertTrue(AudioFocusRules.shouldPause(AudioManager.AUDIOFOCUS_LOSS))
    }

    @Test
    fun pausesOnTransientLossLikeIncomingCall() {
        assertTrue(AudioFocusRules.shouldPause(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT))
    }

    @Test
    fun keepsPlayingWhenOtherAppOnlyDucks() {
        assertFalse(AudioFocusRules.shouldPause(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK))
    }

    @Test
    fun ignoresFocusGain() {
        assertFalse(AudioFocusRules.shouldPause(AudioManager.AUDIOFOCUS_GAIN))
    }
}
