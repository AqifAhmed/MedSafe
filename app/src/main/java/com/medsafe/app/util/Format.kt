package com.medsafe.app.util

import com.medsafe.app.data.local.entity.MedicationEntity
import com.medsafe.app.data.local.entity.ScheduleTimeEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object Format {
    // Read the locale on each call: assigning it once would ignore runtime locale changes.
    private val timeFormatter: DateTimeFormatter
        get() = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val shortDateFormatter: DateTimeFormatter
        get() = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

    fun qty(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString().trimEnd('0')

    fun time(time: LocalTime): String = time.format(timeFormatter)

    fun date(date: LocalDate): String = date.format(shortDateFormatter)

    fun doseAmount(medication: MedicationEntity): String {
        val unit = medication.doseUnit.ifBlank { medication.form.label.lowercase(Locale.getDefault()) }
        return "${qty(medication.doseQty)} $unit"
    }

    fun daysLeft(medication: MedicationEntity, today: LocalDate = LocalDate.now()): Long? {
        if (medication.isIndefinite) return null
        val end = medication.endDate ?: return null
        return ChronoUnit.DAYS.between(today, end)
    }

    fun durationBadge(medication: MedicationEntity, today: LocalDate = LocalDate.now()): String {
        if (medication.archived) return "Archived"
        if (medication.isIndefinite) return "Ongoing"
        val left = daysLeft(medication, today) ?: return "Ongoing"
        return when {
            left < 0 -> "Ended"
            left == 0L -> "Last day"
            left == 1L -> "1 day left"
            else -> "$left days left"
        }
    }

    fun repeatLabel(schedule: ScheduleTimeEntity): String {
        val interval = schedule.intervalDays
        if (interval != null) {
            return if (interval == 1) "Daily" else "Every $interval days"
        }
        val days = schedule.daysOfWeek
        if (days.isEmpty() || days.size == 7) return "Daily"
        if (days.size == 5 && days == setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
            )
        ) {
            return "Weekdays"
        }
        return days.sortedBy { it.value }.joinToString(", ") { it.name.take(3).lowercase(Locale.getDefault()) }
    }

    fun scheduleSummary(schedules: List<ScheduleTimeEntity>): String {
        if (schedules.isEmpty()) return "No times"
        val sorted = schedules.sortedBy { it.timeOfDay }
        val times = sorted.map { time(it.timeOfDay) }.joinToString(", ")
        val repeat = sorted.map { repeatLabel(it) }.distinct()
        val repeatText = if (repeat.size == 1) " · ${repeat.first()}" else ""
        return "${sorted.size}× daily$repeatText · $times"
    }

    fun weekdayShort(day: DayOfWeek): String =
        day.name.take(3).lowercase(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }
}
