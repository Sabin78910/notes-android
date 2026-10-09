package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistTest {
    private fun checklist(text: String = "a\nb\n\nc"): Pair<NoteStore, Long> {
        val s = NoteStore(); val n = s.add(text); s.setChecklist(n.id, true)
        return s to n.id
    }

    @Test fun itemsSkipBlankLines() {
        val (s, _) = checklist()
        assertEquals(listOf("a", "b", "c"), Checklist.items(s.visible().single().text))
    }

    @Test fun progressCountsTickedItems() {
        val (s, id) = checklist()
        s.toggleItem(id, 0); s.toggleItem(id, 2)
        val p = Checklist.progress(s.visible().single())
        assertEquals(2, p.done); assertEquals(3, p.total)
        assertEquals("2/3 done", p.label)
        assertFalse(p.complete)
    }

    @Test fun toggleTwiceUnticks() {
        val (s, id) = checklist()
        s.toggleItem(id, 1); s.toggleItem(id, 1)
        assertEquals(0, Checklist.progress(s.visible().single()).done)
    }

    @Test fun toggleOutOfRangeIsIgnored() {
        val (s, id) = checklist()
        s.toggleItem(id, 9); s.toggleItem(id, -1)
        assertEquals(0, Checklist.progress(s.visible().single()).done)
    }

    @Test fun completeWhenAllDone() {
        val (s, id) = checklist("x\ny")
        s.toggleItem(id, 0); s.toggleItem(id, 1)
        assertTrue(Checklist.progress(s.visible().single()).complete)
    }

    @Test fun emptyChecklistIsNotComplete() {
        assertFalse(Checklist.Progress(0, 0).complete)
    }

    @Test fun celebrationSkippedWhenAnimationsDisabled() {
        assertTrue(Checklist.shouldCelebrate(Checklist.Progress(2, 2), animationsEnabled = true))
        assertFalse(Checklist.shouldCelebrate(Checklist.Progress(2, 2), animationsEnabled = false))
        assertFalse(Checklist.shouldCelebrate(Checklist.Progress(1, 2), animationsEnabled = true))
    }

    @Test fun persistsModeAndTicks() {
        val (s, id) = checklist()
        s.toggleItem(id, 0); s.toggleItem(id, 2)
        val n = NoteStore.deserialize(s.serialize()).visible().single()
        assertTrue(n.checklist)
        assertEquals(setOf(0, 2), n.checked)
    }

    @Test fun oldDataLoadsAsPlainNote() {
        val n = NoteStore.deserialize("1\ttrue:5\thello").visible().single()
        assertFalse(n.checklist); assertTrue(n.checked.isEmpty()); assertEquals(5L, n.createdAt)
    }

    @Test fun switchingBackToPlainClearsTicks() {
        val (s, id) = checklist()
        s.toggleItem(id, 0); s.setChecklist(id, false)
        val n = s.visible().single()
        assertFalse(n.checklist); assertTrue(n.checked.isEmpty())
    }

    @Test fun editDropsTicksBeyondNewItemCount() {
        val (s, id) = checklist("a\nb\nc")
        s.toggleItem(id, 2); s.edit(id, "a\nb")
        assertTrue(s.visible().single().checked.isEmpty())
    }
}
