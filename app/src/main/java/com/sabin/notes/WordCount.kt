package com.sabin.notes

/** Word and character counting for the editor; plain Kotlin, no Android imports. */
object WordCount {
    data class Counts(val words: Int, val characters: Int)

    fun of(text: String): Counts {
        val body = text.trimEnd('\n', '\r')
        val words = body.split(Regex("[\\s\\u00A0\\u2007\\u202F]+")).count { it.isNotEmpty() }
        return Counts(words, body.codePointCount(0, body.length))
    }

    /** Progress for a checklist being edited: items come from [text], done flags from [checked] indices. */
    fun checklistProgress(text: String, checked: Set<Int>): Checklist.Progress {
        val total = Checklist.items(text).size
        return Checklist.Progress(checked.count { it in 0 until total }, total)
    }
}
