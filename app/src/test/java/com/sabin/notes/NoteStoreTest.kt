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

    @Test fun addStoresCreatedAt() {
        val a = NoteStore().add("x", now = 1234L)
        assertEquals(1234L, a.createdAt)
    }

    @Test fun createdAtSurvivesSerialization() {
        val s = NoteStore(); val a = s.add("x\ty", now = 5555L); s.togglePin(a.id)
        val n = NoteStore.deserialize(s.serialize()).visible().single()
        assertEquals(5555L, n.createdAt)
        assertEquals("x\ty", n.text)
        assertTrue(n.pinned)
    }

    @Test fun deserializesOldFormatWithoutCreatedAt() {
        val n = NoteStore.deserialize("1\ttrue\told\ttext\n2\tfalse\tb").visible()
        assertEquals(listOf(1L, 2L), n.map { it.id }.sorted())
        assertEquals(0L, n.first { it.id == 1L }.createdAt)
        assertEquals("old\ttext", n.first { it.id == 1L }.text)
        assertTrue(n.first { it.id == 1L }.pinned)
    }

    @Test fun newestFirstIsDefaultAndKeepsPinnedOnTop() {
        val s = NoteStore(); val a = s.add("a"); s.add("b"); s.add("c"); s.togglePin(a.id)
        assertEquals(listOf("a", "c", "b"), s.visible().map { it.text })
        assertEquals(listOf("a", "c", "b"), s.visible(newestFirst = true).map { it.text })
    }

    @Test fun oldestFirstKeepsPinnedOnTop() {
        val s = NoteStore(); s.add("a"); s.add("b"); val c = s.add("c"); s.togglePin(c.id)
        assertEquals(listOf("c", "a", "b"), s.visible(newestFirst = false).map { it.text })
    }

    @Test fun oldestFirstWithSearch() {
        val s = NoteStore(); s.add("x1"); s.add("y"); s.add("x2")
        assertEquals(listOf("x1", "x2"), s.visible("x", newestFirst = false).map { it.text })
    }

    @Test fun archiveHidesFromVisibleAndUnarchiveBringsBack() {
        val s = NoteStore(); val a = s.add("x")
        s.archive(a.id)
        assertTrue(s.visible().isEmpty())
        assertEquals(listOf("x"), s.archived().map { it.text })
        s.unarchive(a.id)
        assertEquals(listOf("x"), s.visible().map { it.text })
        assertTrue(s.archived().isEmpty())
    }

    @Test fun deleteMovesToTrashAndRestoreFromTrashBringsBack() {
        val s = NoteStore(); val a = s.add("x")
        s.delete(a.id, now = 100L)
        assertTrue(s.visible().isEmpty())
        assertEquals(listOf("x"), s.trashed().map { it.text })
        s.restoreFromTrash(a.id)
        assertEquals(listOf("x"), s.visible().map { it.text })
        assertTrue(s.trashed().isEmpty())
    }

    @Test fun cannotArchiveTrashedNote() {
        val s = NoteStore(); val a = s.add("x")
        s.delete(a.id); s.archive(a.id)
        assertTrue(s.archived().isEmpty())
    }

    @Test fun emptyTrashRemovesOnlyTrashed() {
        val s = NoteStore(); val a = s.add("a"); s.add("b")
        s.delete(a.id)
        s.emptyTrash()
        assertTrue(s.trashed().isEmpty())
        assertEquals(listOf("b"), s.visible().map { it.text })
    }

    @Test fun purgeRemovesNotesTrashedOverThirtyDays() {
        val day = 24L * 60 * 60 * 1000
        val s = NoteStore(); val old = s.add("old"); val recent = s.add("recent")
        s.delete(old.id, now = 0L); s.delete(recent.id, now = 10 * day)
        s.purgeExpired(now = 30 * day + 1)
        assertEquals(listOf("recent"), s.trashed().map { it.text })
    }

    @Test fun purgeKeepsNoteTrashedExactlyThirtyDays() {
        val s = NoteStore(); val a = s.add("x")
        s.delete(a.id, now = 0L)
        s.purgeExpired(now = NoteStore.TRASH_RETENTION_MS)
        assertEquals(1, s.trashed().size)
    }

    @Test fun archiveAndTrashSurviveSerialization() {
        val s = NoteStore(); val a = s.add("a"); val b = s.add("b"); s.add("c")
        s.archive(a.id); s.delete(b.id, now = 777L)
        val r = NoteStore.deserialize(s.serialize())
        assertEquals(listOf("a"), r.archived().map { it.text })
        assertEquals(777L, r.trashed().single().trashedAt)
        assertEquals(listOf("c"), r.visible().map { it.text })
    }
}
