package com.sabin.notes

/** Formats a single note as Markdown for export to a file. No Android imports. */
object NoteExport {
    private const val MAX_NAME = 60

    fun toMarkdown(note: Note): String {
        val out = StringBuilder()
        if (note.checklist) {
            Checklist.items(note.text).forEachIndexed { i, item ->
                out.append("- [${if (i in note.checked) "x" else " "}] $item\n")
            }
        } else {
            val title = note.text.lineSequence().first().trim()
            val body = note.text.lines().drop(1).joinToString("\n").trim()
            if (title.isNotEmpty()) out.append("# $title\n")
            if (body.isNotEmpty()) out.append("\n").append(body).append("\n")
        }
        if (note.tags.isNotEmpty()) {
            if (out.isNotEmpty()) out.append("\n")
            out.append("Tags: ").append(note.tags.joinToString(" ") { "#$it" }).append("\n")
        }
        return out.toString()
    }

    /** Suggested file name from the first line of [text]; falls back to "note". */
    fun fileName(text: String): String {
        val base = text.lineSequence().first().trim()
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
            .take(MAX_NAME).trim().trim('_', '.')
        return (base.ifEmpty { "note" }) + ".md"
    }
}
