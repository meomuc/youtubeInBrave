package com.meomuc.youtubeintablet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lệnh từ MediaSession (nút vô lăng, tai nghe, điều khiển hệ thống) đổi thành script cho trang YouTube. */
class MediaSessionRulesTest {

    @Test
    fun playAndPauseDriveTheVideoElement() {
        assertTrue(MediaSessionRules.scriptFor(MediaCommand.PLAY).contains("v.play()"))
        assertTrue(MediaSessionRules.scriptFor(MediaCommand.PAUSE).contains("v.pause()"))
    }

    @Test
    fun nextClicksYoutubeNextButton() {
        assertTrue(MediaSessionRules.scriptFor(MediaCommand.NEXT).contains(".ytp-next-button"))
    }

    @Test
    fun previousRestartsWhenPastThreeSecondsElseGoesBack() {
        val script = MediaSessionRules.scriptFor(MediaCommand.PREVIOUS)
        assertTrue(script.contains("currentTime>3"))
        assertTrue(script.contains("history.back()"))
    }

    @Test
    fun scriptsHaveNoKotlinInterpolationLeftovers() {
        for (command in MediaCommand.values()) {
            assertFalse(MediaSessionRules.scriptFor(command).contains("\${"))
        }
    }
}
