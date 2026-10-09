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

    @Test fun addRecordsCreatedAt() {
        val s = NoteStore()
        assertEquals(1234L, s.add("a", now = 1234L).createdAt)
    }

    @Test fun createdAtSurvivesSerializationAndEdit() {
        val s = NoteStore(); val a = s.add("a", now = 5555L); s.togglePin(a.id); s.edit(a.id, "b")
        val n = NoteStore.deserialize(s.serialize()).visible().single()
        assertEquals(5555L, n.createdAt)
        assertEquals("b", n.text)
        assertTrue(n.pinned)
    }

    @Test fun deserializesOldFormatWithoutCreatedAt() {
        val s = NoteStore.deserialize("1\ttrue\told one\n2\tfalse\tplain")
        val notes = s.visible()
        assertEquals(listOf("old one", "plain"), notes.map { it.text })
        assertTrue(notes.all { it.createdAt == 0L })
        assertTrue(notes.first().pinned)
    }
}
