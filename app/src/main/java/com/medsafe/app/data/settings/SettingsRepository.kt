package com.medsafe.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val snoozeMinutes: Int = 10,
    val renudgeMinutes: Int = 15,
    val soundEnabled: Boolean = true,
    val onboardingComplete: Boolean = false,
    val activeProfileId: Long? = null,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "medsafe_settings",
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val snoozeMinutes = intPreferencesKey("snooze_minutes")
        val renudgeMinutes = intPreferencesKey("renudge_minutes")
        val soundEnabled = booleanPreferencesKey("sound_enabled")
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val activeProfileId = longPreferencesKey("active_profile_id")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            AppSettings(
                themeMode = prefs[Keys.themeMode]
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM,
                snoozeMinutes = prefs[Keys.snoozeMinutes] ?: 10,
                renudgeMinutes = prefs[Keys.renudgeMinutes] ?: 15,
                soundEnabled = prefs[Keys.soundEnabled] ?: true,
                onboardingComplete = prefs[Keys.onboardingComplete] ?: false,
                activeProfileId = prefs[Keys.activeProfileId],
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.themeMode] = mode.name }
    }

    suspend fun setSnoozeMinutes(minutes: Int) {
        context.settingsDataStore.edit { it[Keys.snoozeMinutes] = minutes.coerceIn(5, 60) }
    }

    suspend fun setRenudgeMinutes(minutes: Int) {
        context.settingsDataStore.edit { it[Keys.renudgeMinutes] = minutes.coerceIn(5, 60) }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.soundEnabled] = enabled }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.settingsDataStore.edit { it[Keys.onboardingComplete] = complete }
    }

    suspend fun setActiveProfileId(id: Long?) {
        context.settingsDataStore.edit { prefs ->
            if (id == null) prefs.remove(Keys.activeProfileId) else prefs[Keys.activeProfileId] = id
        }
    }
}
