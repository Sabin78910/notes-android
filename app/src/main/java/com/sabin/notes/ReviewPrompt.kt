package com.sabin.notes

/** Decides when to ask for a Play Store rating: only after a happy moment, rarely, never early or after an error. */
class ReviewPrompt(private val state: State, private val gateway: Gateway) {
    interface State {
        var firstLaunchAt: Long?
        var lastAskedAt: Long?
    }

    interface Gateway {
        fun launch()
    }

    private var errorThisSession = false

    fun onAppLaunch(now: Long) {
        if (state.firstLaunchAt == null) state.firstLaunchAt = now
    }

    fun onError() { errorThisSession = true }

    fun onChecklistCompleted(now: Long) = ask(now)

    fun onNoteAdded(totalNotes: Int, now: Long) { if (totalNotes >= NOTES_MILESTONE) ask(now) }

    private fun ask(now: Long) {
        val first = state.firstLaunchAt ?: return
        if (errorThisSession || now - first < MIN_APP_AGE_MS) return
        state.lastAskedAt?.let { if (now - it < MIN_GAP_MS) return }
        state.lastAskedAt = now
        gateway.launch()
    }

    companion object {
        const val NOTES_MILESTONE = 10
        const val MIN_GAP_MS = 60L * 24 * 60 * 60 * 1000
        /** Keeps the prompt out of the first launch/day. */
        const val MIN_APP_AGE_MS = 24L * 60 * 60 * 1000
    }
}
