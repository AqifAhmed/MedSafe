package com.medsafe.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medsafe.app.data.local.entity.DoseLogWithMedication
import com.medsafe.app.data.local.entity.ProfileEntity
import com.medsafe.app.data.model.DoseStatus
import com.medsafe.app.data.repository.MedicationRepository
import com.medsafe.app.data.repository.ProfileRepository
import com.medsafe.app.data.settings.SettingsRepository
import com.medsafe.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class HistoryDay(
    val taken: Int = 0,
    val missed: Int = 0,
    val skipped: Int = 0,
    val pending: Int = 0,
) {
    val total: Int get() = taken + missed + skipped + pending
}

data class HistoryUiState(
    val loading: Boolean = true,
    val profile: ProfileEntity? = null,
    val month: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val days: Map<LocalDate, HistoryDay> = emptyMap(),
    val selectedLogs: List<DoseLogWithMedication> = emptyList(),
    val monthTaken: Int = 0,
    val monthMissed: Int = 0,
    val monthSkipped: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    profileRepository: ProfileRepository,
    medicationRepository: MedicationRepository,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow(LocalDate.now())

    private val activeProfile = combine(
        profileRepository.profiles,
        settingsRepository.settings,
    ) { profiles, settings ->
        profiles.firstOrNull { it.id == settings.activeProfileId }
            ?: profiles.firstOrNull { it.isMain }
            ?: profiles.firstOrNull()
    }

    private val logs = combine(activeProfile, month) { profile, selectedMonth ->
        profile to selectedMonth
    }.flatMapLatest { (profile, selectedMonth) ->
        if (profile == null) {
            flowOf(emptyList())
        } else {
            medicationRepository.observeLogs(
                profileId = profile.id,
                fromMillis = DateUtils.startOfDay(selectedMonth.atDay(1)),
                toMillis = DateUtils.startOfDay(selectedMonth.plusMonths(1).atDay(1)),
            )
        }
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        activeProfile,
        month,
        selectedDate,
        logs,
    ) { profile, selectedMonth, selectedDay, monthLogs ->
        val days = mutableMapOf<LocalDate, HistoryDay>()
        monthLogs.forEach { entry ->
            val date = DateUtils.toDate(entry.log.scheduledAt)
            val current = days[date] ?: HistoryDay()
            days[date] = when (entry.log.status) {
                DoseStatus.TAKEN -> current.copy(taken = current.taken + 1)
                DoseStatus.MISSED -> current.copy(missed = current.missed + 1)
                DoseStatus.SKIPPED -> current.copy(skipped = current.skipped + 1)
                DoseStatus.PENDING, DoseStatus.SNOOZED -> current.copy(pending = current.pending + 1)
            }
        }
        val selectedLogs = monthLogs
            .filter { DateUtils.toDate(it.log.scheduledAt) == selectedDay }
            .sortedBy { it.log.scheduledAt }
        HistoryUiState(
            loading = false,
            profile = profile,
            month = selectedMonth,
            selectedDate = selectedDay,
            days = days,
            selectedLogs = selectedLogs,
            monthTaken = days.values.sumOf { it.taken },
            monthMissed = days.values.sumOf { it.missed },
            monthSkipped = days.values.sumOf { it.skipped },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(),
    )

    fun previousMonth() {
        shiftMonth(-1)
    }

    fun nextMonth() {
        shiftMonth(1)
    }

    fun goToCurrentMonth() {
        val today = LocalDate.now()
        month.value = YearMonth.from(today)
        selectedDate.value = today
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    private fun shiftMonth(delta: Long) {
        val next = month.value.plusMonths(delta)
        month.value = next
        val day = selectedDate.value.dayOfMonth.coerceAtMost(next.lengthOfMonth())
        selectedDate.value = next.atDay(day)
    }
}
