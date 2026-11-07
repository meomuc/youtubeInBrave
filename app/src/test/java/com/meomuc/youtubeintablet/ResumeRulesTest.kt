package com.meomuc.youtubeintablet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumeRulesTest {

    private val id = "dQw4w9WgXcQ"

    @Test
    fun savesPositionInMilliseconds() {
        val point = ResumeRules.toSave(id, "Video A", 12.345, 300.0)
        assertEquals(ResumePoint(id, "Video A", 12345L), point)
    }

    @Test
    fun rejectsInvalidVideoId() {
        assertNull(ResumeRules.toSave(null, "T", 10.0, 300.0))
        assertNull(ResumeRules.toSave("short", "T", 10.0, 300.0))
        assertNull(ResumeRules.toSave("bad id with spaces!", "T", 10.0, 300.0))
    }

    @Test
    fun rejectsLivestreamWithInfiniteDuration() {
        assertNull(ResumeRules.toSave(id, "Live", 10.0, Double.POSITIVE_INFINITY))
    }

    @Test
    fun rejectsNotYetKnownDuration() {
        assertNull(ResumeRules.toSave(id, "T", 0.0, Double.NaN))
        assertNull(ResumeRules.toSave(id, "T", 0.0, 0.0))
    }

    @Test
    fun rejectsNonFiniteOrNegativePosition() {
        assertNull(ResumeRules.toSave(id, "T", Double.NaN, 300.0))
        assertNull(ResumeRules.toSave(id, "T", -1.0, 300.0))
    }

    @Test
    fun clampsPositionToDuration() {
        val point = ResumeRules.toSave(id, "T", 500.0, 300.0)
        assertEquals(300_000L, point?.positionMs)
    }

    @Test
    fun usesVideoIdWhenTitleIsBlankAndTrimsLongTitle() {
        assertEquals(id, ResumeRules.toSave(id, "   ", 10.0, 300.0)?.title)
        assertEquals(id, ResumeRules.toSave(id, null, 10.0, 300.0)?.title)
        val long = "a".repeat(500)
        assertEquals(200, ResumeRules.toSave(id, long, 10.0, 300.0)?.title?.length)
    }

    @Test
    fun promptsOnlyForPositionAtLeastFiveSeconds() {
        assertFalse(ResumeRules.shouldPrompt(null))
        assertFalse(ResumeRules.shouldPrompt(ResumePoint(id, "T", 4_999L)))
        assertTrue(ResumeRules.shouldPrompt(ResumePoint(id, "T", 5_000L)))
    }
}
