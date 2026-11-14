package com.medsafe.app.alarm

import com.medsafe.app.data.local.dao.MedicationDao
import com.medsafe.app.data.local.dao.ProfileDao
import com.medsafe.app.data.model.DoseStatus
import com.medsafe.app.data.repository.MedicationRepository
import com.medsafe.app.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

/**
 * Single place where a dose outcome is recorded. Both notification actions and the
 * in-app Today screen funnel through here so stock and alarms stay consistent.
 */
@Singleton
class DoseActionHandler @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val medicationDao: MedicationDao,
    private val profileDao: ProfileDao,
    private val notificationHelper: NotificationHelper,
    private val alarmScheduler: AlarmScheduler,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun markTaken(medId: Long, scheduledAt: Long) {
        val medication = medicationDao.getById(medId) ?: return
        val existing = medicationRepository.ensurePendingLog(medId, scheduledAt)
        notificationHelper.cancelDose(medId, scheduledAt)
        alarmScheduler.cancelRenudge(medId, scheduledAt)
        alarmScheduler.cancelSnooze(medId, scheduledAt)
        if (existing.status == DoseStatus.TAKEN) return

        medicationRepository.recordStatus(medId, scheduledAt, DoseStatus.TAKEN)
        if (medication.stock != null) {
            val amount = ceil(medication.doseQty).toInt().coerceAtLeast(1)
            medicationRepository.decrementStock(medId, amount)
            val updated = medicationDao.getById(medId) ?: return
            val threshold = updated.lowStockThreshold
            if (threshold != null && updated.stock != null && updated.stock <= threshold) {
                val profileName = profileDao.getById(updated.profileId)?.name ?: "MedSafe"
                notificationHelper.showLowStock(profileName, updated)
            }
        }
    }

    suspend fun markSkipped(medId: Long, scheduledAt: Long) {
        medicationRepository.ensurePendingLog(medId, scheduledAt)
        medicationRepository.recordStatus(medId, scheduledAt, DoseStatus.SKIPPED)
        notificationHelper.cancelDose(medId, scheduledAt)
        alarmScheduler.cancelRenudge(medId, scheduledAt)
        alarmScheduler.cancelSnooze(medId, scheduledAt)
    }

    suspend fun snooze(medId: Long, scheduledAt: Long) {
        medicationRepository.ensurePendingLog(medId, scheduledAt)
        medicationRepository.recordStatus(medId, scheduledAt, DoseStatus.SNOOZED)
        notificationHelper.cancelDose(medId, scheduledAt)
        alarmScheduler.cancelRenudge(medId, scheduledAt)
        val settings = settingsRepository.settings.first()
        alarmScheduler.scheduleSnooze(medId, scheduledAt, settings.snoozeMinutes)
    }
}
