package com.sabin.notes

/** Decides when the optional app lock must be shown. */
object AppLockPolicy {
    const val TIMEOUT_MS = 60_000L

    fun shouldLock(enabled: Boolean, unlocked: Boolean, backgroundedAt: Long?, now: Long): Boolean {
        if (!enabled) return false
        if (!unlocked) return true
        return backgroundedAt != null && now - backgroundedAt >= TIMEOUT_MS
    }
}
