package com.medsafe.app.alarm

import com.medsafe.app.data.local.entity.MedicationEntity
import com.medsafe.app.data.local.entity.ScheduleTimeEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Pure date math shared by alarm scheduling and the Today screen. A schedule occurs on a
 * date when the medication is inside its duration window and the repeat rule matches.
 */
object ScheduleCalculator {

    /**
     * MedSafe posts one consolidated notification at each of these local times instead of one
     * notification per dose. The morning digest covers doses until the evening one, and the
     * evening digest covers the night until the next morning.
     */
    val DIGEST_TIMES: List<LocalTime> = listOf(LocalTime.of(9, 0), LocalTime.of(21, 0))

    fun occursOn(
        medication: MedicationEntity,
        schedule: ScheduleTimeEntity,
        date: LocalDate,
    ): Boolean {
        if (date.isBefore(medication.startDate)) return false
        if (!medication.isIndefinite && medication.endDate != null && date.isAfter(medication.endDate)) {
            return false
        }
        val interval = schedule.intervalDays
        if (interval != null && interval > 0) {
            val daysSinceStart = ChronoUnit.DAYS.between(medication.startDate, date)
            return daysSinceStart >= 0 && daysSinceStart % interval == 0L
        }
        val daysOfWeek: Set<DayOfWeek> = schedule.daysOfWeek
        return daysOfWeek.isEmpty() || date.dayOfWeek in daysOfWeek
    }

    /** First occurrence strictly after [after], or null when the medication has ended. */
    fun nextTrigger(
        medication: MedicationEntity,
        schedule: ScheduleTimeEntity,
        after: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Instant? {
        if (medication.archived) return null
        val startDate = maxOf(medication.startDate, after.atZone(zone).toLocalDate())
        val lastDate = if (medication.isIndefinite) {
            startDate.plusDays(MAX_LOOKAHEAD_DAYS.toLong())
        } else {
            medication.endDate ?: medication.startDate
        }
        if (lastDate.isBefore(startDate)) return null

        var date = startDate
        while (!date.isAfter(lastDate)) {
            if (occursOn(medication, schedule, date)) {
                val candidate = date.atTime(schedule.timeOfDay).atZone(zone).toInstant()
                if (candidate.isAfter(after)) return candidate
            }
            date = date.plusDays(1)
        }
        return null
    }

    /** All dose instants that fall on [date], used by the Today timeline. */
    fun occurrencesOn(
        medication: MedicationEntity,
        schedules: List<ScheduleTimeEntity>,
        date: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Instant> {
        if (medication.archived) return emptyList()
        return schedules
            .filter { occursOn(medication, it, date) }
            .map { date.atTime(it.timeOfDay).atZone(zone).toInstant() }
            .sorted()
    }

    /**
     * Doses that fall in the half-open digest window `[start, end)`. Each digest covers the
     * twelve hours that follow it, so every dose belongs to exactly one digest.
     */
    fun occurrencesBetween(
        medication: MedicationEntity,
        schedules: List<ScheduleTimeEntity>,
        start: Instant,
        end: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Instant> {
        if (medication.archived || !end.isAfter(start)) return emptyList()
        val firstDay = start.atZone(zone).toLocalDate()
        val lastDay = end.minusNanos(1).atZone(zone).toLocalDate()
        val result = mutableListOf<Instant>()
        var day = firstDay
        while (!day.isAfter(lastDay)) {
            schedules.forEach { schedule ->
                if (occursOn(medication, schedule, day)) {
                    val at = day.atTime(schedule.timeOfDay).atZone(zone).toInstant()
                    if (!at.isBefore(start) && at.isBefore(end)) result += at
                }
            }
            day = day.plusDays(1)
        }
        return result.sorted()
    }

    /** First digest slot strictly after [after]. */
    fun nextDigest(
        after: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Instant {
        val today = after.atZone(zone).toLocalDate()
        for (dayOffset in 0L..2L) {
            val day = today.plusDays(dayOffset)
            DIGEST_TIMES.forEach { time ->
                val candidate = day.atTime(time).atZone(zone).toInstant()
                if (candidate.isAfter(after)) return candidate
            }
        }
        error("No digest slot found after $after")
    }

    private const val MAX_LOOKAHEAD_DAYS = 400
}
