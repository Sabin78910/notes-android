package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStateTest {
    @Test fun emptyStoreShowsPlaceholder() {
        val s = WidgetState.from(emptyList())
        assertNull(s.noteId)
        assertEquals(WidgetState.EMPTY_TEXT, s.preview)
    }

    @Test fun showsNewestPinnedNote() {
        val s = WidgetState.from(listOf(
            Note(1, "old pin", pinned = true), Note(2, "unpinned"), Note(3, "new pin", pinned = true)
        ))
        assertEquals(3L, s.noteId)
        assertEquals("new pin", s.preview)
    }

    @Test fun fallsBackToNewestVisibleWhenNothingPinned() {
        val s = WidgetState.from(listOf(Note(1, "a"), Note(2, "b")))
        assertEquals("b", s.preview)
    }

    @Test fun ignoresArchivedAndTrashed() {
        val s = WidgetState.from(listOf(
            Note(1, "kept"), Note(2, "gone", pinned = true, trashedAt = 5L), Note(3, "arch", pinned = true, archived = true)
        ))
        assertEquals(1L, s.noteId)
    }

    @Test fun longPreviewIsTruncated() {
        val s = WidgetState.from(listOf(Note(1, "x".repeat(500))))
        assertTrue(s.preview.length <= WidgetState.MAX_PREVIEW + 1)
        assertTrue(s.preview.endsWith("…"))
    }

    @Test fun checklistShowsProgress() {
        val s = WidgetState.from(listOf(Note(1, "milk\neggs", checklist = true, checked = setOf(0))))
        assertEquals("milk\neggs", s.preview)
        assertEquals("1/2 done", s.progress)
    }

    @Test fun plainNoteHasNoProgress() {
        assertNull(WidgetState.from(listOf(Note(1, "hi"))).progress)
    }

    @Test fun launchActionParsing() {
        assertEquals(LaunchAction.NEW_NOTE, LaunchAction.fromName("new_note"))
        assertEquals(LaunchAction.NEW_CHECKLIST, LaunchAction.fromName("new_checklist"))
        assertNull(LaunchAction.fromName("bogus"))
        assertNull(LaunchAction.fromName(null))
    }
}
