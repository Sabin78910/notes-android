package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteGridTest {
    @Test fun gridUsesTwoColumnsAndListOne() {
        assertEquals(2, LayoutMode.GRID.columns)
        assertEquals(1, LayoutMode.LIST.columns)
    }

    @Test fun toggleSwitchesMode() {
        assertEquals(LayoutMode.LIST, LayoutMode.GRID.toggled())
        assertEquals(LayoutMode.GRID, LayoutMode.LIST.toggled())
    }

    @Test fun savedNameRestoresModeAndDefaultsToGrid() {
        assertEquals(LayoutMode.LIST, LayoutMode.fromName("LIST"))
        assertEquals(LayoutMode.GRID, LayoutMode.fromName("GRID"))
        assertEquals(LayoutMode.GRID, LayoutMode.fromName(null))
        assertEquals(LayoutMode.GRID, LayoutMode.fromName("bogus"))
    }

    @Test fun toggleLabelDescribesTheTargetMode() {
        assertEquals("Switch to list view", LayoutMode.GRID.toggleDescription())
        assertEquals("Switch to grid view", LayoutMode.LIST.toggleDescription())
    }

    @Test fun previewKeepsShortTextAndTruncatesLongText() {
        assertEquals("hello", NotePreview.text(Note(1, "hello")))
        val p = NotePreview.text(Note(1, "a".repeat(300)))
        assertEquals("a".repeat(NotePreview.MAX_CHARS) + "…", p)
    }

    @Test fun previewLimitsLines() {
        val p = NotePreview.text(Note(1, "1\n2\n3\n4\n5\n6\n7\n8"))
        assertEquals("1\n2\n3\n4\n5\n6…", p)
    }

    @Test fun checklistPreviewShowsFirstItemsOnly() {
        val n = Note(1, "a\nb\nc\nd\ne\nf", checklist = true)
        assertEquals(listOf("a", "b", "c", "d"), NotePreview.checklistItems(n))
        assertEquals(2, NotePreview.hiddenItems(n))
        assertEquals(0, NotePreview.hiddenItems(Note(1, "a\nb", checklist = true)))
    }

    @Test fun emptyStateMessagesDependOnViewAndFilters() {
        assertEquals("No notes yet — tap New note to add one", EmptyState.message("Notes", false))
        assertEquals("No matching notes", EmptyState.message("Notes", true))
        assertEquals("Archive is empty", EmptyState.message("Archive", false))
        assertEquals("Trash is empty", EmptyState.message("Trash", false))
        assertNull(EmptyState.message("Notes", false).takeIf { it.isEmpty() })
    }

    @Test fun sortToggleDescriptionsAndGlyphs() {
        assertEquals("Sort: oldest first", SortOrder.description(true))
        assertEquals("Sort: newest first", SortOrder.description(false))
        assertEquals("↓", SortOrder.glyph(true))
        assertEquals("↑", SortOrder.glyph(false))
    }

    @Test fun emptyStateOffersNewNoteOnlyOnPlainNotesView() {
        assertEquals(true, EmptyState.showNewNoteAction("Notes", false))
        assertEquals(false, EmptyState.showNewNoteAction("Notes", true))
        assertEquals(false, EmptyState.showNewNoteAction("Trash", false))
        assertEquals(false, EmptyState.showNewNoteAction("Archive", false))
    }

    @Test fun emptyStateHelperTextAndIcons() {
        assertEquals("Capture ideas, lists and reminders.", EmptyState.helper("Notes", false))
        assertEquals("Try a different search or clear the filters.", EmptyState.helper("Notes", true))
        assertEquals("Archived notes show up here.", EmptyState.helper("Archive", false))
        assertEquals("Deleted notes stay here for 30 days.", EmptyState.helper("Trash", false))
        assertEquals("📝", EmptyState.icon("Notes", false))
        assertEquals("🔍", EmptyState.icon("Notes", true))
        assertEquals("📦", EmptyState.icon("Archive", false))
        assertEquals("🗑️", EmptyState.icon("Trash", false))
    }
}
