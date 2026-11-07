package com.meomuc.youtubeintablet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayOptionsTest {

    @Test
    fun defaultsMatchApprovedDesign() {
        val d = DisplayOptions()
        assertFalse(d.showShorts)
        assertTrue(d.showQueue)
        assertTrue(d.showComments)
        assertTrue(d.showRelated)
    }

    @Test
    fun readsDefaultsWhenNothingStored() {
        val d = DisplayOptions.fromStore { _, default -> default }
        assertEquals(DisplayOptions(), d)
    }

    @Test
    fun roundTripsThroughStore() {
        val saved = mutableMapOf<String, Boolean>()
        val changed = DisplayOptions(showShorts = true, showQueue = false, showComments = false, showRelated = false)
        changed.writeTo { key, value -> saved[key] = value }
        val read = DisplayOptions.fromStore { key, default -> saved[key] ?: default }
        assertEquals(changed, read)
    }

    @Test
    fun usesStableKeys() {
        val keys = mutableSetOf<String>()
        DisplayOptions().writeTo { key, _ -> keys.add(key) }
        assertEquals(
            setOf("show_shorts", "show_queue", "show_comments", "show_related"),
            keys
        )
    }
}
