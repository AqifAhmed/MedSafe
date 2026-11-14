package com.medsafe.app.alarm

import com.medsafe.app.data.local.entity.MedicationEntity
import com.medsafe.app.data.local.entity.ScheduleTimeEntity
import com.medsafe.app.data.model.MedicationForm
import com.medsafe.app.data.model.PillShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class ScheduleCalculatorTest {

    private val zone = ZoneOffset.UTC
    private val start = LocalDate.of(2026, 1, 5) // Monday

    private fun medication(
        startDate: LocalDate = start,
        endDate: LocalDate? = null,
        indefinite: Boolean = true,
        archived: Boolean = false,
    ) = MedicationEntity(
        id = 1,
        profileId = 1,
        name = "Test",
        form = MedicationForm.TABLET,
        doseQty = 1.0,
        doseUnit = "tablet",
        pillShape = PillShape.CAPSULE,
        pillColor1 = 0,
        startDate = startDate,
        endDate = endDate,
        isIndefinite = indefinite,
        archived = archived,
    )

    private fun schedule(
        time: LocalTime = LocalTime.of(8, 0),
        days: Set<DayOfWeek> = emptySet(),
        intervalDays: Int? = null,
    ) = ScheduleTimeEntity(medId = 1, timeOfDay = time, daysOfWeek = days, intervalDays = intervalDays)

    private fun instant(date: LocalDate, hour: Int, minute: Int = 0): Instant =
        date.atTime(hour, minute).atZone(zone).toInstant()

    @Test
    fun dailyScheduleFiresLaterToday() {
        val next = ScheduleCalculator.nextTrigger(
            medication = medication(),
            schedule = schedule(),
            after = instant(start, 7),
            zone = zone,
        )
        assertEquals(instant(start, 8), next)
    }

    @Test
    fun dailyScheduleRollsToTomorrowAfterDoseTime() {
        val next = ScheduleCalculator.nextTrigger(
            medication = medication(),
            schedule = schedule(),
            after = instant(start, 9),
            zone = zone,
        )
        assertEquals(instant(start.plusDays(1), 8), next)
    }

    @Test
    fun weekdayScheduleSkipsTheWeekend() {
        val next = ScheduleCalculator.nextTrigger(
            medication = medication(),
            schedule = schedule(days = setOf(DayOfWeek.MONDAY)),
            after = instant(LocalDate.of(2026, 1, 10), 12), // Saturday
            zone = zone,
        )
        assertEquals(instant(LocalDate.of(2026, 1, 12), 8), next) // Monday
    }

    @Test
    fun everyNDaysAnchorsToStartDate() {
        val everyThreeDays = schedule(intervalDays = 3)
        val med = medication()
        // Start day is a dose day.
        assertEquals(instant(start, 8), ScheduleCalculator.nextTrigger(med, everyThreeDays, instant(start.minusDays(1), 12), zone))
        // The two days in between are skipped.
        assertEquals(instant(start.plusDays(3), 8), ScheduleCalculator.nextTrigger(med, everyThreeDays, instant(start.plusDays(1), 9), zone))
        assertEquals(instant(start.plusDays(6), 8), ScheduleCalculator.nextTrigger(med, everyThreeDays, instant(start.plusDays(4), 9), zone))
    }

    @Test
    fun fixedDurationStopsAfterEndDate() {
        val end = start.plusDays(2)
        val med = medication(endDate = end, indefinite = false)
        val last = ScheduleCalculator.nextTrigger(med, schedule(), instant(end, 7), zone)
        assertNotNull(last)
        assertEquals(instant(end, 8), last)

        val afterEnd = ScheduleCalculator.nextTrigger(med, schedule(), instant(end, 9), zone)
        assertNull(afterEnd)
    }

    @Test
    fun archivedMedicationNeverSchedules() {
        val next = ScheduleCalculator.nextTrigger(
            medication = medication(archived = true),
            schedule = schedule(),
            after = instant(start, 7),
            zone = zone,
        )
        assertNull(next)
    }

    @Test
    fun occurrencesOnMatchesOnlySelectedWeekdays() {
        val med = medication()
        val monday = schedule(days = setOf(DayOfWeek.MONDAY))
        val wednesday = schedule(time = LocalTime.of(20, 0), days = setOf(DayOfWeek.WEDNESDAY))
        val schedules = listOf(monday, wednesday)

        assertTrue(ScheduleCalculator.occursOn(med, monday, start))
        assertEquals(
            listOf(instant(start, 8)),
            ScheduleCalculator.occurrencesOn(med, schedules, start, zone),
        )
        assertEquals(
            listOf(instant(start.plusDays(2), 20)),
            ScheduleCalculator.occurrencesOn(med, schedules, start.plusDays(2), zone),
        )
        assertTrue(ScheduleCalculator.occurrencesOn(med, schedules, start.plusDays(1), zone).isEmpty())
    }

    @Test
    fun doesNotScheduleBeforeStartDate() {
        val futureStart = LocalDate.of(2026, 2, 1)
        val med = medication(startDate = futureStart, endDate = futureStart.plusDays(5), indefinite = false)
        val next = ScheduleCalculator.nextTrigger(med, schedule(), instant(start, 8), zone)
        assertEquals(instant(futureStart, 8), next)
    }
}
