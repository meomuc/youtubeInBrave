package com.meomuc.youtubeintablet

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Kiểm tra các script JS tĩnh đã được tách sang assets/js (Bước 1 của kế hoạch kiến trúc). */
class JsAssetsTest {

    private val dir = File("src/main/assets/js")

    @Test
    fun playbackHookAssetExistsAndKeepsBridgeContract() {
        val src = File(dir, "playback_hook.js").readText()
        assertTrue(src.contains("AndroidPlayback.onState(playing)"))
        assertTrue(src.contains("AndroidPlayback.onProgress("))
        assertTrue(src.contains("AndroidPlayback.onEnded("))
        assertTrue(src.contains("window.__ytTabletHooked"))
    }

    @Test
    fun toggleAssetExistsAndKeepsUserPausedFlag() {
        val src = File(dir, "toggle.js").readText()
        assertTrue(src.contains("window.__ytTabletUserPaused"))
        assertTrue(src.contains("v.play()"))
        assertTrue(src.contains("v.pause()"))
    }

    @Test
    fun assetsHaveNoKotlinInterpolationLeftovers() {
        for (name in listOf("playback_hook.js", "toggle.js")) {
            assertFalse("$name còn \${", File(dir, name).readText().contains("\${"))
        }
    }
}
