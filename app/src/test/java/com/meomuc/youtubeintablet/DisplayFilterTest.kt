package com.meomuc.youtubeintablet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayFilterTest {

    @Test
    fun hidesShortsWhenShortsOff() {
        val css = DisplayFilter.css(DisplayOptions(showShorts = false))
        assertTrue(css.contains("ytd-reel-shelf-renderer"))
        assertTrue(css.contains("""a[href^="/shorts"]"""))
        assertTrue(css.contains("display:none"))
    }

    @Test
    fun showsShortsWhenShortsOn() {
        assertFalse(DisplayFilter.css(DisplayOptions(showShorts = true)).contains("ytd-reel-shelf-renderer"))
    }

    @Test
    fun applyJsEscapesCssForJavascriptString() {
        val js = DisplayFilter.applyJs(DisplayOptions(showShorts = false))
        // CSS có dấu nháy kép trong selector: phải được escape, không được có ký tự xuống dòng thô.
        assertTrue(js.contains("""\"/shorts\""""))
        assertFalse(js.contains("\n"))
        assertTrue(js.contains(DisplayFilter.STYLE_ID))
    }
}
