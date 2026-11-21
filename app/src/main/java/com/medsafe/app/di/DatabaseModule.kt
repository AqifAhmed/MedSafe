package com.medsafe.app.di

import android.content.Context
import androidx.room.Room
import com.medsafe.app.data.local.MedSafeDatabase
import com.medsafe.app.data.local.MedSafeMigrations
import com.medsafe.app.data.local.dao.DoseLogDao
import com.medsafe.app.data.local.dao.MedicationDao
import com.medsafe.app.data.local.dao.ProfileDao
import com.medsafe.app.data.local.dao.ScheduleTimeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MedSafeDatabase =
        Room.databaseBuilder(context, MedSafeDatabase::class.java, MedSafeDatabase.NAME)
            .addMigrations(*MedSafeMigrations.ALL)
            .build()

    @Provides
    fun provideProfileDao(db: MedSafeDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideMedicationDao(db: MedSafeDatabase): MedicationDao = db.medicationDao()

    @Provides
    fun provideScheduleTimeDao(db: MedSafeDatabase): ScheduleTimeDao = db.scheduleTimeDao()

    @Provides
    fun provideDoseLogDao(db: MedSafeDatabase): DoseLogDao = db.doseLogDao()

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
