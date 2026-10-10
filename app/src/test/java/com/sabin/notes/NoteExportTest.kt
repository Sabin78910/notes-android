package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteExportTest {
    private fun note(text: String, checklist: Boolean = false, checked: Set<Int> = emptySet(), tags: Set<String> = emptySet()) =
        Note(1, text, checklist = checklist, checked = checked, tags = tags)

    @Test fun plainNoteUsesFirstLineAsTitle() =
        assertEquals("# Groceries\n\nmilk\neggs\n", NoteExport.toMarkdown(note("Groceries\nmilk\neggs")))

    @Test fun checklistWithCheckedAndUnchecked() =
        assertEquals("- [x] a\n- [ ] b\n", NoteExport.toMarkdown(note("a\nb", checklist = true, checked = setOf(0))))

    @Test fun tagsLine() =
        assertEquals("# T\n\nbody\n\nTags: #a #b\n", NoteExport.toMarkdown(note("T\nbody", tags = linkedSetOf("a", "b"))))

    @Test fun emptyTitle() =
        assertEquals("\nbody\n", NoteExport.toMarkdown(note("\nbody")))

    @Test fun emptyBody() =
        assertEquals("# Only title\n", NoteExport.toMarkdown(note("Only title")))

    @Test fun unicodeAndSpecialCharsPreserved() =
        assertEquals("# नमस्ते *café* 😀\n\n<b>&\n", NoteExport.toMarkdown(note("नमस्ते *café* 😀\n<b>&")))

    @Test fun fileNameSanitised() = assertEquals("a_b_c.md", NoteExport.fileName("a/b:c"))
    @Test fun fileNameFallback() { assertEquals("note.md", NoteExport.fileName("  ")); assertEquals("note.md", NoteExport.fileName("///")) }
    @Test fun fileNameUsesFirstLineAndTruncates() {
        assertEquals("Title.md", NoteExport.fileName("Title\nbody"))
        assertEquals(60 + 3, NoteExport.fileName("x".repeat(100)).length)
    }
}
