package com.medsafe.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medsafe.app.data.local.entity.ScheduleTimeEntity

@Dao
interface ScheduleTimeDao {
    @Query("SELECT * FROM schedule_times WHERE medId = :medId ORDER BY timeOfDay")
    suspend fun getForMed(medId: Long): List<ScheduleTimeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<ScheduleTimeEntity>)

    @Query("DELETE FROM schedule_times WHERE medId = :medId")
    suspend fun deleteForMed(medId: Long)
}
