package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NoteDuplicateTest {
    private val source = Note(3, "a\nb", pinned = true, createdAt = 10, checklist = true, checked = setOf(1),
        color = NoteColor.values().first(), tags = setOf("x", "y"), remindAt = 99L)

    @Test fun copyKeepsContentAndResetsState() {
        val c = NoteDuplicate.copyOf(source, newId = 8, now = 500)
        assertEquals(8L, c.id)
        assertEquals(500L, c.createdAt)
        assertEquals(source.text, c.text)
        assertEquals(source.checklist, c.checklist)
        assertEquals(source.checked, c.checked)
        assertEquals(source.color, c.color)
        assertEquals(source.tags, c.tags)
        assertFalse(c.pinned)
        assertNull(c.remindAt)
        assertFalse(c.archived)
        assertNull(c.trashedAt)
    }

    @Test fun trashedAndArchivedSourceGivesActiveCopy() {
        val c = NoteDuplicate.copyOf(source.copy(archived = true, trashedAt = 5), 9, 1)
        assertFalse(c.archived)
        assertNull(c.trashedAt)
    }

    @Test fun storeDuplicateAddsVisibleCopyWithFreshId() {
        val store = NoteStore(listOf(source))
        val c = store.duplicate(3, now = 700)
        assertNotNull(c)
        assertNotEquals(3L, c!!.id)
        assertEquals(2, store.all().size)
        assertEquals(listOf(3L, c.id), store.visible().map { it.id })
        assertEquals(source, store.all().first { it.id == 3L })
    }

    @Test fun storeDuplicateUnknownIdReturnsNull() {
        assertNull(NoteStore().duplicate(1))
    }
}
