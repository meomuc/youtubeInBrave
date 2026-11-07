package com.meomuc.youtubeintablet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdBlockerTest {

    @Test
    fun blocksRequestToKnownAdDomain() {
        assertTrue(AdBlocker.isBlocked("https://doubleclick.net/pagead/ads?x=1"))
    }

    @Test
    fun blocksSubdomainOfKnownAdDomain() {
        assertTrue(AdBlocker.isBlocked("https://static.doubleclick.net/instream/ad_status.js"))
    }

    @Test
    fun allowsYoutubePage() {
        assertFalse(AdBlocker.isBlocked("https://www.youtube.com/watch?v=abc"))
    }

    @Test
    fun allowsVideoStreamHost() {
        assertFalse(AdBlocker.isBlocked("https://rr1---sn-abc.googlevideo.com/videoplayback?id=1"))
    }

    @Test
    fun doesNotBlockDomainThatMerelyEndsWithAdDomainName() {
        assertFalse(AdBlocker.isBlocked("https://notdoubleclick.net/x"))
    }

    @Test
    fun allowsMalformedUrl() {
        assertFalse(AdBlocker.isBlocked("not a url"))
    }
}
