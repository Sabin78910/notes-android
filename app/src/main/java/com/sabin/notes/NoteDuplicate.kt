package com.sabin.notes

/** Builds a copy of a note for the Duplicate action. No Android imports. */
object NoteDuplicate {
    /** Same text/checklist/colour/tags; unpinned, no reminder, active (not archived or trashed). */
    fun copyOf(note: Note, newId: Long, now: Long): Note = note.copy(
        id = newId, createdAt = now, pinned = false, remindAt = null, archived = false, trashedAt = null
    )
}
