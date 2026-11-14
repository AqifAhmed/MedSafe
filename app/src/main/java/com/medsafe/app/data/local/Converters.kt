package com.medsafe.app.data.local

import androidx.room.TypeConverter
import com.medsafe.app.data.model.DoseStatus
import com.medsafe.app.data.model.MedicationForm
import com.medsafe.app.data.model.PillShape
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Local dates and times are persisted as text and resolved to an instant only when
 * scheduling, so timezone changes stay predictable.
 */
class Converters {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun localTimeToString(value: LocalTime?): String? = value?.format(timeFormatter)

    @TypeConverter
    fun stringToLocalTime(value: String?): LocalTime? =
        value?.let { LocalTime.parse(it, timeFormatter) }

    @TypeConverter
    fun formToString(value: MedicationForm): String = value.name

    @TypeConverter
    fun stringToForm(value: String): MedicationForm = MedicationForm.valueOf(value)

    @TypeConverter
    fun shapeToString(value: PillShape): String = value.name

    @TypeConverter
    fun stringToShape(value: String): PillShape = PillShape.valueOf(value)

    @TypeConverter
    fun statusToString(value: DoseStatus): String = value.name

    @TypeConverter
    fun stringToStatus(value: String): DoseStatus = DoseStatus.valueOf(value)

    @TypeConverter
    fun daysToString(value: Set<DayOfWeek>): String =
        value.sortedBy { it.value }.joinToString(",") { it.value.toString() }

    @TypeConverter
    fun stringToDays(value: String?): Set<DayOfWeek> {
        if (value.isNullOrBlank()) return emptySet()
        return value.split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .mapNotNull { runCatching { DayOfWeek.of(it) }.getOrNull() }
            .toSet()
    }
}
