package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareTextTest {
    @Test
    fun subjectAndTextBecomeTitleThenBody() {
        assertEquals("Title\nBody", ShareText.incoming("Title", "Body"))
    }

    @Test
    fun textOnly() {
        assertEquals("Body", ShareText.incoming(null, "Body"))
        assertEquals("Body", ShareText.incoming("  ", "Body"))
    }

    @Test
    fun subjectOnly() {
        assertEquals("Title", ShareText.incoming("Title", null))
    }

    @Test
    fun blankOrNullExtrasGiveNoNote() {
        assertNull(ShareText.incoming(null, null))
        assertNull(ShareText.incoming(" ", "\n\t"))
    }

    @Test
    fun veryLongTextIsCapped() {
        val out = ShareText.incoming(null, "a".repeat(ShareText.MAX_LENGTH + 500))!!
        assertEquals(ShareText.MAX_LENGTH, out.length)
        assertTrue(out.all { it == 'a' })
    }

    @Test
    fun plainNoteIsSharedAsIs() {
        assertEquals("Title\nBody", ShareText.outgoing(Note(1, "Title\nBody")))
    }

    @Test
    fun checklistNoteUsesCheckboxLines() {
        val n = Note(1, "milk\n\neggs\nbread", checklist = true, checked = setOf(1))
        assertEquals("- [ ] milk\n- [x] eggs\n- [ ] bread", ShareText.outgoing(n))
    }
}
