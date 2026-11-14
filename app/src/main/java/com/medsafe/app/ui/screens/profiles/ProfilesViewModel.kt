package com.medsafe.app.ui.screens.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medsafe.app.data.local.entity.ProfileEntity
import com.medsafe.app.data.repository.ProfileLimitException
import com.medsafe.app.data.repository.ProfileRepository
import com.medsafe.app.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfilesUiState(
    val loading: Boolean = true,
    val profiles: List<ProfileEntity> = emptyList(),
    val activeProfileId: Long? = null,
) {
    val canAdd: Boolean get() = profiles.size < ProfileEntity.MAX_PROFILES
    val maxProfiles: Int get() = ProfileEntity.MAX_PROFILES
}

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfilesUiState> = combine(
        profileRepository.profiles,
        settingsRepository.settings,
    ) { profiles, settings ->
        val active = profiles.firstOrNull { it.id == settings.activeProfileId }
            ?: profiles.firstOrNull { it.isMain }
            ?: profiles.firstOrNull()
        ProfilesUiState(
            loading = false,
            profiles = profiles,
            activeProfileId = active?.id,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfilesUiState(),
    )

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    fun addProfile(name: String, color: Int) {
        viewModelScope.launch {
            profileRepository.addProfile(name, color)
                .onSuccess { id ->
                    profileRepository.setActiveProfile(id)
                    _error.value = null
                }
                .onFailure { error ->
                    _error.value = if (error is ProfileLimitException) {
                        "MedSafe allows up to 3 profiles."
                    } else {
                        error.message ?: "Could not add the profile."
                    }
                }
        }
    }

    fun updateProfile(profile: ProfileEntity, name: String, color: Int) {
        viewModelScope.launch {
            profileRepository.updateProfile(profile.copy(name = name, color = color))
        }
    }

    fun deleteProfile(profileId: Long) {
        viewModelScope.launch {
            val deleted = profileRepository.deleteProfile(profileId)
            if (!deleted) _error.value = "The main profile cannot be deleted."
        }
    }

    fun setActiveProfile(profileId: Long) {
        viewModelScope.launch { profileRepository.setActiveProfile(profileId) }
    }
}
