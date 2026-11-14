package com.medsafe.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.medsafe.app.data.model.DoseStatus
import com.medsafe.app.data.model.MedicationForm
import com.medsafe.app.data.model.PillShape
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "profiles",
    indices = [Index(value = ["isMain"])],
)
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** ARGB color. */
    val color: Int,
    /** "initials" or a short label; the UI renders initials from the name by default. */
    val avatar: String = AVATAR_INITIALS,
    val isMain: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val AVATAR_INITIALS = "initials"
        const val MAX_PROFILES = 3
    }
}

@Entity(
    tableName = "medications",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("profileId")],
)
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val name: String,
    val form: MedicationForm,
    /** Quantity per dose, e.g. 2 tablets or 5 ml. Never a strength. */
    val doseQty: Double,
    val doseUnit: String,
    val pillShape: PillShape,
    val pillColor1: Int,
    val pillColor2: Int? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val isIndefinite: Boolean = true,
    val stock: Int? = null,
    val lowStockThreshold: Int? = null,
    val notes: String? = null,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "schedule_times",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("medId")],
)
data class ScheduleTimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medId: Long,
    val timeOfDay: LocalTime,
    /** Empty means every day. ISO day-of-week values (Mon = 1). */
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    /** When set, the dose repeats every N days counted from the medication start date. */
    val intervalDays: Int? = null,
)

@Entity(
    tableName = "dose_logs",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["medId", "scheduledAt"], unique = true),
        Index("scheduledAt"),
    ],
)
data class DoseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medId: Long,
    /** Epoch millis of the scheduled dose. */
    val scheduledAt: Long,
    val status: DoseStatus = DoseStatus.PENDING,
    val actedAt: Long? = null,
)
