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
    fun hidesMobileShortsTabAndShelvesWhenShortsOff() {
        // Giao diện m.youtube.com (đã kiểm chứng trên DOM): mục thanh dưới, kệ Shorts và thẻ Shorts.
        val css = DisplayFilter.css(DisplayOptions(showShorts = false))
        assertTrue(css.contains("ytm-pivot-bar-item-renderer:has(.pivot-shorts)"))
        assertTrue(css.contains("ytm-rich-section-renderer:has(ytm-shorts-lockup-view-model)"))
    }

    @Test
    fun showsShortsWhenShortsOn() {
        assertFalse(DisplayFilter.css(DisplayOptions(showShorts = true)).contains("ytd-reel-shelf-renderer"))
    }

    @Test
    fun hidesCommentsOnlyWhenCommentsOff() {
        assertTrue(DisplayFilter.css(DisplayOptions(showComments = false)).contains("ytd-watch-flexy #comments"))
        assertFalse(DisplayFilter.css(DisplayOptions(showComments = true)).contains("#comments"))
    }

    @Test
    fun hidesRelatedOnlyOnWatchPageWhenRelatedOff() {
        val css = DisplayFilter.css(DisplayOptions(showRelated = false))
        assertTrue(css.contains("ytd-watch-flexy #related"))
        // Trang chủ vẫn phải hiện đề xuất: không có selector nào nhắm vào lưới trang chủ.
        assertFalse(css.contains("ytd-rich-grid"))
        assertFalse(DisplayFilter.css(DisplayOptions(showRelated = true)).contains("#related"))
    }

    @Test
    fun emptyCssWhenEverythingShown() {
        assertTrue(DisplayFilter.css(DisplayOptions(showShorts = true, showComments = true, showRelated = true)).isEmpty())
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
