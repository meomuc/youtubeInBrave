package com.meomuc.youtubeintablet

import org.junit.Assert.assertEquals
import org.junit.Test

class RequestTypeMapperTest {

    @Test
    fun mapsScriptByExtension() {
        assertEquals("script", RequestTypeMapper.typeOf("https://www.youtube.com/s/player/base.js", null))
    }

    @Test
    fun mapsStylesheetByAccept() {
        assertEquals("stylesheet", RequestTypeMapper.typeOf("https://www.youtube.com/a", "text/css,*/*;q=0.1"))
    }

    @Test
    fun mapsImageByAccept() {
        assertEquals("image", RequestTypeMapper.typeOf("https://i.ytimg.com/vi/x/hq.jpg", "image/webp,*/*"))
    }

    @Test
    fun mapsFontByExtension() {
        assertEquals("font", RequestTypeMapper.typeOf("https://fonts.gstatic.com/s/roboto.woff2", null))
    }

    @Test
    fun mapsMediaByAccept() {
        assertEquals("media", RequestTypeMapper.typeOf("https://rr1.googlevideo.com/videoplayback", "video/webm,*/*"))
    }

    @Test
    fun mapsXhrByJsonAccept() {
        assertEquals("xmlhttprequest", RequestTypeMapper.typeOf("https://www.youtube.com/api/x", "application/json"))
    }

    @Test
    fun fallsBackToOther() {
        assertEquals("other", RequestTypeMapper.typeOf("https://www.youtube.com/unknown", null))
    }
}
