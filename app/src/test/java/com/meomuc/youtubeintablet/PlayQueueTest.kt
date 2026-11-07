package com.meomuc.youtubeintablet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayQueueTest {

    private fun item(id: String) = QueueItem(videoId = id, title = "Video $id")

    private fun ids(q: PlayQueue) = q.items.map { it.videoId }

    @Test
    fun appendAddsToEnd() {
        val q = PlayQueue()
        q.append(item("a"))
        q.append(item("b"))
        assertEquals(listOf("a", "b"), ids(q))
    }

    @Test
    fun appendDuplicateIsIgnored() {
        val q = PlayQueue()
        q.append(item("a"))
        q.append(item("b"))
        q.append(item("a"))
        assertEquals(listOf("a", "b"), ids(q))
    }

    @Test
    fun playNextInsertsNewItemAtFront() {
        val q = PlayQueue(listOf(item("a"), item("b")))
        q.playNext(item("c"))
        assertEquals(listOf("c", "a", "b"), ids(q))
    }

    @Test
    fun playNextMovesExistingItemToFrontWithoutDuplicating() {
        val q = PlayQueue(listOf(item("a"), item("b"), item("c")))
        q.playNext(item("c"))
        assertEquals(listOf("c", "a", "b"), ids(q))
    }

    @Test
    fun pollHeadReturnsAndRemovesFirstItem() {
        val q = PlayQueue(listOf(item("a"), item("b")))
        assertEquals("a", q.pollHead()?.videoId)
        assertEquals(listOf("b"), ids(q))
    }

    @Test
    fun pollHeadOnEmptyQueueReturnsNull() {
        val q = PlayQueue()
        assertNull(q.pollHead())
        assertEquals(true, q.isEmpty)
    }

    @Test
    fun moveReordersItems() {
        val q = PlayQueue(listOf(item("a"), item("b"), item("c")))
        q.move(0, 2)
        assertEquals(listOf("b", "c", "a"), ids(q))
    }

    @Test
    fun moveWithInvalidIndexIsIgnored() {
        val q = PlayQueue(listOf(item("a"), item("b")))
        q.move(0, 5)
        q.move(-1, 0)
        assertEquals(listOf("a", "b"), ids(q))
    }

    @Test
    fun removeAtDeletesOnlyThatItem() {
        val q = PlayQueue(listOf(item("a"), item("b"), item("c")))
        q.removeAt(1)
        assertEquals(listOf("a", "c"), ids(q))
    }

    @Test
    fun removeAtInvalidIndexIsIgnored() {
        val q = PlayQueue(listOf(item("a")))
        q.removeAt(3)
        assertEquals(listOf("a"), ids(q))
    }

    @Test
    fun codecRoundTripsTitlesWithSpecialCharacters() {
        val items = listOf(
            QueueItem("id1", "Tiêu đề có tab\tvà xuống dòng\nvà %ký tự"),
            QueueItem("id2", ""),
        )
        assertEquals(items, QueueCodec.decode(QueueCodec.encode(items)))
    }

    @Test
    fun validVideoIdAcceptsYoutubeIdsOnly() {
        assertEquals(true, isValidVideoId("dQw4w9WgXcQ"))
        assertEquals(true, isValidVideoId("abc_-DEF123"))
        assertEquals(false, isValidVideoId(""))
        assertEquals(false, isValidVideoId("dQw4w9WgXc"))
        assertEquals(false, isValidVideoId("dQw4w9WgXcQQ"))
        assertEquals(false, isValidVideoId("abc<script>"))
    }

    @Test
    fun codecDecodesEmptyAndSkipsMalformedLines() {
        assertEquals(emptyList<QueueItem>(), QueueCodec.decode(""))
        assertEquals(
            listOf(QueueItem("ok", "Được")),
            QueueCodec.decode("\nkhong-co-tab\n\tthieu-id\nok\t" + java.net.URLEncoder.encode("Được", "UTF-8") + "\n")
        )
    }
}
