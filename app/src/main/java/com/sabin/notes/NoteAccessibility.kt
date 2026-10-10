package com.sabin.notes

/** Builds the single spoken description for a note card; localised pieces are passed in. */
object NoteAccessibility {
    const val MAX_TITLE = 60

    class Labels(val pinned: String, val colour: (String) -> String, val tags: (String) -> String)

    fun title(text: String): String {
        val line = text.trim().lineSequence().first().trim()
        return if (line.length > MAX_TITLE) line.take(MAX_TITLE) + "…" else line
    }

    fun describe(text: String, pinned: Boolean, colourLabel: String?, tags: Collection<String>, labels: Labels): String =
        listOfNotNull(
            title(text).ifEmpty { null },
            if (pinned) labels.pinned else null,
            colourLabel?.let(labels.colour),
            if (tags.isEmpty()) null else labels.tags(tags.joinToString(", "))
        ).joinToString(". ")
}
