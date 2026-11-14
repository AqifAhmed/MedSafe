package com.medsafe.app.alarm

import com.medsafe.app.data.local.entity.MedicationEntity
import com.medsafe.app.data.local.entity.ScheduleTimeEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Pure date math shared by alarm scheduling and the Today screen. A schedule occurs on a
 * date when the medication is inside its duration window and the repeat rule matches.
 */
object ScheduleCalculator {

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

    private const val MAX_LOOKAHEAD_DAYS = 400
}
