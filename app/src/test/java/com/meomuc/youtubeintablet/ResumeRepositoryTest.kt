package com.meomuc.youtubeintablet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lưu vị trí xem dở qua interface thuần, kiểm thử không cần Android. */
class ResumeRepositoryTest {

    private val id = "dQw4w9WgXcQ"
    private val other = "aaaaaaaaaaa"

    @Test
    fun sanitizeLoadedAcceptsValidStoredValues() {
        val point = ResumeRules.sanitizeLoaded(StoredResume(id, "Title", 12_000L))
        assertEquals(ResumePoint(id, "Title", 12_000L), point)
    }

    @Test
    fun sanitizeLoadedRejectsMissingOrBadValues() {
        assertNull(ResumeRules.sanitizeLoaded(StoredResume(null, "T", 1L)))
        assertNull(ResumeRules.sanitizeLoaded(StoredResume("bad id!", "T", 1L)))
        assertNull(ResumeRules.sanitizeLoaded(StoredResume(id, null, 1L)))
        assertNull(ResumeRules.sanitizeLoaded(StoredResume(id, "T", -1L)))
    }

    @Test
    fun shouldClearOnlyForTheSameVideoId() {
        assertTrue(ResumeRules.shouldClear(id, id))
        assertFalse(ResumeRules.shouldClear(id, other))
        assertFalse(ResumeRules.shouldClear(id, null))
        assertFalse(ResumeRules.shouldClear(null, id))
    }

    @Test
    fun inMemoryRepositorySavesLoadsAndClearsOnlyTheSameVideo() {
        val repo = InMemoryResumeRepository()
        assertNull(repo.load())

        repo.save(ResumePoint(id, "T", 10_000L))
        assertEquals(ResumePoint(id, "T", 10_000L), repo.load())

        repo.clearIf(other)
        assertEquals(ResumePoint(id, "T", 10_000L), repo.load())

        repo.clearIf(id)
        assertNull(repo.load())
    }

    @Test
    fun inMemoryRepositoryOverwritesWithLatestSave() {
        val repo = InMemoryResumeRepository()
        repo.save(ResumePoint(id, "A", 1_000L))
        repo.save(ResumePoint(other, "B", 2_000L))
        assertEquals(ResumePoint(other, "B", 2_000L), repo.load())
    }
}
