package com.sabin.notes

/** Days with at least one note written or edited; pure logic, no Android imports. */
object WritingStreak {
    private const val DAY_MS = 86_400_000L
    private const val KEEP_DAYS = 400L
    private val MILESTONES = setOf(3, 7, 14, 30, 60, 100, 365)

    fun dayOf(millis: Long, offsetMillis: Long): Long = Math.floorDiv(millis + offsetMillis, DAY_MS)

    /** Run of consecutive days ending today, or yesterday if today has no note yet. */
    fun current(days: Set<Long>, today: Long): Int {
        var d = if (today in days) today else today - 1
        var n = 0
        while (d in days) { n++; d-- }
        return n
    }

    fun record(days: Set<Long>, today: Long): Set<Long> =
        (days + today).filter { it > today - KEEP_DAYS }.toSet()

    fun milestone(streak: Int): Int? = streak.takeIf { it in MILESTONES }

    fun serialize(days: Set<Long>): String = days.sorted().joinToString(",")

    fun parse(data: String): Set<Long> = data.split(',').mapNotNull { it.trim().toLongOrNull() }.toSet()

    fun description(streak: Int): String = "$streak day writing streak"
}
