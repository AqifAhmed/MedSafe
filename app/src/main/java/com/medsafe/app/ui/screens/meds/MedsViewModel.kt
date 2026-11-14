package com.medsafe.app.ui.screens.meds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medsafe.app.data.local.entity.MedicationWithSchedules
import com.medsafe.app.data.local.entity.ProfileEntity
import com.medsafe.app.data.repository.MedicationRepository
import com.medsafe.app.data.repository.ProfileRepository
import com.medsafe.app.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MedsUiState(
    val loading: Boolean = true,
    val profile: ProfileEntity? = null,
    val medications: List<MedicationWithSchedules> = emptyList(),
) {
    val active: List<MedicationWithSchedules> get() = medications.filter { !it.medication.archived }
    val archived: List<MedicationWithSchedules> get() = medications.filter { it.medication.archived }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MedsViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    profileRepository: ProfileRepository,
    private val medicationRepository: MedicationRepository,
) : ViewModel() {

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
        else medicationRepository.observeAllWithSchedules(profile.id)
    }

    val uiState: StateFlow<MedsUiState> = combine(activeProfile, medications) { profile, list ->
        MedsUiState(loading = false, profile = profile, medications = list)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MedsUiState(),
    )

    fun setArchived(medId: Long, archived: Boolean) {
        viewModelScope.launch { medicationRepository.setArchived(medId, archived) }
    }

    fun delete(medId: Long) {
        viewModelScope.launch { medicationRepository.delete(medId) }
    }
}
