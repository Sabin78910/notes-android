package com.sabin.notes

data class Note(val id: Long, val text: String, val pinned: Boolean = false, val createdAt: Long = 0L,
    val checklist: Boolean = false,
    /** Indices (into [Checklist.items]) of ticked items. */
    val checked: Set<Int> = emptySet(),
    val color: NoteColor? = null
)

/** Checklist helpers: each non-blank line of a checklist note is one item. */
object Checklist {
    data class Progress(val done: Int, val total: Int) {
        val label: String get() = "$done/$total done"
        val complete: Boolean get() = total > 0 && done == total
    }

    fun items(text: String): List<String> = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

    fun progress(note: Note): Progress {
        val total = items(note.text).size
        return Progress(note.checked.count { it in 0 until total }, total)
    }

    fun shouldCelebrate(progress: Progress, animationsEnabled: Boolean) = animationsEnabled && progress.complete
}

/** Pure note logic; serialization is a simple line format so it persists without extra libraries. */
class NoteStore(initial: List<Note> = emptyList()) {
    private val notes = initial.toMutableList()
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    fun add(text: String, now: Long = System.currentTimeMillis()): Note {
        require(text.isNotBlank()) { "Note is empty" }
        return Note(nextId++, text.trim(), createdAt = now).also { notes.add(it) }
    }

    fun edit(id: Long, text: String) {
        require(text.isNotBlank()) { "Note is empty" }
        val i = notes.indexOfFirst { it.id == id }
        if (i >= 0) {
            val count = Checklist.items(text).size
            notes[i] = notes[i].copy(text = text.trim(), checked = notes[i].checked.filter { it < count }.toSet())
        }
    }

    fun setChecklist(id: Long, on: Boolean) {
        val i = notes.indexOfFirst { it.id == id }
        if (i >= 0) notes[i] = notes[i].copy(checklist = on, checked = emptySet())
    }

    fun setColor(id: Long, color: NoteColor?) {
        val i = notes.indexOfFirst { it.id == id }
        if (i >= 0) notes[i] = notes[i].copy(color = color)
    }

    fun toggleItem(id: Long, index: Int) {
        val i = notes.indexOfFirst { it.id == id }
        if (i < 0 || !notes[i].checklist || index !in Checklist.items(notes[i].text).indices) return
        val c = notes[i].checked
        notes[i] = notes[i].copy(checked = if (index in c) c - index else c + index)
    }

    fun delete(id: Long) { notes.removeAll { it.id == id } }

    /** Re-inserts a previously deleted note (same id, so order and pin are preserved). */
    fun restore(note: Note) {
        if (notes.none { it.id == note.id }) notes.add(note)
        if (note.id >= nextId) nextId = note.id + 1
    }

    fun togglePin(id: Long) {
        val i = notes.indexOfFirst { it.id == id }
        if (i >= 0) notes[i] = notes[i].copy(pinned = !notes[i].pinned)
    }

    /** Pinned first, then newest (or oldest) first; filtered by case-insensitive query. */
    fun visible(query: String = "", newestFirst: Boolean = true, color: NoteColor? = null): List<Note> =
        notes.filter { it.text.contains(query.trim(), ignoreCase = true) && (color == null || it.color == color) }
            .sortedWith(
                compareByDescending<Note> { it.pinned }
                    .let { if (newestFirst) it.thenByDescending { n -> n.id } else it.thenBy { n -> n.id } }
            )

    fun serialize(): String = notes.joinToString("\n") {
        "${it.id}\t${it.pinned}:${it.createdAt}:${it.checklist}:${it.checked.sorted().joinToString(",")}:${it.color?.name.orEmpty()}\t${it.text.replace("\\", "\\\\").replace("\n", "\\n")}"
    }

    companion object {
        fun deserialize(data: String): NoteStore = NoteStore(
            data.lines().filter { it.isNotBlank() }.mapNotNull { line ->
                val parts = line.split("\t", limit = 3)
                if (parts.size < 3) return@mapNotNull null
                val text = parts[2].replace(Regex("\\\\(.)")) { if (it.groupValues[1] == "n") "\n" else it.groupValues[1] }
                // Old data has just "pinned"; new data has "pinned:createdAt" (0 = unknown).
                val meta = parts[1].split(":", limit = 5)
                Note(
                    parts[0].toLongOrNull() ?: return@mapNotNull null,
                    text,
                    meta[0].toBoolean(),
                    meta.getOrNull(1)?.toLongOrNull() ?: 0L,
                    meta.getOrNull(2).toBoolean(),
                    meta.getOrNull(3).orEmpty().split(",").mapNotNull { it.toIntOrNull() }.toSet(),
                    NoteColor.fromName(meta.getOrNull(4))
                )
            }
        )
    }
}
