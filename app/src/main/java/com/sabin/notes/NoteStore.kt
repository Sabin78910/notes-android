package com.sabin.notes

data class Note(val id: Long, val text: String, val pinned: Boolean = false, val createdAt: Long = 0L,
    val checklist: Boolean = false,
    /** Indices (into [Checklist.items]) of ticked items. */
    val checked: Set<Int> = emptySet(),
    val color: NoteColor? = null,
    val archived: Boolean = false,
    /** Epoch millis when moved to Trash; null if not trashed. */
    val trashedAt: Long? = null,
    /** Lowercase labels, see [Tags]. */
    val tags: Set<String> = emptySet(),
    /** Epoch millis of the reminder; null if none. */
    val remindAt: Long? = null
)

object Tags {
    /** Splits on whitespace/commas, strips '#', lowercases and drops characters outside [a-z0-9_-]. */
    fun parse(input: String): Set<String> =
        input.split(Regex("[\\s,]+")).map { t -> t.lowercase().filter { it in 'a'..'z' || it in '0'..'9' || it == '_' || it == '-' } }
            .filter { it.isNotEmpty() }.toSortedSet()
}

/** Checklist helpers: each non-blank line of a checklist note is one item. */
object Checklist {
    data class Progress(val done: Int, val total: Int) {
        val label: String get() = "$done/$total done"
        val complete: Boolean get() = total > 0 && done == total
        val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total
    }

    data class Entry(val index: Int, val text: String)
    data class Sections(val open: List<Entry>, val done: List<Entry>)

    /** Splits items into open and Done groups; [Entry.index] is the item's index for [NoteStore.toggleItem]. */
    fun sections(note: Note): Sections {
        val (done, open) = items(note.text).mapIndexed { i, t -> Entry(i, t) }.partition { it.index in note.checked }
        return Sections(open, done)
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

    fun setReminder(id: Long, at: Long) {
        val i = notes.indexOfFirst { it.id == id }
        if (i >= 0) notes[i] = notes[i].copy(remindAt = at)
    }

    fun clearReminder(id: Long) {
        val i = notes.indexOfFirst { it.id == id }
        if (i >= 0) notes[i] = notes[i].copy(remindAt = null)
    }

    fun addTags(id: Long, raw: String) {
        val i = notes.indexOfFirst { it.id == id }
        if (i >= 0) notes[i] = notes[i].copy(tags = notes[i].tags + Tags.parse(raw))
    }

    fun removeTag(id: Long, tag: String) {
        val i = notes.indexOfFirst { it.id == id }
        if (i >= 0) notes[i] = notes[i].copy(tags = notes[i].tags - tag)
    }

    /** Sorted distinct tags across notes that are not in Trash. */
    fun allTags(): List<String> = notes.filter { it.trashedAt == null }.flatMap { it.tags }.distinct().sorted()

    /** Snapshot of every note, including archived and trashed ones. */
    fun all(): List<Note> = notes.toList()

    /** Adds a note from a backup under a fresh id. */
    fun addImported(note: Note) { notes.add(note.copy(id = nextId++)) }

    /** Adds a copy of the note (see [NoteDuplicate]); null if [id] is unknown. */
    fun duplicate(id: Long, now: Long = System.currentTimeMillis()): Note? {
        val src = notes.firstOrNull { it.id == id } ?: return null
        return NoteDuplicate.copyOf(src, nextId++, now).also { notes.add(it) }
    }

    fun toggleItem(id: Long, index: Int) {
        val i = notes.indexOfFirst { it.id == id }
        if (i < 0 || !notes[i].checklist || index !in Checklist.items(notes[i].text).indices) return
        val c = notes[i].checked
        notes[i] = notes[i].copy(checked = if (index in c) c - index else c + index)
    }

    /** Moves the note to Trash; it is purged after [TRASH_RETENTION_MS]. */
    fun delete(id: Long, now: Long = System.currentTimeMillis()) {
        val i = notes.indexOfFirst { it.id == id }
        if (i >= 0) notes[i] = notes[i].copy(trashedAt = now, archived = false)
    }

    /** Brings a deleted note back (from Trash, or re-inserts it if it is gone entirely). */
    fun restore(note: Note) {
        val i = notes.indexOfFirst { it.id == note.id }
        if (i < 0) notes.add(note.copy(trashedAt = null)) else if (notes[i].trashedAt != null) notes[i] = notes[i].copy(trashedAt = null)
        if (note.id >= nextId) nextId = note.id + 1
    }

    fun restoreFromTrash(id: Long) {
        val i = notes.indexOfFirst { it.id == id }
        if (i >= 0) notes[i] = notes[i].copy(trashedAt = null)
    }

    fun archive(id: Long) = setArchived(id, true)

    fun unarchive(id: Long) = setArchived(id, false)

    private fun setArchived(id: Long, on: Boolean) {
        val i = notes.indexOfFirst { it.id == id }
        if (i >= 0 && notes[i].trashedAt == null) notes[i] = notes[i].copy(archived = on)
    }

    fun emptyTrash() { notes.removeAll { it.trashedAt != null } }

    /** Permanently removes notes trashed for more than 30 days. */
    fun purgeExpired(now: Long = System.currentTimeMillis()) {
        notes.removeAll { n -> n.trashedAt?.let { now - it > TRASH_RETENTION_MS } == true }
    }

    fun archived(): List<Note> = notes.filter { it.archived && it.trashedAt == null }.sortedByDescending { it.id }

    fun trashed(): List<Note> = notes.filter { it.trashedAt != null }.sortedByDescending { it.trashedAt }

    fun togglePin(id: Long) {
        val i = notes.indexOfFirst { it.id == id }
        if (i >= 0) notes[i] = notes[i].copy(pinned = !notes[i].pinned)
    }

    /** Pinned first, then newest (or oldest) first; filtered by case-insensitive query. */
    fun visible(query: String = "", newestFirst: Boolean = true, color: NoteColor? = null, tag: String? = null): List<Note> =
        notes.filter { !it.archived && it.trashedAt == null && it.text.contains(query.trim(), ignoreCase = true) && (color == null || it.color == color) && (tag == null || tag in it.tags) }
            .sortedWith(
                compareByDescending<Note> { it.pinned }
                    .let { if (newestFirst) it.thenByDescending { n -> n.id } else it.thenBy { n -> n.id } }
            )

    fun serialize(): String = notes.joinToString("\n") {
        "${it.id}\t${it.pinned}:${it.createdAt}:${it.checklist}:${it.checked.sorted().joinToString(",")}:${it.color?.name.orEmpty()}:${it.archived}:${it.trashedAt ?: ""}:${it.tags.joinToString(",")}:${it.remindAt ?: ""}\t${it.text.replace("\\", "\\\\").replace("\n", "\\n")}"
    }

    companion object {
        const val TRASH_RETENTION_MS = 30L * 24 * 60 * 60 * 1000

        fun deserialize(data: String): NoteStore = NoteStore(
            data.lines().filter { it.isNotBlank() }.mapNotNull { line ->
                val parts = line.split("\t", limit = 3)
                if (parts.size < 3) return@mapNotNull null
                val text = parts[2].replace(Regex("\\\\(.)")) { if (it.groupValues[1] == "n") "\n" else it.groupValues[1] }
                // Old data has just "pinned"; new data has "pinned:createdAt" (0 = unknown).
                val meta = parts[1].split(":", limit = 9)
                Note(
                    parts[0].toLongOrNull() ?: return@mapNotNull null,
                    text,
                    meta[0].toBoolean(),
                    meta.getOrNull(1)?.toLongOrNull() ?: 0L,
                    meta.getOrNull(2).toBoolean(),
                    meta.getOrNull(3).orEmpty().split(",").mapNotNull { it.toIntOrNull() }.toSet(),
                    NoteColor.fromName(meta.getOrNull(4)),
                    meta.getOrNull(5).toBoolean(),
                    meta.getOrNull(6)?.toLongOrNull(),
                    Tags.parse(meta.getOrNull(7).orEmpty()),
                    meta.getOrNull(8)?.toLongOrNull()
                )
            }
        )
    }
}
