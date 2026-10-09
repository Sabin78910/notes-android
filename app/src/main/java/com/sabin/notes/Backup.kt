package com.sabin.notes

/** JSON backup/restore of notes; plain Kotlin (org.json is unavailable in unit tests). */
object Backup {
    sealed class Result {
        data class Imported(val added: Int) : Result()
        data class Error(val message: String) : Result()
    }

    const val VERSION = 1

    fun export(notes: List<Note>): String = buildString {
        append("{\"version\":$VERSION,\"notes\":[")
        notes.forEachIndexed { i, n ->
            if (i > 0) append(',')
            append("{\"text\":").append(quote(n.text))
            append(",\"pinned\":${n.pinned},\"createdAt\":${n.createdAt},\"checklist\":${n.checklist}")
            append(",\"checked\":[${n.checked.sorted().joinToString(",")}]")
            append(",\"color\":").append(n.color?.let { quote(it.name) } ?: "null")
            append(",\"archived\":${n.archived},\"trashedAt\":${n.trashedAt ?: "null"}")
            append(",\"tags\":[${n.tags.joinToString(",") { quote(it) }}]}")
        }
        append("]}")
    }

    /** Merges [json] into [store]; notes already present (same text and creation time) are skipped. Nothing changes on error. */
    fun import(store: NoteStore, json: String): Result {
        val parsed = try {
            parseNotes(json)
        } catch (e: IllegalArgumentException) {
            return Result.Error("Not a valid notes backup")
        }
        val known = store.all().map { it.text to it.createdAt }.toMutableSet()
        var added = 0
        for (n in parsed) if (known.add(n.text to n.createdAt)) { store.addImported(n); added++ }
        return Result.Imported(added)
    }

    private fun parseNotes(json: String): List<Note> {
        val root = Parser(json).parseDocument() as? Map<*, *> ?: bad()
        if (root["version"] != VERSION.toLong()) bad()
        val list = root["notes"] as? List<*> ?: bad()
        return list.map { item ->
            val m = item as? Map<*, *> ?: bad()
            Note(
                id = 0,
                text = (m["text"] as? String)?.takeIf { it.isNotBlank() } ?: bad(),
                pinned = m["pinned"] as? Boolean ?: false,
                createdAt = m["createdAt"] as? Long ?: 0L,
                checklist = m["checklist"] as? Boolean ?: false,
                checked = (m["checked"] as? List<*>)?.map { (it as? Long ?: bad()).toInt() }?.toSet() ?: emptySet(),
                color = NoteColor.fromName(m["color"] as? String),
                archived = m["archived"] as? Boolean ?: false,
                trashedAt = m["trashedAt"] as? Long,
                tags = (m["tags"] as? List<*>)?.map { it as? String ?: bad() }?.let { Tags.parse(it.joinToString(" ")) } ?: emptySet()
            )
        }
    }

    private fun bad(): Nothing = throw IllegalArgumentException("bad backup")

    private fun quote(s: String): String = buildString {
        append('"')
        for (c in s) when {
            c == '"' -> append("\\\"")
            c == '\\' -> append("\\\\")
            c == '\n' -> append("\\n")
            c == '\r' -> append("\\r")
            c == '\t' -> append("\\t")
            c < ' ' -> append("\\u%04x".format(c.code))
            else -> append(c)
        }
        append('"')
    }

    /** Minimal strict JSON parser: objects, arrays, strings, integers, booleans, null. */
    private class Parser(private val s: String) {
        private var i = 0

        fun parseDocument(): Any? {
            val v = value()
            ws()
            if (i != s.length) bad()
            return v
        }

        private fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }

        private fun peek(): Char = if (i < s.length) s[i] else bad()

        private fun expect(c: Char) { if (peek() != c) bad(); i++ }

        private fun value(): Any? {
            ws()
            return when (val c = peek()) {
                '{' -> obj()
                '[' -> arr()
                '"' -> str()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                else -> if (c == '-' || c.isDigit()) num() else bad()
            }
        }

        private fun literal(word: String, v: Any?): Any? {
            if (!s.startsWith(word, i)) bad()
            i += word.length
            return v
        }

        private fun num(): Long {
            val start = i
            if (peek() == '-') i++
            while (i < s.length && s[i].isDigit()) i++
            return s.substring(start, i).toLongOrNull() ?: bad()
        }

        private fun obj(): Map<String, Any?> {
            expect('{'); ws()
            val m = LinkedHashMap<String, Any?>()
            if (peek() == '}') { i++; return m }
            while (true) {
                ws(); val k = str(); ws(); expect(':')
                m[k] = value(); ws()
                if (peek() == ',') i++ else { expect('}'); return m }
            }
        }

        private fun arr(): List<Any?> {
            expect('['); ws()
            val l = ArrayList<Any?>()
            if (peek() == ']') { i++; return l }
            while (true) {
                l.add(value()); ws()
                if (peek() == ',') i++ else { expect(']'); return l }
            }
        }

        private fun str(): String {
            expect('"')
            val sb = StringBuilder()
            while (true) {
                val c = peek(); i++
                when {
                    c == '"' -> return sb.toString()
                    c == '\\' -> {
                        when (val e = peek()) {
                            '"', '\\', '/' -> sb.append(e)
                            'n' -> sb.append('\n'); 'r' -> sb.append('\r'); 't' -> sb.append('\t')
                            'b' -> sb.append('\b'); 'f' -> sb.append('\u000c')
                            'u' -> {
                                if (i + 5 > s.length) bad()
                                sb.append((s.substring(i + 1, i + 5).toIntOrNull(16) ?: bad()).toChar())
                                i += 4
                            }
                            else -> bad()
                        }
                        i++
                    }
                    c < ' ' -> bad()
                    else -> sb.append(c)
                }
            }
        }
    }
}
