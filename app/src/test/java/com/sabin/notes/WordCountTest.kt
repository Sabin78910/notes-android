package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Test

class WordCountTest {
    @Test fun emptyIsZero() = assertEquals(WordCount.Counts(0, 0), WordCount.of(""))

    @Test fun whitespaceOnlyHasNoWords() = assertEquals(0, WordCount.of("  \n\t ").words)

    @Test fun multipleSpacesAndNewlinesSeparateWords() {
        assertEquals(3, WordCount.of("one   two\n\n\nthree").words)
    }

    @Test fun leadingAndTrailingWhitespaceIgnoredForWords() {
        assertEquals(2, WordCount.of("  hello world \n").words)
    }

    @Test fun trailingNewlineNotCounted() {
        assertEquals(5, WordCount.of("hello\n").characters)
        assertEquals(5, WordCount.of("hello\r\n").characters)
    }

    @Test fun spacesAndInnerNewlinesCount() {
        assertEquals(8, WordCount.of("a b\n\nc d").characters)
    }

    @Test fun emojiCountsAsOneCharacter() {
        assertEquals(Pair(2, 5), WordCount.of("hi 😀!").let { it.words to it.characters })
    }

    @Test fun devanagariWords() {
        val c = WordCount.of("नमस्ते   संसार\n")
        assertEquals(2, c.words)
        assertEquals("नमस्ते   संसार".codePointCount(0, "नमस्ते   संसार".length), c.characters)
    }

    @Test fun checklistCounts() {
        assertEquals(Checklist.Progress(1, 2), WordCount.checklistProgress("a\n\nb\n", setOf(0, 5)))
    }
}
