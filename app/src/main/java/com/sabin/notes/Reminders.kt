package com.sabin.notes

/** Platform alarm hook so scheduling logic stays free of Android imports. */
interface AlarmGateway {
    fun set(noteId: Long, at: Long)
    fun cancel(noteId: Long)
}

class ReminderScheduler(private val alarms: AlarmGateway) {
    /** Schedules the note's reminder if it is still upcoming and the note is live; otherwise cancels it. */
    fun sync(note: Note, now: Long = System.currentTimeMillis()) {
        val at = note.remindAt
        if (at != null && at > now && note.trashedAt == null && !note.archived) alarms.set(note.id, at) else alarms.cancel(note.id)
    }

    /** Used after reboot or when reminders are switched on. */
    fun rescheduleAll(notes: List<Note>, now: Long = System.currentTimeMillis()) = notes.forEach { sync(it, now) }

    fun cancelAll(notes: List<Note>) = notes.forEach { alarms.cancel(it.id) }
}
