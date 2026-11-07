package com.meomuc.youtubeintablet

import com.meomuc.youtubeintablet.UnavailableAction.GO_HOME
import com.meomuc.youtubeintablet.UnavailableAction.SKIP_TO_NEXT
import com.meomuc.youtubeintablet.UnavailableAction.STAY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackErrorRulesTest {

    @Test
    fun recognizesUnavailableTextInEnglishAndVietnamese() {
        assertTrue(PlaybackErrorRules.isUnavailableText("Video unavailable"))
        assertTrue(PlaybackErrorRules.isUnavailableText("This video is private"))
        assertTrue(PlaybackErrorRules.isUnavailableText("Video không khả dụng"))
        assertTrue(PlaybackErrorRules.isUnavailableText("Video riêng tư"))
    }

    @Test
    fun ignoresUnrelatedOrBlankPlayerText() {
        assertFalse(PlaybackErrorRules.isUnavailableText(null))
        assertFalse(PlaybackErrorRules.isUnavailableText(""))
        assertFalse(PlaybackErrorRules.isUnavailableText("   "))
        assertFalse(PlaybackErrorRules.isUnavailableText("Đã xảy ra lỗi"))
        assertFalse(PlaybackErrorRules.isUnavailableText("Phát lỗi, thử lại"))
    }

    @Test
    fun treatsConnectionErrorCodesAsNetworkErrors() {
        // Mã WebViewClient: HOST_LOOKUP=-2, CONNECT=-6, IO=-7, TIMEOUT=-8.
        listOf(-2, -6, -7, -8).forEach { assertTrue("code $it", PlaybackErrorRules.isNetworkError(it)) }
    }

    @Test
    fun otherErrorCodesAreNotNetworkErrors() {
        // ERROR_UNKNOWN=-1, ERROR_UNSUPPORTED_SCHEME=-10: lỗi chưa rõ, không coi là mất mạng.
        assertFalse(PlaybackErrorRules.isNetworkError(-1))
        assertFalse(PlaybackErrorRules.isNetworkError(-10))
    }

    @Test
    fun skipsUnavailableVideoToNextWhenQueueHasItems() {
        assertEquals(SKIP_TO_NEXT, PlaybackErrorRules.decideOnUnavailable(isResumeVideo = false, queueOn = true, queueHasItems = true))
    }

    @Test
    fun staysWhenQueueIsOff() {
        assertEquals(STAY, PlaybackErrorRules.decideOnUnavailable(isResumeVideo = false, queueOn = false, queueHasItems = true))
    }

    @Test
    fun staysWhenQueueIsEmpty() {
        assertEquals(STAY, PlaybackErrorRules.decideOnUnavailable(isResumeVideo = false, queueOn = true, queueHasItems = false))
    }

    @Test
    fun goesHomeWhenUnavailableVideoIsTheResumeVideo() {
        // Ưu tiên lời nhắc tiếp tục xem hơn hàng đợi: về trang chủ, không tự nhảy mục khác.
        assertEquals(GO_HOME, PlaybackErrorRules.decideOnUnavailable(isResumeVideo = true, queueOn = true, queueHasItems = true))
        assertEquals(GO_HOME, PlaybackErrorRules.decideOnUnavailable(isResumeVideo = true, queueOn = false, queueHasItems = false))
    }
}
