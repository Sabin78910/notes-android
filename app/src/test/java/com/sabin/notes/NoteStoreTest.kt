package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assert.assertThrows

class NoteStoreTest {
    @Test fun pinnedNotesComeFirst() {
        val s = NoteStore()
        val a = s.add("first"); s.add("second")
        s.togglePin(a.id)
        assertEquals("first", s.visible().first().text)
    }

    @Test fun searchIsCaseInsensitive() {
        val s = NoteStore(); s.add("Buy MILK"); s.add("Call mom")
        assertEquals(listOf("Buy MILK"), s.visible("milk").map { it.text })
    }

    @Test fun roundTripsThroughSerialization() {
        val s = NoteStore(); s.add("line1\nline2\twith tab \\ slash"); val b = s.add("b"); s.togglePin(b.id)
        val restored = NoteStore.deserialize(s.serialize())
        assertEquals(s.visible(), restored.visible())
        assertTrue(restored.visible().first().pinned)
    }

    @Test fun editChangesTextAndKeepsPin() {
        val s = NoteStore(); val a = s.add("old"); s.togglePin(a.id)
        s.edit(a.id, "  new  ")
        val n = s.visible().single()
        assertEquals("new", n.text)
        assertTrue(n.pinned)
    }

    @Test fun editRejectsBlankText() {
        val s = NoteStore(); val a = s.add("old")
        assertThrows(IllegalArgumentException::class.java) { s.edit(a.id, "   ") }
        assertEquals("old", s.visible().single().text)
    }

    @Test fun editUnknownIdIsNoOp() {
        val s = NoteStore(); s.add("old")
        s.edit(999, "new")
        assertEquals(listOf("old"), s.visible().map { it.text })
    }

    @Test fun editedNoteSurvivesSerialization() {
        val s = NoteStore(); val a = s.add("old"); s.edit(a.id, "new")
        assertEquals("new", NoteStore.deserialize(s.serialize()).visible().single().text)
    }

    @Test fun restoreBringsBackDeletedNoteWithPin() {
        val s = NoteStore(); val a = s.add("keep"); s.togglePin(a.id)
        val pinned = s.visible().single()
        s.delete(a.id)
        assertTrue(s.visible().isEmpty())
        s.restore(pinned)
        assertEquals(listOf(pinned), s.visible())
    }

    @Test fun restoreDoesNotDuplicateExistingNote() {
        val s = NoteStore(); val a = s.add("x")
        s.restore(a)
        assertEquals(1, s.visible().size)
    }

    @Test fun addAfterRestoreGetsFreshId() {
        val s = NoteStore(); val a = s.add("x")
        s.delete(a.id); s.restore(a)
        assertTrue(s.add("y").id > a.id)
    }
}
