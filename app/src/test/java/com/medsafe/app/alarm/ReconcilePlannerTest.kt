package com.medsafe.app.alarm

import org.junit.Assert.assertEquals
import org.junit.Test

class ReconcilePlannerTest {

    private val minute = 60_000L
    private val hour = 60 * minute
    private val now = 1_000_000_000_000L

    @Test
    fun snoozeFiresAtActedAtPlusSnoozeMinutes() {
        val actedAt = now - 3 * minute
        assertEquals(
            actedAt + 10 * minute,
            ReconcilePlanner.snoozeFireAt(now, actedAt, snoozeMinutes = 10),
        )
    }

    @Test
    fun overdueSnoozeFiresImmediately() {
        val actedAt = now - 2 * hour
        assertEquals(now, ReconcilePlanner.snoozeFireAt(now, actedAt, snoozeMinutes = 10))
    }

    @Test
    fun snoozeWithoutActedAtFallsBackToNow() {
        assertEquals(now + 10 * minute, ReconcilePlanner.snoozeFireAt(now, null, snoozeMinutes = 10))
    }

    @Test
    fun nonPositiveSnoozeMinutesAreTreatedAsOneMinute() {
        assertEquals(now + minute, ReconcilePlanner.snoozeFireAt(now, null, snoozeMinutes = 0))
    }

    @Test
    fun windowIsCappedAtLookback() {
        val longAgo = now - 30 * 24 * hour
        assertEquals(now - ReconcilePlanner.MAX_LOOKBACK_MS, ReconcilePlanner.windowStart(longAgo, now))
        val recent = now - 2 * hour
        assertEquals(recent, ReconcilePlanner.windowStart(recent, now))
    }
}
