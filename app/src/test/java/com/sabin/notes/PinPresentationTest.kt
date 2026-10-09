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
}
