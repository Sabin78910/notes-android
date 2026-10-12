package com.sabin.notes

/** Where a search query matches inside note text; the UI only renders these ranges. */
object SearchHighlight {
    /** Case-insensitive, literal, merged and sorted match ranges (inclusive) for each word of [query]. */
    fun ranges(text: String, query: String): List<IntRange> {
        val words = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.distinct()
        if (words.isEmpty()) return emptyList()
        val found = words.flatMap { w ->
            val out = mutableListOf<IntRange>()
            var i = text.indexOf(w, 0, ignoreCase = true)
            while (i >= 0) {
                out += i until i + w.length
                i = text.indexOf(w, i + 1, ignoreCase = true)
            }
            out
        }.sortedBy { it.first }
        val merged = mutableListOf<IntRange>()
        for (r in found) {
            val last = merged.lastOrNull()
            if (last != null && r.first <= last.last + 1) merged[merged.lastIndex] = last.first..maxOf(last.last, r.last)
            else merged += r
        }
        return merged
    }

    /** Offset to start the card preview from: the line of the first match when it would be clipped, else 0. */
    fun previewStart(text: String, query: String, maxChars: Int): Int {
        val first = ranges(text, query).firstOrNull()?.first ?: return 0
        if (first < maxChars) return 0
        return text.lastIndexOf('\n', first) + 1
    }
}
