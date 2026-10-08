package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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

    @Test fun addRecordsCreatedAt() {
        val n = NoteStore().add("x", now = 1234L)
        assertEquals(1234L, n.createdAt)
    }

    @Test fun createdAtRoundTrips() {
        val s = NoteStore(); val n = s.add("a\tb", now = 99L); s.togglePin(n.id)
        val r = NoteStore.deserialize(s.serialize()).visible().single()
        assertEquals(99L, r.createdAt)
        assertEquals("a\tb", r.text)
        assertTrue(r.pinned)
    }

    @Test fun deserializesOldFormatWithoutCreatedAt() {
        val r = NoteStore.deserialize("1\ttrue\told\ttext\n2\tfalse\tb").visible()
        assertEquals(listOf("old\ttext", "b"), r.map { it.text })
        assertEquals(0L, r.first().createdAt)
        assertTrue(r.first().pinned)
    }
}
