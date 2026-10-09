package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {
    private fun sample(): NoteStore {
        val s = NoteStore()
        val a = s.add("Line \"one\"\nline two \\ é 😀", now = 100)
        s.add("second", now = 200)
        s.togglePin(a.id); s.setChecklist(a.id, true); s.toggleItem(a.id, 1); s.setColor(a.id, NoteColor.entries.first()); s.addTags(a.id, "work ideas")
        return s
    }

    @Test fun roundTripPreservesNotes() {
        val s = sample()
        val restored = NoteStore()
        val result = Backup.import(restored, Backup.export(s.all()))
        assertEquals(2, (result as Backup.Result.Imported).added)
        assertEquals(s.all().map { it.copy(id = 0) }.sortedBy { it.createdAt }, restored.all().map { it.copy(id = 0) }.sortedBy { it.createdAt })
    }

    @Test fun importMergesWithoutDuplicates() {
        val s = sample()
        val json = Backup.export(s.all())
        val result = Backup.import(s, json) as Backup.Result.Imported
        assertEquals(0, result.added)
        assertEquals(2, s.all().size)
        val other = NoteStore(); other.add("third", now = 300)
        assertEquals(2, (Backup.import(other, json) as Backup.Result.Imported).added)
        assertEquals(3, other.all().size)
        assertEquals(3, other.all().map { it.id }.distinct().size)
    }

    @Test fun malformedFileChangesNothing() {
        val s = sample(); val before = s.serialize()
        for (bad in listOf("", "not json", "{\"notes\": [", "[1,2]", "{\"version\":1}", "{\"version\":1,\"notes\":[{\"text\":5}]}", "{\"version\":1,\"notes\":[{\"text\":\"ok\",\"createdAt\":1},{\"bad\":1}]}")) {
            assertTrue(bad, Backup.import(s, bad) is Backup.Result.Error)
            assertEquals(before, s.serialize())
        }
    }
}
