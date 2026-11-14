package com.medsafe.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.medsafe.app.data.local.dao.MedicationDao
import com.medsafe.app.data.local.dao.ProfileDao
import com.medsafe.app.data.model.DoseStatus
import com.medsafe.app.data.repository.MedicationRepository
import com.medsafe.app.data.settings.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * Receives the alarm itself, the one-time re-nudge, and snooze expirations. All work
 * happens off the main thread via goAsync(), and posts the notification quickly.
 */
@AndroidEntryPoint
class DoseAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var medicationDao: MedicationDao
    @Inject lateinit var profileDao: ProfileDao
    @Inject lateinit var medicationRepository: MedicationRepository
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in AlarmActions.doseAlarmActions) return
        val medId = intent.getLongExtra(AlarmActions.EXTRA_MED_ID, -1L)
        val scheduledAt = intent.getLongExtra(AlarmActions.EXTRA_SCHEDULED_AT, -1L)
        if (medId <= 0L || scheduledAt <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handle(action, intent, medId, scheduledAt)
            } catch (error: Throwable) {
                Log.e(TAG, "Dose alarm handling failed", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handle(
        action: String,
        intent: Intent,
        medId: Long,
        scheduledAt: Long,
    ) {
        val medication = medicationDao.getById(medId) ?: return
        if (medication.archived) return
        if (!medication.isIndefinite &&
            medication.endDate != null &&
            LocalDate.now().isAfter(medication.endDate)
        ) {
            return
        }
        val profile = profileDao.getById(medication.profileId) ?: return
        val log = medicationRepository.ensurePendingLog(medId, scheduledAt)

        when (action) {
            AlarmActions.DOSE -> {
                if (log.status == DoseStatus.TAKEN || log.status == DoseStatus.SKIPPED) return
                notificationHelper.showDoseReminder(
                    profileName = profile.name,
                    medication = medication,
                    scheduledAt = scheduledAt,
                    missed = log.status == DoseStatus.MISSED,
                )
                val settings = settingsRepository.settings.first()
                alarmScheduler.scheduleRenudge(medId, scheduledAt, settings.renudgeMinutes)

                intent.getStringExtra(AlarmActions.EXTRA_TIME)?.let { raw ->
                    runCatching { LocalTime.parse(raw) }.getOrNull()?.let { time ->
                        alarmScheduler.scheduleNext(medId, time)
                    }
                }
            }

            AlarmActions.RENUDGE -> {
                if (log.status == DoseStatus.PENDING) {
                    medicationRepository.recordStatus(medId, scheduledAt, DoseStatus.MISSED)
                    notificationHelper.showDoseReminder(
                        profileName = profile.name,
                        medication = medication,
                        scheduledAt = scheduledAt,
                        missed = true,
                    )
                }
            }

            AlarmActions.SNOOZE_FIRE -> {
                if (log.status == DoseStatus.SNOOZED) {
                    medicationRepository.recordStatus(medId, scheduledAt, DoseStatus.PENDING)
                }
                notificationHelper.showDoseReminder(
                    profileName = profile.name,
                    medication = medication,
                    scheduledAt = scheduledAt,
                    missed = false,
                )
                val settings = settingsRepository.settings.first()
                alarmScheduler.scheduleRenudge(medId, scheduledAt, settings.renudgeMinutes)
            }
        }
    }

    companion object {
        private const val TAG = "DoseAlarmReceiver"
    }
}
