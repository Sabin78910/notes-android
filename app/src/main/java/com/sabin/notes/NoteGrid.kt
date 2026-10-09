package com.sabin.notes

/** How the note wall is laid out; persisted by [name]. */
enum class LayoutMode(val columns: Int) {
    GRID(2), LIST(1);

    fun toggled(): LayoutMode = if (this == GRID) LIST else GRID
    fun toggleDescription(): String = if (this == GRID) "Switch to list view" else "Switch to grid view"

    companion object {
        fun fromName(name: String?): LayoutMode = entries.firstOrNull { it.name == name } ?: GRID
    }
}

/** Trimmed text shown on a card so staggered cards stay a sensible height. */
object NotePreview {
    const val MAX_CHARS = 140
    const val MAX_LINES = 6
    const val MAX_ITEMS = 4

    fun text(note: Note): String {
        val lines = note.text.trim().lines()
        var out = lines.take(MAX_LINES).joinToString("\n")
        var cut = lines.size > MAX_LINES
        if (out.length > MAX_CHARS) { out = out.take(MAX_CHARS); cut = true }
        return if (cut) "$out…" else out
    }

    fun checklistItems(note: Note): List<String> = Checklist.items(note.text).take(MAX_ITEMS)

    fun hiddenItems(note: Note): Int = (Checklist.items(note.text).size - MAX_ITEMS).coerceAtLeast(0)
}

object EmptyState {
    fun message(view: String, filtered: Boolean): String = when {
        view == "Archive" -> "Archive is empty"
        view == "Trash" -> "Trash is empty"
        filtered -> "No matching notes"
        else -> "No notes yet — tap New note to add one"
    }

    fun helper(view: String, filtered: Boolean): String = when {
        view == "Archive" -> "Archived notes show up here."
        view == "Trash" -> "Deleted notes stay here for 30 days."
        filtered -> "Try a different search or clear the filters."
        else -> "Capture ideas, lists and reminders."
    }

    fun icon(view: String, filtered: Boolean): String = when {
        view == "Trash" -> "🗑️"
        view == "Archive" -> "📦"
        filtered -> "🔍"
        else -> "📝"
    }

    fun showNewNoteAction(view: String, filtered: Boolean): Boolean = view == "Notes" && !filtered
}

object SortOrder {
    fun description(newestFirst: Boolean): String = if (newestFirst) "Sort: oldest first" else "Sort: newest first"
    fun glyph(newestFirst: Boolean): String = if (newestFirst) "↓" else "↑"
}
