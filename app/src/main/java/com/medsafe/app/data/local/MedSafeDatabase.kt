package com.medsafe.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.medsafe.app.data.local.dao.DoseLogDao
import com.medsafe.app.data.local.dao.MedicationDao
import com.medsafe.app.data.local.dao.ProfileDao
import com.medsafe.app.data.local.dao.ScheduleTimeDao
import com.medsafe.app.data.local.entity.DoseLogEntity
import com.medsafe.app.data.local.entity.MedicationEntity
import com.medsafe.app.data.local.entity.ProfileEntity
import com.medsafe.app.data.local.entity.ScheduleTimeEntity

@Database(
    entities = [
        ProfileEntity::class,
        MedicationEntity::class,
        ScheduleTimeEntity::class,
        DoseLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MedSafeDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun medicationDao(): MedicationDao
    abstract fun scheduleTimeDao(): ScheduleTimeDao
    abstract fun doseLogDao(): DoseLogDao

    companion object {
        const val NAME = "medsafe.db"
    }
}
