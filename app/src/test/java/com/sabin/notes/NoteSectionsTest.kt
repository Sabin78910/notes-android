package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteSectionsTest {
    private val a = Note(1, "a", pinned = true)
    private val b = Note(2, "b")
    private val c = Note(3, "c", pinned = true)

    @Test fun splitKeepsOrderWithinSections() {
        val s = NoteSections.split(listOf(b, a, c))
        assertEquals(listOf(a, c), s.pinned)
        assertEquals(listOf(b), s.others)
    }

    @Test fun headersOnlyWhenPinnedNotesExistInNotesView() {
        assertTrue(NoteSections.split(listOf(a, b)).showHeaders)
        assertFalse(NoteSections.split(listOf(b)).showHeaders)
        assertFalse(NoteSections.split(emptyList()).showHeaders)
    }

    @Test fun headersHiddenWhenEverythingPinned() {
        assertTrue(NoteSections.split(listOf(a, c)).showHeaders)
        assertFalse(NoteSections.split(listOf(a, c)).othersNonEmpty)
    }

    @Test fun motionIsSkippedWhenAnimationsAreOff() {
        assertTrue(NoteSections.useSpringMotion(animationScale = 1f))
        assertFalse(NoteSections.useSpringMotion(animationScale = 0f))
    }
}
