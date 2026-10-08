package com.sabin.notes

data class Note(val id: Long, val text: String, val pinned: Boolean = false)

/** Pure note logic; serialization is a simple line format so it persists without extra libraries. */
class NoteStore(initial: List<Note> = emptyList()) {
    private val notes = initial.toMutableList()
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    fun add(text: String): Note {
        require(text.isNotBlank()) { "Note is empty" }
        return Note(nextId++, text.trim()).also { notes.add(it) }
    }

    fun delete(id: Long) { notes.removeAll { it.id == id } }

    fun togglePin(id: Long) {
        val i = notes.indexOfFirst { it.id == id }
        if (i >= 0) notes[i] = notes[i].copy(pinned = !notes[i].pinned)
    }

    /** Pinned first, newest first; filtered by case-insensitive query. */
    fun visible(query: String = ""): List<Note> =
        notes.filter { it.text.contains(query.trim(), ignoreCase = true) }
            .sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.id })

    fun serialize(): String = notes.joinToString("\n") {
        "${it.id}\t${it.pinned}\t${it.text.replace("\\", "\\\\").replace("\n", "\\n")}"
    }

    companion object {
        fun deserialize(data: String): NoteStore = NoteStore(
            data.lines().filter { it.isNotBlank() }.mapNotNull { line ->
                val parts = line.split("\t", limit = 3)
                if (parts.size < 3) return@mapNotNull null
                val text = parts[2].replace(Regex("\\\\(.)")) { if (it.groupValues[1] == "n") "\n" else it.groupValues[1] }
                Note(parts[0].toLongOrNull() ?: return@mapNotNull null, text, parts[1].toBoolean())
            }
        )
    }
}
