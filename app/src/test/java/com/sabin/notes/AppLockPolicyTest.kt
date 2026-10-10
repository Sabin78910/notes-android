package com.sabin.notes

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockPolicyTest {
    private val min = AppLockPolicy.TIMEOUT_MS

    @Test fun disabledNeverLocks() {
        assertFalse(AppLockPolicy.shouldLock(false, unlocked = false, backgroundedAt = 0, now = min * 10))
    }

    @Test fun coldStartLocksWhenEnabled() {
        assertTrue(AppLockPolicy.shouldLock(true, unlocked = false, backgroundedAt = null, now = 0))
    }

    @Test fun staysUnlockedWhileForeground() {
        assertFalse(AppLockPolicy.shouldLock(true, unlocked = true, backgroundedAt = null, now = min * 10))
    }

    @Test fun shortBackgroundStaysUnlocked() {
        assertFalse(AppLockPolicy.shouldLock(true, unlocked = true, backgroundedAt = 1000, now = 1000 + min - 1))
    }

    @Test fun backgroundForTimeoutLocks() {
        assertTrue(AppLockPolicy.shouldLock(true, unlocked = true, backgroundedAt = 1000, now = 1000 + min))
    }

    @Test fun timeoutIsOneMinute() { assertTrue(min == 60_000L) }
}
