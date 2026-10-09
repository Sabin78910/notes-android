package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagsTest {
    @Test fun parseNormalizesAndDeduplicates() {
        assertEquals(listOf("ideas", "work"), Tags.parse("#Work, ideas  #work").toList())
    }

    @Test fun parseDropsInvalidCharactersAndEmpties() {
        assertEquals(listOf("a-b_1"), Tags.parse("# a-b_1! ,,#").toList())
    }

    @Test fun addAndRemoveTag() {
        val s = NoteStore(); val a = s.add("x")
        s.addTags(a.id, "#work ideas")
        assertEquals(setOf("work", "ideas"), s.visible().single().tags)
        s.removeTag(a.id, "work")
        assertEquals(setOf("ideas"), s.visible().single().tags)
    }

    @Test fun filtersByTag() {
        val s = NoteStore(); val a = s.add("one"); s.add("two")
        s.addTags(a.id, "work")
        assertEquals(listOf("one"), s.visible(tag = "work").map { it.text })
        assertEquals(2, s.visible(tag = null).size)
        assertTrue(s.visible(tag = "none").isEmpty())
    }

    @Test fun allTagsAreSortedAndExcludeTrashed() {
        val s = NoteStore(); val a = s.add("one"); val b = s.add("two")
        s.addTags(a.id, "work"); s.addTags(b.id, "ideas")
        assertEquals(listOf("ideas", "work"), s.allTags())
        s.delete(b.id)
        assertEquals(listOf("work"), s.allTags())
    }

    @Test fun tagsPersist() {
        val s = NoteStore(); val a = s.add("one"); s.archive(a.id); s.addTags(a.id, "work ideas")
        val r = NoteStore.deserialize(s.serialize())
        assertEquals(setOf("work", "ideas"), r.archived().single().tags)
    }

    @Test fun oldDataWithoutTagsLoads() {
        val r = NoteStore.deserialize("1\ttrue:5:false::::\thello")
        assertTrue(r.visible().single().tags.isEmpty())
    }
}
