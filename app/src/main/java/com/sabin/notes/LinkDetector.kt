package com.sabin.notes

/** Finds http/https URLs in note text. No Android imports. */
object LinkDetector {
    private val URL = Regex("""https?://[^\s<>"]+""", RegexOption.IGNORE_CASE)
    private const val TRAILING = ".,;:!?)]}'"

    /** Index ranges (inclusive) of each URL in [text], excluding trailing punctuation. */
    fun find(text: String): List<IntRange> = URL.findAll(text).mapNotNull { m ->
        var end = m.range.last
        while (end >= m.range.first && text[end] in TRAILING) end--
        val range = m.range.first..end
        range.takeIf { text.substring(it.first, it.last + 1).substringAfter("://").isNotEmpty() }
    }.toList()
}
