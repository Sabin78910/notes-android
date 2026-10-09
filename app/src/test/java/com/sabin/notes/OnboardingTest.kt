package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingTest {
    private class Flag(var value: Boolean = false) : Onboarding.Flag {
        override fun isSeen() = value
        override fun markSeen() { value = true }
    }

    @Test fun shownOnFirstRun() = assertTrue(Onboarding.shouldShow(Flag()))

    @Test fun notShownOnceSeen() = assertFalse(Onboarding.shouldShow(Flag(true)))

    @Test fun finishMarksSeenSoItShowsOnlyOnce() {
        val f = Flag()
        Onboarding.finish(f)
        assertFalse(Onboarding.shouldShow(f))
    }

    @Test fun hasThreeScreensWithOneLineBenefits() {
        assertEquals(3, Onboarding.pages.size)
        Onboarding.pages.forEach { assertFalse(it.benefit.contains("\n")) }
    }

    @Test fun onlyLastScreenStartsFirstNote() {
        assertEquals(listOf(false, false, true), Onboarding.pages.indices.map { Onboarding.isLast(it) })
    }
}
