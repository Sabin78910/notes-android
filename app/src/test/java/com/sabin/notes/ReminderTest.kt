package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderTest {
    private class FakeAlarms : AlarmGateway {
        val set = mutableMapOf<Long, Long>()
        override fun set(noteId: Long, at: Long) { set[noteId] = at }
        override fun cancel(noteId: Long) { set.remove(noteId) }
    }

    @Test fun setReminderSchedulesAlarm() {
        val store = NoteStore(); val n = store.add("a"); val alarms = FakeAlarms()
        val s = ReminderScheduler(alarms)
        store.setReminder(n.id, 5_000L)
        s.sync(store.all().first(), now = 1_000L)
        assertEquals(mapOf(n.id to 5_000L), alarms.set)
    }

    @Test fun clearingReminderCancelsAlarm() {
        val store = NoteStore(); val n = store.add("a"); val alarms = FakeAlarms()
        val s = ReminderScheduler(alarms)
        store.setReminder(n.id, 5_000L); s.sync(store.all().first(), 1_000L)
        store.clearReminder(n.id); s.sync(store.all().first(), 1_000L)
        assertTrue(alarms.set.isEmpty())
        assertNull(store.all().first().remindAt)
    }

    @Test fun pastTrashedAndArchivedNotesAreNotScheduled() {
        val store = NoteStore(); val alarms = FakeAlarms(); val s = ReminderScheduler(alarms)
        val past = store.add("p"); val trash = store.add("t"); val arch = store.add("r")
        listOf(past, trash, arch).forEach { store.setReminder(it.id, 5_000L) }
        store.delete(trash.id, 0L); store.archive(arch.id)
        s.rescheduleAll(store.all(), now = 6_000L)
        assertTrue(alarms.set.isEmpty())
    }

    @Test fun rescheduleAllRestoresFutureAlarms() {
        val store = NoteStore(); val a = store.add("a"); val b = store.add("b"); store.add("c")
        store.setReminder(a.id, 9_000L); store.setReminder(b.id, 100L)
        val alarms = FakeAlarms()
        ReminderScheduler(alarms).rescheduleAll(store.all(), now = 1_000L)
        assertEquals(mapOf(a.id to 9_000L), alarms.set)
    }

    @Test fun cancelAllRemovesEveryAlarm() {
        val store = NoteStore(); val a = store.add("a"); val b = store.add("b")
        store.setReminder(a.id, 9_000L); store.setReminder(b.id, 8_000L)
        val alarms = FakeAlarms(); val s = ReminderScheduler(alarms)
        s.rescheduleAll(store.all(), 0L); s.cancelAll(store.all())
        assertTrue(alarms.set.isEmpty())
    }

    @Test fun reminderSurvivesSerialization() {
        val store = NoteStore(); val n = store.add("a\tb"); store.setReminder(n.id, 1234L)
        store.addTags(n.id, "x")
        assertEquals(1234L, NoteStore.deserialize(store.serialize()).all().first().remindAt)
    }

    @Test fun oldDataHasNoReminder() {
        assertNull(NoteStore.deserialize("1\tfalse:0\thi").all().first().remindAt)
    }
}
