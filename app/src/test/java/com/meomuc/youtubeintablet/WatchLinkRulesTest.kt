package com.meomuc.youtubeintablet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchLinkRulesTest {

    @Test
    fun readsIdFromRelativeWatchHref() {
        assertEquals("dQw4w9WgXcQ", WatchLinkRules.videoIdFromHref("/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun readsIdWhenOtherParamsFollow() {
        assertEquals("abc_DEF-123", WatchLinkRules.videoIdFromHref("/watch?v=abc_DEF-123&t=42s"))
        assertEquals("dQw4w9WgXcQ", WatchLinkRules.videoIdFromHref("/watch?feature=share&v=dQw4w9WgXcQ"))
    }

    @Test
    fun readsIdFromAbsoluteHref() {
        assertEquals("dQw4w9WgXcQ", WatchLinkRules.videoIdFromHref("https://m.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun rejectsNonWatchOrMalformedHref() {
        assertNull(WatchLinkRules.videoIdFromHref("/channel/UC123"))
        assertNull(WatchLinkRules.videoIdFromHref("/watch?v=short"))
        assertNull(WatchLinkRules.videoIdFromHref("/watch?v=toolongvideoid123"))
        assertNull(WatchLinkRules.videoIdFromHref(""))
    }
}
