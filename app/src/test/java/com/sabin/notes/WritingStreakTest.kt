package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WritingStreakTest {
    @Test fun noDaysMeansNoStreak() = assertEquals(0, WritingStreak.current(emptySet(), 10))

    @Test fun consecutiveDaysEndingTodayCount() = assertEquals(3, WritingStreak.current(setOf(8L, 9L, 10L), 10))

    @Test fun streakSurvivesUntilEndOfTomorrow() = assertEquals(2, WritingStreak.current(setOf(8L, 9L), 10))

    @Test fun missedDayBreaksStreak() = assertEquals(0, WritingStreak.current(setOf(7L, 8L), 10))

    @Test fun gapEndsRun() = assertEquals(2, WritingStreak.current(setOf(5L, 6L, 9L, 10L), 10))

    @Test fun dayOfRespectsTimezoneOffset() {
        val almostMidnightUtc = 86_400_000L - 1
        assertEquals(0L, WritingStreak.dayOf(almostMidnightUtc, 0))
        assertEquals(1L, WritingStreak.dayOf(almostMidnightUtc, 3_600_000))
        assertEquals(-1L, WritingStreak.dayOf(0, -3_600_000))
    }

    @Test fun recordAddsDayAndPrunesOldOnes() {
        val result = WritingStreak.record(setOf(1L, 500L), 600)
        assertEquals(setOf(500L, 600L), result)
    }

    @Test fun milestonesAreCelebratedOnlyOnExactDays() {
        assertEquals(3, WritingStreak.milestone(3))
        assertEquals(7, WritingStreak.milestone(7))
        assertNull(WritingStreak.milestone(4))
        assertNull(WritingStreak.milestone(0))
    }

    @Test fun serializeRoundTripsAndIgnoresJunk() {
        assertEquals(setOf(1L, 2L, 3L), WritingStreak.parse(WritingStreak.serialize(setOf(3L, 1L, 2L))))
        assertEquals(setOf(4L), WritingStreak.parse("4,x,,"))
        assertTrue(WritingStreak.parse("").isEmpty())
    }

    @Test fun descriptionIsPluralAware() {
        assertEquals("1 day writing streak", WritingStreak.description(1))
        assertEquals("5 day writing streak", WritingStreak.description(5))
        assertFalse(WritingStreak.description(2).isEmpty())
    }

    @Test fun illustrationPerViewAndFilter() {
        assertEquals(EmptyIllustration.NOTES, EmptyState.illustration("Notes", false))
        assertEquals(EmptyIllustration.SEARCH, EmptyState.illustration("Notes", true))
        assertEquals(EmptyIllustration.ARCHIVE, EmptyState.illustration("Archive", false))
        assertEquals(EmptyIllustration.TRASH, EmptyState.illustration("Trash", false))
    }
}
