package com.sabin.notes

/** Builds notes from shared text and plain text for sharing a note out. No Android imports. */
object ShareText {
    const val MAX_LENGTH = 20_000

    /** Note text for an incoming ACTION_SEND (subject as title line, then body), or null if nothing usable. */
    fun incoming(subject: String?, text: String?): String? {
        val title = subject?.trim().orEmpty()
        val body = text?.trim().orEmpty()
        return listOf(title, body).filter { it.isNotEmpty() }.joinToString("\n")
            .take(MAX_LENGTH).takeIf { it.isNotBlank() }
    }

    fun outgoing(note: Note): String =
        if (!note.checklist) note.text
        else Checklist.items(note.text).withIndex().joinToString("\n") { (i, item) ->
            "- [${if (i in note.checked) "x" else " "}] $item"
        }
}
