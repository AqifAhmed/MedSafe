package com.medsafe.app.alarm

import com.medsafe.app.data.local.dao.MedicationDao
import com.medsafe.app.data.local.dao.ProfileDao
import com.medsafe.app.data.model.DoseStatus
import com.medsafe.app.data.repository.MedicationRepository
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Everything that happens when an alarm fires.
 *
 * There are only two automatic reminders per day: a morning digest and an evening digest. Each
 * digest lists every dose in the twelve hours that follow it, so a medication with several
 * daily times still produces one notification. Snooze is the only per-dose notification and is
 * always user-requested.
 */
@Singleton
class DoseReminderProcessor @Inject constructor(
    private val medicationDao: MedicationDao,
    private val profileDao: ProfileDao,
    private val medicationRepository: MedicationRepository,
    private val notificationHelper: NotificationHelper,
    private val reminderScheduler: ReminderScheduler,
) {
    suspend fun process(action: String, medId: Long, scheduledAt: Long) {
        reminderScheduler.markFired(action, medId, scheduledAt)
        when (action) {
            ReminderActions.DIGEST -> postDigest(scheduledAt)
            ReminderActions.SNOOZE_FIRE -> fireSnooze(medId, scheduledAt)
        }
    }

    private suspend fun postDigest(digestAt: Long) {
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(digestAt)
        val end = ScheduleCalculator.nextDigest(start, zone)

        // Anything still waiting from an earlier window is now over.
        medicationRepository.markUnansweredBefore(digestAt)

        val entries = mutableListOf<DigestEntry>()
        medicationDao.getAllActiveWithSchedules().forEach { item ->
            val profile = profileDao.getById(item.medication.profileId) ?: return@forEach
            ScheduleCalculator.occurrencesBetween(item.medication, item.schedules, start, end, zone)
                .forEach { instant ->
                    val at = instant.toEpochMilli()
                    val log = medicationRepository.ensurePendingLog(item.medication.id, at)
                    // Snoozed doses have their own follow-up alarm; do not list them twice.
                    if (log.status == DoseStatus.PENDING) {
                        entries += DigestEntry(
                            profileName = profile.name,
                            medication = item.medication,
                            scheduledAt = at,
                        )
                    }
                }
        }

        notificationHelper.showDigest(
            entries = entries.sortedBy { it.scheduledAt },
            evening = start.atZone(zone).hour >= 12,
        )
        reminderScheduler.scheduleNextDigest(after = start)
    }

    private suspend fun fireSnooze(medId: Long, scheduledAt: Long) {
        val medication = medicationDao.getById(medId) ?: return
        if (medication.archived) return
        val log = medicationRepository.ensurePendingLog(medId, scheduledAt)
        if (log.status == DoseStatus.TAKEN || log.status == DoseStatus.SKIPPED) return
        if (log.status == DoseStatus.SNOOZED) {
            medicationRepository.recordStatus(medId, scheduledAt, DoseStatus.PENDING)
        }
        val profile = profileDao.getById(medication.profileId) ?: return
        notificationHelper.showDoseReminder(
            profileName = profile.name,
            medication = medication,
            scheduledAt = scheduledAt,
            missed = false,
        )
    }
}
