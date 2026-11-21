package com.medsafe.app.alarm

/**
 * Pure decisions used by [DoseReconciler], kept free of Android types so they are unit-testable.
 * All times are epoch milliseconds.
 */
object ReconcilePlanner {
    /** Never look further back than this when repairing state. */
    const val MAX_LOOKBACK_MS = 48L * 60 * 60 * 1000

    /** An answered dose is not closed out until this long after its scheduled time. */
    const val MISSED_GRACE_MS = 15L * 60 * 1000

    fun windowStart(since: Long, now: Long): Long = maxOf(since, now - MAX_LOOKBACK_MS)

    /** When a snoozed dose should fire again; overdue snoozes fire immediately. */
    fun snoozeFireAt(now: Long, actedAt: Long?, snoozeMinutes: Int): Long =
        maxOf((actedAt ?: now) + minutesToMs(snoozeMinutes), now)

    private fun minutesToMs(minutes: Int): Long = minutes.coerceAtLeast(1) * 60_000L
}
