package com.medsafe.app.data.repository

import androidx.room.withTransaction
import com.medsafe.app.data.local.MedSafeDatabase
import com.medsafe.app.data.local.dao.DoseLogDao
import com.medsafe.app.data.local.dao.MedicationDao
import com.medsafe.app.data.local.dao.ScheduleTimeDao
import com.medsafe.app.data.local.entity.DoseLogEntity
import com.medsafe.app.data.local.entity.DoseLogWithMedication
import com.medsafe.app.data.local.entity.MedicationEntity
import com.medsafe.app.data.local.entity.MedicationWithSchedules
import com.medsafe.app.data.local.entity.ScheduleTimeEntity
import com.medsafe.app.data.model.DoseStatus
import com.medsafe.app.di.ApplicationScope
import com.medsafe.app.alarm.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationRepository @Inject constructor(
    private val database: MedSafeDatabase,
    private val medicationDao: MedicationDao,
    private val scheduleTimeDao: ScheduleTimeDao,
    private val doseLogDao: DoseLogDao,
    private val alarmScheduler: AlarmScheduler,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    fun observeAllWithSchedules(profileId: Long): Flow<List<MedicationWithSchedules>> =
        medicationDao.observeAllWithSchedules(profileId)

    fun observeActiveWithSchedules(profileId: Long): Flow<List<MedicationWithSchedules>> =
        medicationDao.observeActiveWithSchedules(profileId)

    suspend fun getWithSchedules(medId: Long): MedicationWithSchedules? =
        medicationDao.getWithSchedules(medId)

    /**
     * Creates or updates a medication and its schedule times in one transaction, then
     * rebuilds alarms. Room cascades take care of the old schedule rows.
     */
    suspend fun save(
        medication: MedicationEntity,
        schedules: List<ScheduleTimeEntity>,
    ): Long {
        val medId = database.withTransaction {
            val id = if (medication.id == 0L) {
                medicationDao.insert(medication)
            } else {
                medicationDao.update(medication)
                medication.id
            }
            scheduleTimeDao.deleteForMed(id)
            if (schedules.isNotEmpty()) {
                scheduleTimeDao.insertAll(schedules.map { it.copy(id = 0, medId = id) })
            }
            id
        }
        reschedule()
        return medId
    }

    suspend fun setArchived(medId: Long, archived: Boolean) {
        val med = medicationDao.getById(medId) ?: return
        medicationDao.update(med.copy(archived = archived))
        reschedule()
    }

    suspend fun delete(medId: Long) {
        val med = medicationDao.getById(medId) ?: return
        medicationDao.delete(med)
        alarmScheduler.cancelAll()
        reschedule()
    }

    suspend fun archiveExpired(today: LocalDate = LocalDate.now()): Int {
        val archived = medicationDao.archiveExpired(today)
        if (archived > 0) reschedule()
        return archived
    }

    suspend fun decrementStock(medId: Long, amount: Int) {
        medicationDao.decrementStock(medId, amount)
    }

    /** Alarms are cheap to rebuild, and rebuilding is the safest way to drop stale ones. */
    fun reschedule() {
        appScope.launch { alarmScheduler.scheduleAll() }
    }

    suspend fun getLogsForMed(medId: Long): List<DoseLogEntity> = doseLogDao.getForMed(medId)

    fun observeLogs(profileId: Long, fromMillis: Long, toMillis: Long): Flow<List<DoseLogWithMedication>> =
        doseLogDao.observeForProfileBetween(profileId, fromMillis, toMillis)

    suspend fun recordStatus(
        medId: Long,
        scheduledAt: Long,
        status: DoseStatus,
    ): DoseLogEntity {
        val now = System.currentTimeMillis()
        val existing = doseLogDao.get(medId, scheduledAt)
        if (existing == null) {
            doseLogDao.insert(
                DoseLogEntity(medId = medId, scheduledAt = scheduledAt, status = status, actedAt = now),
            )
            return doseLogDao.get(medId, scheduledAt)
                ?: DoseLogEntity(medId = medId, scheduledAt = scheduledAt, status = status, actedAt = now)
        }
        doseLogDao.updateStatus(existing.id, status, now)
        return existing.copy(status = status, actedAt = now)
    }

    /** Creates a PENDING log if none exists and returns the current row. */
    suspend fun ensurePendingLog(medId: Long, scheduledAt: Long): DoseLogEntity {
        val existing = doseLogDao.get(medId, scheduledAt)
        if (existing != null) return existing
        doseLogDao.insert(
            DoseLogEntity(medId = medId, scheduledAt = scheduledAt, status = DoseStatus.PENDING),
        )
        return doseLogDao.get(medId, scheduledAt)
            ?: DoseLogEntity(medId = medId, scheduledAt = scheduledAt, status = DoseStatus.PENDING)
    }
}
