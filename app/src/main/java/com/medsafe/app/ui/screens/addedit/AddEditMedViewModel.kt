package com.medsafe.app.ui.screens.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medsafe.app.data.local.entity.MedicationEntity
import com.medsafe.app.data.local.entity.MedicationWithSchedules
import com.medsafe.app.data.local.entity.ScheduleTimeEntity
import com.medsafe.app.data.model.DurationMode
import com.medsafe.app.data.model.MedicationForm
import com.medsafe.app.data.model.PillShape
import com.medsafe.app.data.model.RepeatMode
import com.medsafe.app.data.repository.MedicationRepository
import com.medsafe.app.data.repository.ProfileRepository
import com.medsafe.app.data.settings.SettingsRepository
import com.medsafe.app.ui.navigation.Routes
import com.medsafe.app.ui.theme.PillPalette
import com.medsafe.app.util.Format
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class MedFormState(
    val medId: Long = 0,
    val step: Int = 0,
    val profileName: String = "",
    val name: String = "",
    val form: MedicationForm = MedicationForm.TABLET,
    val doseQty: String = "1",
    val doseUnit: String = "tablet",
    val notes: String = "",
    val pillShape: PillShape = PillShape.CAPSULE,
    val pillColor1: Int = PillPalette.first(),
    val pillColor2: Int = PillPalette[1],
    val twoColors: Boolean = false,
    val times: List<LocalTime> = listOf(LocalTime.of(9, 0)),
    val repeatMode: RepeatMode = RepeatMode.DAILY,
    val weekdays: Set<DayOfWeek> = WeekdaysDefault,
    val intervalDays: String = "2",
    val startDate: LocalDate = LocalDate.now(),
    val durationMode: DurationMode = DurationMode.ONGOING,
    val durationDays: String = "30",
    val endDate: LocalDate = LocalDate.now().plusDays(29),
    val hasStock: Boolean = false,
    val stock: String = "",
    val lowStockThreshold: String = "",
    val isEditing: Boolean = false,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
)

val WeekdaysDefault: Set<DayOfWeek> = setOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
)

@HiltViewModel
class AddEditMedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val medicationRepository: MedicationRepository,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val medId: Long = savedStateHandle.get<Long>(Routes.ARG_MED_ID) ?: -1L
    private var profileId: Long? = null
    private var wasArchived: Boolean = false

    private val _state = MutableStateFlow(MedFormState(isEditing = medId > 0))
    val state: StateFlow<MedFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val profiles = profileRepository.profiles.first()
        val activeId = settingsRepository.settings.first().activeProfileId
        val profile = profiles.firstOrNull { it.id == activeId }
            ?: profiles.firstOrNull { it.isMain }
            ?: profiles.firstOrNull()
        profileId = profile?.id

        if (medId > 0L) {
            val existing = medicationRepository.getWithSchedules(medId)
            if (existing != null) {
                _state.value = existing.toFormState(profile?.name.orEmpty())
                return
            }
        }
        _state.update { it.copy(loading = false, profileName = profile?.name.orEmpty()) }
    }

    fun update(transform: (MedFormState) -> MedFormState) {
        _state.update(transform)
    }

    fun setForm(form: MedicationForm) {
        _state.update { current ->
            val previousDefault = defaultUnit(current.form)
            val unit = if (current.doseUnit.isBlank() ||
                current.doseUnit.equals(previousDefault, ignoreCase = true)
            ) {
                defaultUnit(form)
            } else {
                current.doseUnit
            }
            current.copy(form = form, doseUnit = unit)
        }
    }

    fun addTime() {
        _state.update { current ->
            if (current.times.size >= MAX_TIMES) {
                current
            } else {
                val next = current.times.lastOrNull()?.plusHours(2) ?: LocalTime.of(9, 0)
                current.copy(times = (current.times + next).sorted())
            }
        }
    }

    fun updateTime(index: Int, time: LocalTime) {
        _state.update { current ->
            val updated = current.times.toMutableList()
            if (index in updated.indices) updated[index] = time
            current.copy(times = updated.sorted())
        }
    }

    fun removeTime(index: Int) {
        _state.update { current ->
            if (current.times.size <= 1) current
            else current.copy(times = current.times.filterIndexed { i, _ -> i != index })
        }
    }

    fun toggleWeekday(day: DayOfWeek) {
        _state.update { current ->
            val days = if (day in current.weekdays) current.weekdays - day else current.weekdays + day
            current.copy(weekdays = days)
        }
    }

    fun next() {
        val current = _state.value
        val error = validate(current, current.step)
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        _state.update {
            it.copy(step = (it.step + 1).coerceAtMost(LAST_STEP), error = null)
        }
    }

    fun back() {
        _state.update {
            it.copy(step = (it.step - 1).coerceAtLeast(0), error = null)
        }
    }

    fun save(onSaved: () -> Unit) {
        val current = _state.value
        for (step in 0..LAST_STEP) {
            val error = validate(current, step)
            if (error != null) {
                _state.update { it.copy(step = step, error = error) }
                return
            }
        }
        val targetProfile = profileId ?: run {
            _state.update { it.copy(error = "No profile is selected.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                val endDate = when (current.durationMode) {
                    DurationMode.ONGOING -> null
                    DurationMode.FIXED_DAYS -> current.startDate.plusDays(
                        (current.durationDays.toIntOrNull() ?: 1).toLong() - 1,
                    )
                    DurationMode.FIXED_END_DATE -> current.endDate
                }
                val medication = MedicationEntity(
                    id = current.medId,
                    profileId = targetProfile,
                    name = current.name.trim(),
                    form = current.form,
                    doseQty = current.doseQty.toDouble(),
                    doseUnit = current.doseUnit.trim(),
                    pillShape = current.pillShape,
                    pillColor1 = current.pillColor1,
                    pillColor2 = if (current.twoColors) current.pillColor2 else null,
                    startDate = current.startDate,
                    endDate = endDate,
                    isIndefinite = current.durationMode == DurationMode.ONGOING,
                    stock = if (current.hasStock) current.stock.toIntOrNull() else null,
                    lowStockThreshold = if (current.hasStock) {
                        current.lowStockThreshold.toIntOrNull()
                    } else {
                        null
                    },
                    notes = current.notes.trim().ifBlank { null },
                    archived = wasArchived,
                )
                val schedules = current.times.distinct().map { time ->
                    ScheduleTimeEntity(
                        medId = current.medId,
                        timeOfDay = time,
                        daysOfWeek = if (current.repeatMode == RepeatMode.WEEKDAYS) {
                            current.weekdays
                        } else {
                            emptySet()
                        },
                        intervalDays = if (current.repeatMode == RepeatMode.INTERVAL) {
                            current.intervalDays.toIntOrNull()
                        } else {
                            null
                        },
                    )
                }
                medicationRepository.save(medication, schedules)
                _state.update { it.copy(saving = false) }
                onSaved()
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        saving = false,
                        error = error.message ?: "Could not save the medication.",
                    )
                }
            }
        }
    }

    private fun validate(state: MedFormState, step: Int): String? = when (step) {
        STEP_BASICS -> when {
            state.name.isBlank() -> "Enter a medication name."
            (state.doseQty.toDoubleOrNull() ?: 0.0) <= 0.0 -> "Enter a dose quantity greater than zero."
            state.doseUnit.isBlank() -> "Enter a unit, for example tablets or ml."
            else -> null
        }
        STEP_SCHEDULE -> when {
            state.times.isEmpty() -> "Add at least one time of day."
            state.times.size > MAX_TIMES -> "You can set up to $MAX_TIMES times per day."
            state.times.distinct().size != state.times.size -> "Each time of day must be unique."
            state.repeatMode == RepeatMode.WEEKDAYS && state.weekdays.isEmpty() ->
                "Choose at least one weekday."
            state.repeatMode == RepeatMode.INTERVAL &&
                (state.intervalDays.toIntOrNull() ?: 0) !in 1..365 ->
                "Repeat interval must be between 1 and 365 days."
            else -> null
        }
        STEP_DURATION -> when (state.durationMode) {
            DurationMode.ONGOING -> null
            DurationMode.FIXED_DAYS ->
                if ((state.durationDays.toIntOrNull() ?: 0) < 1) {
                    "Enter a duration of at least 1 day."
                } else {
                    null
                }
            DurationMode.FIXED_END_DATE ->
                if (state.endDate.isBefore(state.startDate)) {
                    "The end date cannot be before the start date."
                } else {
                    null
                }
        }
        STEP_EXTRAS -> when {
            !state.hasStock -> null
            (state.stock.toIntOrNull() ?: -1) < 0 -> "Enter a stock count of zero or more."
            (state.lowStockThreshold.toIntOrNull() ?: -1) < 0 ->
                "Enter a low-stock threshold of zero or more."
            else -> null
        }
        else -> null
    }

    private fun MedicationWithSchedules.toFormState(profileName: String): MedFormState {
        wasArchived = medication.archived
        val sortedSchedules = schedules.sortedBy { it.timeOfDay }
        val repeatMode = when {
            sortedSchedules.any { it.intervalDays != null } -> RepeatMode.INTERVAL
            sortedSchedules.any { it.daysOfWeek.isNotEmpty() && it.daysOfWeek.size < 7 } ->
                RepeatMode.WEEKDAYS
            else -> RepeatMode.DAILY
        }
        val weekdays = sortedSchedules.firstOrNull { it.daysOfWeek.isNotEmpty() }?.daysOfWeek
            ?: WeekdaysDefault
        val interval = sortedSchedules.firstOrNull { it.intervalDays != null }?.intervalDays ?: 2
        val durationMode = if (medication.isIndefinite) {
            DurationMode.ONGOING
        } else {
            DurationMode.FIXED_END_DATE
        }
        val durationDays = if (!medication.isIndefinite && medication.endDate != null) {
            ChronoUnit.DAYS.between(medication.startDate, medication.endDate).plus(1).toString()
        } else {
            "30"
        }
        return MedFormState(
            medId = medication.id,
            profileName = profileName,
            name = medication.name,
            form = medication.form,
            doseQty = Format.qty(medication.doseQty),
            doseUnit = medication.doseUnit,
            notes = medication.notes.orEmpty(),
            pillShape = medication.pillShape,
            pillColor1 = medication.pillColor1,
            pillColor2 = medication.pillColor2 ?: PillPalette[1],
            twoColors = medication.pillColor2 != null,
            times = sortedSchedules.map { it.timeOfDay }.ifEmpty { listOf(LocalTime.of(9, 0)) },
            repeatMode = repeatMode,
            weekdays = weekdays,
            intervalDays = interval.toString(),
            startDate = medication.startDate,
            durationMode = durationMode,
            durationDays = durationDays,
            endDate = medication.endDate ?: medication.startDate.plusDays(29),
            hasStock = medication.stock != null,
            stock = medication.stock?.toString().orEmpty(),
            lowStockThreshold = medication.lowStockThreshold?.toString().orEmpty(),
            isEditing = true,
            loading = false,
        )
    }

    private fun defaultUnit(form: MedicationForm): String = when (form) {
        MedicationForm.TABLET -> "tablet"
        MedicationForm.CAPSULE -> "capsule"
        MedicationForm.LIQUID -> "ml"
        MedicationForm.INJECTION -> "syringe"
        MedicationForm.DROPS -> "drop"
        MedicationForm.INHALER -> "puff"
        MedicationForm.OTHER -> ""
    }

    companion object {
        const val STEP_BASICS = 0
        const val STEP_PILL = 1
        const val STEP_SCHEDULE = 2
        const val STEP_DURATION = 3
        const val STEP_EXTRAS = 4
        const val LAST_STEP = 4
        const val STEP_COUNT = 5
        const val MAX_TIMES = 6

        val STEP_TITLES = listOf("Basics", "Pill icon", "Schedule", "Duration", "Extras")
    }
}
