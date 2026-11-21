package com.medsafe.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

object ReminderActions {
    const val DIGEST = "com.medsafe.app.action.DIGEST"
    const val SNOOZE_FIRE = "com.medsafe.app.action.SNOOZE_FIRE"
    const val TAKE = "com.medsafe.app.action.TAKE"
    const val SKIP = "com.medsafe.app.action.SKIP"
    const val SNOOZE = "com.medsafe.app.action.SNOOZE"

    const val EXTRA_MED_ID = "com.medsafe.app.extra.MED_ID"
    const val EXTRA_SCHEDULED_AT = "com.medsafe.app.extra.SCHEDULED_AT"

    /** Digest alarms belong to the device, not to a medication. */
    const val DIGEST_MED_ID = 0L
}

/**
 * Schedules the two daily digest notifications with [AlarmManager] so they are posted at the
 * exact minute. Uses `setExactAndAllowWhileIdle` when the user has granted the exact-alarm
 * permission and falls back to `setAndAllowWhileIdle` (may be late) when they have not.
 *
 * Each alarm is identified by (action, medId, scheduledAt) through a unique intent data URI,
 * and its key is recorded in SharedPreferences so alarms can be cancelled in bulk.
 *
 * Alarm kinds:
 *  - [ReminderActions.DIGEST]: the next morning/evening summary. [scheduleAll] rebuilds it.
 *  - [ReminderActions.SNOOZE_FIRE]: a user-requested follow-up for one dose. Never touched by
 *    [scheduleAll]; [DoseReconciler] rebuilds it from the dose log.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val mutex = Mutex()

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /**
     * Last time the alarm system is known to have been working (an alarm fired or the schedule
     * was rebuilt). Doses that fell after this moment and have no log are the ones that were
     * missed while the phone was off or the app was force-stopped.
     */
    fun lastAliveAt(): Long {
        val stored = prefs.getLong(ALIVE, 0L)
        if (stored > 0L) return stored
        val now = System.currentTimeMillis()
        prefs.edit { putLong(ALIVE, now) }
        return now
    }

    private fun touchAlive() {
        prefs.edit { putLong(ALIVE, System.currentTimeMillis()) }
    }

    /**
     * Rebuilds the next digest alarm. Snooze alarms are deliberately left alone.
     * Called after any medication change, on launch, boot and clock changes.
     */
    suspend fun scheduleAll() {
        mutex.withLock {
            cancelTracked(onlyAction = ReminderActions.DIGEST)
            armNextDigest(Instant.now())
            touchAlive()
        }
    }

    /** Queues the digest that follows [after]. */
    suspend fun scheduleNextDigest(after: Instant = Instant.now()) {
        mutex.withLock { armNextDigest(after) }
    }

    private fun armNextDigest(after: Instant) {
        val next = ScheduleCalculator.nextDigest(after)
        arm(
            medId = ReminderActions.DIGEST_MED_ID,
            action = ReminderActions.DIGEST,
            scheduledAt = next.toEpochMilli(),
            triggerAtMillis = next.toEpochMilli(),
        )
    }

    suspend fun scheduleSnooze(medId: Long, scheduledAt: Long, minutes: Int) {
        scheduleSnoozeAt(medId, scheduledAt, System.currentTimeMillis() + minutes.coerceAtLeast(1) * 60_000L)
    }

    suspend fun scheduleSnoozeAt(medId: Long, scheduledAt: Long, triggerAtMillis: Long) {
        mutex.withLock {
            arm(medId, ReminderActions.SNOOZE_FIRE, scheduledAt, triggerAtMillis)
        }
    }

    suspend fun cancelSnooze(medId: Long, scheduledAt: Long) {
        mutex.withLock { cancel(ReminderActions.SNOOZE_FIRE, medId, scheduledAt) }
    }

    /** True when the system still holds a live alarm for this exact reminder. */
    fun isScheduled(action: String, medId: Long, scheduledAt: Long): Boolean =
        pendingIntent(action, medId, scheduledAt, PendingIntent.FLAG_NO_CREATE) != null

    /** Cancels every alarm this app scheduled, including snoozes. */
    suspend fun cancelAll() {
        mutex.withLock { cancelTracked(onlyAction = null) }
    }

    /** Called by the alarm receiver once an alarm has fired. */
    suspend fun markFired(action: String, medId: Long, scheduledAt: Long) {
        mutex.withLock {
            untrack(key(action, medId, scheduledAt))
            touchAlive()
        }
    }

    // ---- internals (callers hold the mutex) ----

    private fun arm(
        medId: Long,
        action: String,
        scheduledAt: Long,
        triggerAtMillis: Long,
    ) {
        val pending = pendingIntent(action, medId, scheduledAt, PendingIntent.FLAG_UPDATE_CURRENT)
            ?: return
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            }
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the call.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }
        track(key(action, medId, scheduledAt))
    }

    private fun cancel(action: String, medId: Long, scheduledAt: Long) {
        cancelPending(action, medId, scheduledAt)
        untrack(key(action, medId, scheduledAt))
    }

    private fun cancelPending(action: String, medId: Long, scheduledAt: Long) {
        pendingIntent(action, medId, scheduledAt, PendingIntent.FLAG_NO_CREATE)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    /** Cancels tracked alarms, either all of them or only those of [onlyAction]. */
    private fun cancelTracked(onlyAction: String?) {
        val keys = prefs.getStringSet(KEYS, emptySet()).orEmpty()
        val remaining = mutableSetOf<String>()
        keys.forEach { raw ->
            val parts = raw.split(SEP)
            if (onlyAction != null && parts.firstOrNull() != onlyAction) {
                remaining += raw
                return@forEach
            }
            if (parts.size == 3) {
                val medId = parts[1].toLongOrNull()
                val scheduledAt = parts[2].toLongOrNull()
                if (medId != null && scheduledAt != null) cancelPending(parts[0], medId, scheduledAt)
            }
        }
        prefs.edit {
            if (remaining.isEmpty()) remove(KEYS) else putStringSet(KEYS, remaining)
        }
    }

    private fun pendingIntent(
        action: String,
        medId: Long,
        scheduledAt: Long,
        flag: Int,
    ): PendingIntent? {
        val intent = Intent(context, DoseAlarmReceiver::class.java).apply {
            this.action = action
            // Unique data makes each (action, med, instant) its own PendingIntent identity.
            data = Uri.parse("medsafe://reminder/${action.substringAfterLast('.')}/$medId/$scheduledAt")
            putExtra(ReminderActions.EXTRA_MED_ID, medId)
            putExtra(ReminderActions.EXTRA_SCHEDULED_AT, scheduledAt)
        }
        return PendingIntent.getBroadcast(context, 0, intent, flag or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun key(action: String, medId: Long, scheduledAt: Long) =
        "$action$SEP$medId$SEP$scheduledAt"

    private fun track(key: String) {
        val updated = prefs.getStringSet(KEYS, emptySet()).orEmpty() + key
        prefs.edit { putStringSet(KEYS, updated) }
    }

    private fun untrack(key: String) {
        val current = prefs.getStringSet(KEYS, emptySet()).orEmpty()
        if (key in current) prefs.edit { putStringSet(KEYS, current - key) }
    }

    private companion object {
        const val PREFS = "medsafe_alarms"
        const val KEYS = "scheduled_keys"
        const val ALIVE = "last_alive_at"
        const val SEP = "|"
    }
}
