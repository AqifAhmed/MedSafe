package com.medsafe.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medsafe.app.data.local.entity.ProfileEntity
import com.medsafe.app.data.repository.ProfileRepository
import com.medsafe.app.data.settings.AppSettings
import com.medsafe.app.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    profileRepository: ProfileRepository,
) : ViewModel() {

    /** Null until DataStore has emitted, so the app can avoid flashing onboarding. */
    val settings: StateFlow<AppSettings?> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val profiles: StateFlow<List<ProfileEntity>> = profileRepository.profiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )
}
