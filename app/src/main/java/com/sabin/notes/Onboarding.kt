package com.sabin.notes

/** First-run onboarding: three screens, shown once; the last leads straight into writing a note. */
object Onboarding {
    class Page(val title: String, val benefit: String)

    val pages = listOf(
        Page("Capture fast", "Jot down a thought in seconds."),
        Page("Find anything", "Search, tag and colour to find it later."),
        Page("Pin what matters", "Keep your important notes on top.")
    )

    interface Flag {
        fun isSeen(): Boolean
        fun markSeen()
    }

    fun shouldShow(flag: Flag) = !flag.isSeen()

    fun finish(flag: Flag) = flag.markSeen()

    fun isLast(index: Int) = index == pages.lastIndex
}
