package com.medsafe.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.medsafe.app.data.local.entity.MedicationEntity
import com.medsafe.app.data.local.entity.MedicationWithSchedules
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MedicationDao {
    @Transaction
    @Query("SELECT * FROM medications WHERE profileId = :profileId ORDER BY archived ASC, name COLLATE NOCASE ASC")
    fun observeAllWithSchedules(profileId: Long): Flow<List<MedicationWithSchedules>>

    @Transaction
    @Query("SELECT * FROM medications WHERE profileId = :profileId AND archived = 0 ORDER BY name COLLATE NOCASE ASC")
    fun observeActiveWithSchedules(profileId: Long): Flow<List<MedicationWithSchedules>>

    @Transaction
    @Query("SELECT * FROM medications WHERE archived = 0")
    suspend fun getAllActiveWithSchedules(): List<MedicationWithSchedules>

    @Transaction
    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getWithSchedules(id: Long): MedicationWithSchedules?

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: Long): MedicationEntity?

    @Query("SELECT * FROM medications WHERE profileId = :profileId")
    suspend fun getForProfile(profileId: Long): List<MedicationEntity>

    @Insert
    suspend fun insert(medication: MedicationEntity): Long

    @Update
    suspend fun update(medication: MedicationEntity)

    @Delete
    suspend fun delete(medication: MedicationEntity)

    @Query("UPDATE medications SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query(
        "UPDATE medications SET archived = 1 " +
            "WHERE archived = 0 AND isIndefinite = 0 AND endDate IS NOT NULL AND endDate < :today",
    )
    suspend fun archiveExpired(today: LocalDate): Int

    @Query("UPDATE medications SET stock = MAX(0, stock - :amount) WHERE id = :id AND stock IS NOT NULL")
    suspend fun decrementStock(id: Long, amount: Int)
}
