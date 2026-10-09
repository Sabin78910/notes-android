package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteColorTest {
    @Test fun hasEightColours() = assertEquals(8, NoteColor.entries.size)

    @Test fun textMeetsWcagAaInBothThemes() {
        for (c in NoteColor.entries) for (dark in listOf(false, true))
            assertTrue("${c.name} dark=$dark", c.contrast(dark) >= 4.5)
    }

    @Test fun filterByColour() {
        val s = NoteStore()
        val a = s.add("a"); val b = s.add("b"); s.add("c")
        s.setColor(a.id, NoteColor.RED); s.setColor(b.id, NoteColor.BLUE)
        assertEquals(listOf("a"), s.visible(color = NoteColor.RED).map { it.text })
        assertEquals(3, s.visible(color = null).size)
    }

    @Test fun filterCombinesWithSearch() {
        val s = NoteStore()
        val a = s.add("milk"); val b = s.add("milk run")
        s.setColor(a.id, NoteColor.GREEN); s.setColor(b.id, NoteColor.RED)
        assertEquals(listOf("milk"), s.visible("milk", color = NoteColor.GREEN).map { it.text })
    }

    @Test fun clearingColour() {
        val s = NoteStore(); val a = s.add("a")
        s.setColor(a.id, NoteColor.TEAL); s.setColor(a.id, null)
        assertNull(s.visible().single().color)
    }

    @Test fun colourPersists() {
        val s = NoteStore(); val a = s.add("a"); s.add("b")
        s.setColor(a.id, NoteColor.PURPLE)
        val r = NoteStore.deserialize(s.serialize())
        assertEquals(NoteColor.PURPLE, r.visible(color = NoteColor.PURPLE).single().color)
        assertEquals(s.visible(), r.visible())
    }

    @Test fun oldDataWithoutColourLoads() {
        val r = NoteStore.deserialize("1\ttrue:5:false:\thello")
        assertNull(r.visible().single().color)
    }

    @Test fun unknownColourNameIgnored() {
        val r = NoteStore.deserialize("1\tfalse:5:false::NOPE\thello")
        assertNull(r.visible().single().color)
    }
}
