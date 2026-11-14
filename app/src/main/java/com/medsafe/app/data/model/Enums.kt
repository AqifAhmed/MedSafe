package com.medsafe.app.data.model

/** How a medication is taken. Strength (e.g. mg) is deliberately not modelled. */
enum class MedicationForm(val label: String) {
    TABLET("Tablet"),
    CAPSULE("Capsule"),
    LIQUID("Liquid"),
    INJECTION("Injection"),
    DROPS("Drops"),
    INHALER("Inhaler"),
    OTHER("Other"),
}

/** Shapes drawn by [com.medsafe.app.ui.components.PillIcon]. */
enum class PillShape(val label: String) {
    CAPSULE("Capsule"),
    HALF_CAPSULE("Half & half"),
    ROUND_TABLET("Round tablet"),
    OVAL_TABLET("Oval tablet"),
    DROP("Drop"),
    INJECTION("Injection"),
    INHALER("Inhaler"),
}

/** Outcome of a scheduled dose. */
enum class DoseStatus {
    PENDING,
    TAKEN,
    SKIPPED,
    MISSED,
    SNOOZED,
}

/** How often a schedule repeats. "Every X days" is stored as [ScheduleTimeEntity.intervalDays]. */
enum class RepeatMode { DAILY, WEEKDAYS, INTERVAL }

/** Fixed-duration or ongoing. */
enum class DurationMode { ONGOING, FIXED_DAYS, FIXED_END_DATE }
