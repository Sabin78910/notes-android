package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Test

class PinPresentationTest {
    @Test fun pinnedNoteShowsPinnedLabelAndUnpinButton() {
        assertEquals("Pinned", PinPresentation.badge(true))
        assertEquals("Unpin", PinPresentation.buttonLabel(true))
    }

    @Test fun unpinnedNoteHasNoBadgeAndPinButton() {
        assertEquals(null, PinPresentation.badge(false))
        assertEquals("Pin", PinPresentation.buttonLabel(false))
    }

    @Test fun actionDescriptionsNameTheNote() {
        assertEquals("Pin note: Milk", PinPresentation.pinDescription(false, "Milk"))
        assertEquals("Unpin note: Milk", PinPresentation.pinDescription(true, "Milk"))
        assertEquals("Edit note: Milk", PinPresentation.editDescription("Milk"))
        assertEquals("Delete note: Milk", PinPresentation.deleteDescription("Milk"))
    }

    @Test fun longNoteTextIsTruncatedInDescription() {
        val d = PinPresentation.deleteDescription("a".repeat(100))
        assertEquals("Delete note: " + "a".repeat(40) + "…", d)
    }

    @Test fun multilineNoteUsesFirstLine() {
        assertEquals("Edit note: one", PinPresentation.editDescription("one\ntwo"))
    }
}
