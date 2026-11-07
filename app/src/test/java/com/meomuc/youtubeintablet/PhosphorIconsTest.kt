package com.meomuc.youtubeintablet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhosphorIconsTest {

    private val names = listOf(
        "queue", "list-plus", "skip-forward", "x", "play", "trash",
        "dots-six-vertical", "picture-in-picture", "list",
    )

    @Test
    fun everyIconHasNonBlankPath() {
        names.forEach { name ->
            assertTrue("path rỗng: $name", PhosphorIcons.path(name).isNotBlank())
        }
    }

    @Test
    fun unknownIconFailsFast() {
        var failed = false
        try {
            PhosphorIcons.path("khong-co")
        } catch (e: IllegalArgumentException) {
            failed = true
        }
        assertTrue(failed)
    }

    @Test
    fun pathDataHasNoQuotesSoItCanBeEmbeddedInJs() {
        names.forEach { name ->
            assertFalse("path có dấu nháy: $name", PhosphorIcons.path(name).contains('"') || PhosphorIcons.path(name).contains('\''))
        }
    }

    @Test
    fun pathDataMatchesPhosphorRegularSource() {
        assertEquals(
            "M32,64a8,8,0,0,1,8-8H216a8,8,0,0,1,0,16H40A8,8,0,0,1,32,64Zm104,56H40a8,8,0,0,0,0,16h96a8,8,0,0,0,0-16Zm0,64H40a8,8,0,0,0,0,16h96a8,8,0,0,0,0-16Zm112-24a8,8,0,0,1-3.76,6.78l-64,40A8,8,0,0,1,168,200V120a8,8,0,0,1,12.24-6.78l64,40A8,8,0,0,1,248,160Zm-23.09,0L184,134.43v51.14Z",
            PhosphorIcons.path("queue"),
        )
    }
}
