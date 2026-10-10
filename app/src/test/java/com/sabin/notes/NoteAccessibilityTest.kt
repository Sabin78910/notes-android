package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteAccessibilityTest {
    private val labels = NoteAccessibility.Labels(pinned = "Pinned", colour = { "Colour: $it" }, tags = { "Tags: $it" })

    @Test fun plainNoteIsJustItsTitle() {
        assertEquals("Milk", NoteAccessibility.describe("Milk", false, null, emptyList(), labels))
    }

    @Test fun includesPinnedColourAndTagsInOrder() {
        assertEquals(
            "Milk. Pinned. Colour: Red. Tags: work, home",
            NoteAccessibility.describe("Milk", true, "Red", listOf("work", "home"), labels)
        )
    }

    @Test fun usesOnlyFirstLineTruncatedAsTitle() {
        val long = "x".repeat(100)
        assertEquals("Line one", NoteAccessibility.title("Line one\nline two"))
        assertEquals("x".repeat(NoteAccessibility.MAX_TITLE) + "…", NoteAccessibility.title(long))
    }

    @Test fun blankTextHasEmptyTitle() {
        assertEquals("", NoteAccessibility.title("  \n "))
    }
}
