package com.medsafe.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.medsafe.app.data.local.entity.DoseLogEntity
import com.medsafe.app.data.local.entity.DoseLogWithMedication
import com.medsafe.app.data.model.DoseStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: DoseLogEntity): Long

    @Query("SELECT * FROM dose_logs WHERE medId = :medId AND scheduledAt = :scheduledAt LIMIT 1")
    suspend fun get(medId: Long, scheduledAt: Long): DoseLogEntity?

    @Query("UPDATE dose_logs SET status = :status, actedAt = :actedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: DoseStatus, actedAt: Long?)

    @Transaction
    @Query(
        "SELECT dose_logs.* FROM dose_logs " +
            "INNER JOIN medications ON medications.id = dose_logs.medId " +
            "WHERE medications.profileId = :profileId " +
            "AND dose_logs.scheduledAt >= :from AND dose_logs.scheduledAt < :to " +
            "ORDER BY dose_logs.scheduledAt ASC",
    )
    fun observeForProfileBetween(
        profileId: Long,
        from: Long,
        to: Long,
    ): Flow<List<DoseLogWithMedication>>

    @Query("SELECT * FROM dose_logs WHERE medId = :medId ORDER BY scheduledAt DESC")
    suspend fun getForMed(medId: Long): List<DoseLogEntity>

    @Query("DELETE FROM dose_logs WHERE medId = :medId")
    suspend fun deleteForMed(medId: Long)
}
