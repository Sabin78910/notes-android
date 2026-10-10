package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReviewPromptTest {
    private val day = 24L * 60 * 60 * 1000

    private class FakeState(override var firstLaunchAt: Long? = null, override var lastAskedAt: Long? = null) : ReviewPrompt.State
    private class FakeGateway : ReviewPrompt.Gateway { var launches = 0; override fun launch() { launches++ } }

    private fun prompt(state: FakeState = FakeState(firstLaunchAt = 0L), gw: FakeGateway = FakeGateway()) = Triple(ReviewPrompt(state, gw), state, gw)

    @Test fun asksAfterChecklistCompleted() {
        val (p, s, gw) = prompt()
        p.onChecklistCompleted(2 * day)
        assertEquals(1, gw.launches)
        assertEquals(2 * day, s.lastAskedAt)
    }

    @Test fun asksOnTenthNoteButNotBefore() {
        val (p, _, gw) = prompt()
        p.onNoteAdded(9, 2 * day)
        assertEquals(0, gw.launches)
        p.onNoteAdded(10, 2 * day)
        assertEquals(1, gw.launches)
    }

    @Test fun neverOnFirstLaunch() {
        val (p, s, gw) = prompt(FakeState())
        p.onAppLaunch(1000L)
        p.onChecklistCompleted(2000L)
        assertEquals(0, gw.launches)
        assertEquals(1000L, s.firstLaunchAt)
        assertNull(s.lastAskedAt)
    }

    @Test fun atMostOnceEvery60Days() {
        val (p, _, gw) = prompt()
        p.onChecklistCompleted(2 * day)
        p.onChecklistCompleted(61 * day)
        assertEquals(1, gw.launches)
        p.onChecklistCompleted(62 * day)
        assertEquals(2, gw.launches)
    }

    @Test fun neverAfterError() {
        val (p, _, gw) = prompt()
        p.onError()
        p.onChecklistCompleted(2 * day)
        assertEquals(0, gw.launches)
    }

    @Test fun unknownFirstLaunchDoesNotAsk() {
        val (p, _, gw) = prompt(FakeState())
        p.onChecklistCompleted(100 * day)
        assertEquals(0, gw.launches)
    }
}
