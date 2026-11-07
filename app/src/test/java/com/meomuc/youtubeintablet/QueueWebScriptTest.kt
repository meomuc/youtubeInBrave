package com.meomuc.youtubeintablet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueWebScriptTest {

    private val script = QueueWebScript.SCRIPT

    @Test
    fun allPlaceholdersAreResolved() {
        // Không còn "${" thì mọi giá trị nội suy đã được thay bằng chuỗi thật.
        assertFalse(script.contains("\${"))
    }

    @Test
    fun usesSharedWatchPathAndLinkPattern() {
        assertTrue(script.contains("'/watch'"))
        assertTrue(script.contains(WatchLinkRules.HREF_PATTERN_SOURCE))
    }

    @Test
    fun keepsBridgeContractAndMarkers() {
        assertTrue(script.contains("AndroidQueue.add(id, title || '', next)"))
        assertTrue(script.contains("data-yt-tablet-watch"))
        assertTrue(script.contains("data-yt-tablet-cover"))
        assertTrue(script.contains("slim-video-action-bar-actions"))
        assertTrue(script.contains("MutationObserver"))
    }

    @Test
    fun embedsIconPathDataAsJsStrings() {
        assertTrue(script.contains("\"" + PhosphorIcons.path("list-plus") + "\""))
        assertTrue(script.contains("\"" + PhosphorIcons.path("skip-forward") + "\""))
    }

    @Test
    fun avoidsHtmlSinksBecauseYoutubeEnforcesTrustedTypes() {
        // YouTube bật Trusted Types: gán innerHTML sẽ ném lỗi, nên SVG phải dựng bằng createElementNS.
        assertFalse(script.contains(".innerHTML"))
        assertTrue(script.contains("createElementNS"))
    }
}
