package com.medsafe.app.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medsafe.app.alarm.DoseActionHandler
import com.medsafe.app.alarm.ScheduleCalculator
import com.medsafe.app.data.local.entity.MedicationEntity
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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class TodayDose(
    val medication: MedicationEntity,
    val time: LocalTime,
    val scheduledAt: Long,
    val status: DoseStatus,
)

data class TodayUiState(
    val loading: Boolean = true,
    val profile: ProfileEntity? = null,
    val profiles: List<ProfileEntity> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val doses: List<TodayDose> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    medicationRepository: MedicationRepository,
    private val doseActionHandler: DoseActionHandler,
) : ViewModel() {

    private val date = MutableStateFlow(LocalDate.now())

    private val activeProfile = combine(
        profileRepository.profiles,
        settingsRepository.settings,
    ) { profiles, settings ->
        profiles.firstOrNull { it.id == settings.activeProfileId }
            ?: profiles.firstOrNull { it.isMain }
            ?: profiles.firstOrNull()
    }

    private val medications = activeProfile.flatMapLatest { profile ->
        if (profile == null) flowOf(emptyList())
        else medicationRepository.observeActiveWithSchedules(profile.id)
    }

    private val logs = combine(activeProfile, date) { profile, day -> profile to day }
        .flatMapLatest { (profile, day) ->
            if (profile == null) {
                flowOf(emptyList())
            } else {
                medicationRepository.observeLogs(
                    profileId = profile.id,
                    fromMillis = DateUtils.startOfDay(day),
                    toMillis = DateUtils.startOfNextDay(day),
                )
            }
        }

    val uiState: StateFlow<TodayUiState> = combine(
        activeProfile,
        profileRepository.profiles,
        medications,
        logs,
        date,
    ) { profile, profiles, medicationList, dayLogs, day ->
        val logByKey = dayLogs.associateBy { it.log.medId to it.log.scheduledAt }
        val doses = medicationList.flatMap { item ->
            item.schedules
                .filter { ScheduleCalculator.occursOn(item.medication, it, day) }
                .map { schedule ->
                    val scheduledAt = DateUtils.instantOf(
                        date = day,
                        hour = schedule.timeOfDay.hour,
                        minute = schedule.timeOfDay.minute,
                    ).toEpochMilli()
                    TodayDose(
                        medication = item.medication,
                        time = schedule.timeOfDay,
                        scheduledAt = scheduledAt,
                        status = logByKey[item.medication.id to scheduledAt]?.log?.status
                            ?: DoseStatus.PENDING,
                    )
                }
        }.sortedBy { it.scheduledAt }
        TodayUiState(
            loading = false,
            profile = profile,
            profiles = profiles,
            today = day,
            doses = doses,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState(),
    )

    fun refresh() {
        date.value = LocalDate.now()
    }

    fun switchProfile(profileId: Long) {
        viewModelScope.launch { profileRepository.setActiveProfile(profileId) }
    }

    fun markTaken(dose: TodayDose) {
        viewModelScope.launch {
            doseActionHandler.markTaken(dose.medication.id, dose.scheduledAt)
        }
    }

    fun snooze(dose: TodayDose) {
        viewModelScope.launch {
            doseActionHandler.snooze(dose.medication.id, dose.scheduledAt)
        }
    }

    fun skip(dose: TodayDose) {
        viewModelScope.launch {
            doseActionHandler.markSkipped(dose.medication.id, dose.scheduledAt)
        }
    }
}
