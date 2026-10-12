package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchHighlightTest {
    @Test fun caseInsensitive() = assertEquals(listOf(0..3), SearchHighlight.ranges("MILK and eggs", "milk"))

    @Test fun multipleMatches() = assertEquals(listOf(0..1, 6..7), SearchHighlight.ranges("ab cd ab", "ab"))

    @Test fun multiWordQueryMatchesEachWord() = assertEquals(listOf(0..3, 9..12), SearchHighlight.ranges("milk and eggs", "eggs milk"))

    @Test fun overlappingWordsMerge() = assertEquals(listOf(0..5), SearchHighlight.ranges("banana", "bana nana"))

    @Test fun repeatedWordsDoNotDuplicate() = assertEquals(listOf(0..1), SearchHighlight.ranges("ab", "ab ab"))

    @Test fun blankQueryReturnsNone() {
        assertTrue(SearchHighlight.ranges("abc", "").isEmpty())
        assertTrue(SearchHighlight.ranges("abc", "   ").isEmpty())
    }

    @Test fun regexCharactersAreLiteral() {
        assertEquals(listOf(1..2), SearchHighlight.ranges("a.*b", ".*"))
        assertTrue(SearchHighlight.ranges("abc", ".").isEmpty())
        assertTrue(SearchHighlight.ranges("abc", "(").isEmpty())
    }

    @Test fun nonAscii() = assertEquals(listOf(0..4, 6..8), SearchHighlight.ranges("ÉCOLE été", "école été"))

    @Test fun previewStartIsZeroWhenMatchVisible() = assertEquals(0, SearchHighlight.previewStart("hello world", "world", 140))

    @Test fun previewStartMovesToLineOfHiddenMatch() {
        val text = "a".repeat(200) + "\nneedle here"
        assertEquals(201, SearchHighlight.previewStart(text, "needle", 140))
    }

    @Test fun previewStartZeroWithoutMatch() = assertEquals(0, SearchHighlight.previewStart("abc", "", 140))
}
