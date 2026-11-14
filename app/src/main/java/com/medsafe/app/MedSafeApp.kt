package com.medsafe.app

import android.app.Application
import com.medsafe.app.alarm.AlarmScheduler
import com.medsafe.app.alarm.NotificationHelper
import com.medsafe.app.data.repository.MedicationRepository
import com.medsafe.app.data.repository.ProfileRepository
import com.medsafe.app.data.settings.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MedSafeApp : Application() {

    @Inject lateinit var profileRepository: ProfileRepository
    @Inject lateinit var medicationRepository: MedicationRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var settingsRepository: SettingsRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            val settings = settingsRepository.settings.first()
            notificationHelper.ensureChannels(soundEnabled = settings.soundEnabled)
            profileRepository.ensureMainProfile()
            medicationRepository.archiveExpired()
            alarmScheduler.scheduleAll()
        }
    }
}
