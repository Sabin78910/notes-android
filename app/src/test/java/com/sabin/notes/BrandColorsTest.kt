package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandColorsTest {
    @Test fun seedIsAmber() = assertEquals(0xFFF59E0B, BrandColors.SEED)

    @Test fun lightAndDarkDiffer() = assertNotEquals(BrandColors.light, BrandColors.dark)

    @Test fun primaryIsAmberInLightAndLighterInDark() {
        assertEquals(BrandColors.SEED, BrandColors.light.primaryContainer)
        assertTrue(BrandColors.dark.primary != BrandColors.light.primary)
    }

    @Test fun textMeetsWcagAaInBothSchemes() {
        for (s in listOf(BrandColors.light, BrandColors.dark)) {
            assertTrue(NoteColor.contrastRatio(s.primary, s.onPrimary) >= 4.5)
            assertTrue(NoteColor.contrastRatio(s.primaryContainer, s.onPrimaryContainer) >= 4.5)
            assertTrue(NoteColor.contrastRatio(s.background, s.onBackground) >= 4.5)
            assertTrue(NoteColor.contrastRatio(s.surfaceVariant, s.onSurfaceVariant) >= 4.5)
        }
    }

    @Test fun cardCornerIsWithinExpressiveRange() = assertTrue(BrandColors.CARD_CORNER_DP in 24..28)
}
