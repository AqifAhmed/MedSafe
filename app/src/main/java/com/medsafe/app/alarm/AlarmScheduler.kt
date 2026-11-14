package com.medsafe.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.medsafe.app.data.local.dao.MedicationDao
import com.medsafe.app.data.local.dao.ScheduleTimeDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

object AlarmActions {
    const val DOSE = "com.medsafe.app.action.DOSE_ALARM"
    const val RENUDGE = "com.medsafe.app.action.RENUDGE"
    const val SNOOZE_FIRE = "com.medsafe.app.action.SNOOZE_FIRE"
    const val TAKE = "com.medsafe.app.action.TAKE"
    const val SKIP = "com.medsafe.app.action.SKIP"
    const val SNOOZE = "com.medsafe.app.action.SNOOZE"

    const val EXTRA_MED_ID = "com.medsafe.app.extra.MED_ID"
    const val EXTRA_SCHEDULED_AT = "com.medsafe.app.extra.SCHEDULED_AT"
    const val EXTRA_TIME = "com.medsafe.app.extra.TIME"

    val doseAlarmActions = setOf(DOSE, RENUDGE, SNOOZE_FIRE)
}

/**
 * Schedules exact alarms for each medication time. All bookkeeping lives in
 * SharedPreferences so [cancelAll] can drop every alarm this app created, including
 * re-nudges and snoozes.
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val medicationDao: MedicationDao,
    private val scheduleTimeDao: ScheduleTimeDao,
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(AlarmManager::class.java)

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** Rebuilds every alarm from the database. Called on launch, boot, time change and app update. */
    suspend fun scheduleAll() {
        mutex.withLock {
            cancelAll()
            val now = Instant.now()
            medicationDao.getAllActiveWithSchedules().forEach { item ->
                item.schedules.forEach { schedule ->
                    val next = ScheduleCalculator.nextTrigger(item.medication, schedule, now) ?: return@forEach
                    scheduleDoseAlarm(item.medication.id, schedule.timeOfDay, next)
                }
            }
        }
    }

    /** Queues the next occurrence of one schedule after [after]. */
    suspend fun scheduleNext(
        medId: Long,
        time: LocalTime,
        after: Instant = Instant.now(),
    ) {
        val medication = medicationDao.getById(medId) ?: return
        if (medication.archived) return
        val schedule = scheduleTimeDao.getForMed(medId).firstOrNull { it.timeOfDay == time } ?: return
        val next = ScheduleCalculator.nextTrigger(medication, schedule, after) ?: return
        scheduleDoseAlarm(medId, time, next)
    }

    private fun scheduleDoseAlarm(medId: Long, time: LocalTime, triggerAt: Instant) {
        val triggerMillis = triggerAt.toEpochMilli()
        setAlarm(
            medId = medId,
            triggerAtMillis = triggerMillis,
            action = AlarmActions.DOSE,
            scheduledAt = triggerMillis,
            time = time,
        )
    }

    fun scheduleRenudge(medId: Long, scheduledAt: Long, minutes: Int) {
        val triggerAt = System.currentTimeMillis() + minutes.coerceAtLeast(1) * 60_000L
        setAlarm(medId, triggerAt, AlarmActions.RENUDGE, scheduledAt)
    }

    fun cancelRenudge(medId: Long, scheduledAt: Long) {
        cancelAlarm(medId, AlarmActions.RENUDGE, scheduledAt)
    }

    fun scheduleSnooze(medId: Long, scheduledAt: Long, minutes: Int) {
        val triggerAt = System.currentTimeMillis() + minutes.coerceAtLeast(1) * 60_000L
        setAlarm(medId, triggerAt, AlarmActions.SNOOZE_FIRE, scheduledAt)
    }

    fun cancelSnooze(medId: Long, scheduledAt: Long) {
        cancelAlarm(medId, AlarmActions.SNOOZE_FIRE, scheduledAt)
    }

    /** Cancels every alarm recorded by this scheduler. */
    fun cancelAll() {
        val keys = prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty().toList()
        keys.forEach { key ->
            val parts = key.split('|')
            if (parts.size != 3) return@forEach
            val medId = parts[0].toLongOrNull() ?: return@forEach
            val action = parts[1]
            val scheduledAt = parts[2].toLongOrNull() ?: return@forEach
            alarmManager.cancel(buildPendingIntent(medId, action, scheduledAt, null))
        }
        prefs.edit().remove(KEY_SCHEDULED).apply()
    }

    private fun setAlarm(
        medId: Long,
        triggerAtMillis: Long,
        action: String,
        scheduledAt: Long,
        time: LocalTime? = null,
    ) {
        val pendingIntent = buildPendingIntent(medId, action, scheduledAt, time)
        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            } else {
                // Documented fallback: inexact but still allowed while idle. The UI shows
                // a warning while exact alarms are not granted.
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            }
            trackAlarm(medId, action, scheduledAt)
        } catch (security: SecurityException) {
            Log.w(TAG, "Cannot schedule alarm $action for med $medId", security)
        }
    }

    private fun cancelAlarm(medId: Long, action: String, scheduledAt: Long) {
        alarmManager.cancel(buildPendingIntent(medId, action, scheduledAt, null))
        untrackAlarm(medId, action, scheduledAt)
    }

    private fun buildPendingIntent(
        medId: Long,
        action: String,
        scheduledAt: Long,
        time: LocalTime?,
    ): PendingIntent {
        val intent = Intent(context, DoseAlarmReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmActions.EXTRA_MED_ID, medId)
            putExtra(AlarmActions.EXTRA_SCHEDULED_AT, scheduledAt)
            if (time != null) putExtra(AlarmActions.EXTRA_TIME, time.toString())
        }
        return PendingIntent.getBroadcast(
            context,
            alarmKey(medId, action, scheduledAt).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun alarmKey(medId: Long, action: String, scheduledAt: Long): String =
        "$medId|$action|$scheduledAt"

    private fun trackAlarm(medId: Long, action: String, scheduledAt: Long) {
        val key = alarmKey(medId, action, scheduledAt)
        val current = prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty()
        prefs.edit().putStringSet(KEY_SCHEDULED, current + key).apply()
    }

    private fun untrackAlarm(medId: Long, action: String, scheduledAt: Long) {
        val key = alarmKey(medId, action, scheduledAt)
        val current = prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty()
        prefs.edit().putStringSet(KEY_SCHEDULED, current - key).apply()
    }

    companion object {
        private const val TAG = "AlarmScheduler"
        private const val PREFS_NAME = "medsafe_alarms"
        private const val KEY_SCHEDULED = "scheduled_alarms"
    }
}
