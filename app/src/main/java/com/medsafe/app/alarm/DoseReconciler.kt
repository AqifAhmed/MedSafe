package com.medsafe.app.alarm

import com.medsafe.app.data.local.dao.DoseLogDao
import com.medsafe.app.data.local.dao.MedicationDao
import com.medsafe.app.data.model.DoseStatus
import com.medsafe.app.data.repository.MedicationRepository
import com.medsafe.app.data.settings.SettingsRepository
import com.medsafe.app.util.DateUtils
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repairs reminder state from the dose log after a reboot, app update, force-stop or clock
 * change:
 *  - re-arms snoozes the user explicitly requested,
 *  - marks doses left unanswered in an earlier digest window as MISSED,
 *  - records doses that came due while the app could not run, so History stays complete.
 *
 * Repair never posts a reminder: the only automatic notifications are the two daily digests.
 */
@Singleton
class DoseReconciler @Inject constructor(
    private val medicationDao: MedicationDao,
    private val doseLogDao: DoseLogDao,
    private val medicationRepository: MedicationRepository,
    private val reminderScheduler: ReminderScheduler,
    private val settingsRepository: SettingsRepository,
) {
    /**
     * @param since last time the alarm system was known healthy
     * ([ReminderScheduler.lastAliveAt], read before rebuilding alarms).
     */
    suspend fun reconcile(
        since: Long,
        now: Long = System.currentTimeMillis(),
    ) {
        val settings = settingsRepository.settings.first()
        rearmSnoozes(now, settings.snoozeMinutes)
        medicationRepository.markUnansweredBefore(now - ReconcilePlanner.MISSED_GRACE_MS)
        recordDosesMissedWhileAway(ReconcilePlanner.windowStart(since, now), now)
    }

    private suspend fun rearmSnoozes(now: Long, snoozeMinutes: Int) {
        doseLogDao.getSnoozed().forEach { log ->
            val medication = medicationDao.getById(log.medId) ?: return@forEach
            if (medication.archived) return@forEach
            if (reminderScheduler.isScheduled(ReminderActions.SNOOZE_FIRE, log.medId, log.scheduledAt)) {
                return@forEach
            }
            val fireAt = ReconcilePlanner.snoozeFireAt(now, log.actedAt, snoozeMinutes)
            reminderScheduler.scheduleSnoozeAt(log.medId, log.scheduledAt, fireAt)
        }
    }

    private suspend fun recordDosesMissedWhileAway(from: Long, now: Long) {
        val zone = ZoneId.systemDefault()
        val firstDay = DateUtils.toDate(from, zone)
        val lastDay = DateUtils.toDate(now, zone)
        medicationDao.getAllActiveWithSchedules().forEach { item ->
            var day = firstDay
            while (!day.isAfter(lastDay)) {
                ScheduleCalculator.occurrencesOn(item.medication, item.schedules, day, zone).forEach { instant ->
                    val at = instant.toEpochMilli()
                    if (at <= from || at > now) return@forEach
                    // A dose that fell before the medication existed was never due.
                    if (at < item.medication.createdAt) return@forEach
                    if (doseLogDao.get(item.medication.id, at) != null) return@forEach
                    medicationRepository.recordStatus(item.medication.id, at, DoseStatus.MISSED)
                }
                day = day.plusDays(1)
            }
        }
    }
}
