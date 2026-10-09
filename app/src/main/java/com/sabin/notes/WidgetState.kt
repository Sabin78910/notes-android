package com.sabin.notes

/** What the home-screen widget shows; pure so it can be unit tested. */
data class WidgetState(val noteId: Long?, val preview: String, val progress: String?) {
    companion object {
        const val EMPTY_TEXT = "No notes yet"
        const val MAX_PREVIEW = 140

        /** Newest pinned note, else newest visible note, else a placeholder. */
        fun from(notes: List<Note>): WidgetState {
            val note = NoteStore(notes).visible().firstOrNull() ?: return WidgetState(null, EMPTY_TEXT, null)
            val text = note.text.trim()
            val preview = if (text.length > MAX_PREVIEW) text.take(MAX_PREVIEW) + "…" else text
            val progress = if (note.checklist) Checklist.progress(note).label else null
            return WidgetState(note.id, preview, progress)
        }
    }
}

/** Entry points the widget buttons open in the app. */
enum class LaunchAction(val wireName: String) {
    NEW_NOTE("new_note"), NEW_CHECKLIST("new_checklist");

    companion object {
        const val EXTRA = "com.sabin.notes.LAUNCH_ACTION"
        fun fromName(name: String?): LaunchAction? = entries.firstOrNull { it.wireName == name }
    }
}
