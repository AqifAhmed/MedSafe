package com.medsafe.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class MedicationWithSchedules(
    @Embedded val medication: MedicationEntity,
    @Relation(parentColumn = "id", entityColumn = "medId")
    val schedules: List<ScheduleTimeEntity>,
) {
    val sortedTimes get() = schedules.map { it.timeOfDay }.sorted()
}

data class DoseLogWithMedication(
    @Embedded val log: DoseLogEntity,
    @Relation(parentColumn = "medId", entityColumn = "id")
    val medication: MedicationEntity,
)
