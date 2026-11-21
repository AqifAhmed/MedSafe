package com.medsafe.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.medsafe.app.data.repository.ProfileRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Rebuilds every alarm after a reboot, timezone change, manual clock change, app update or
 * a change to the exact-alarm permission (AlarmManager alarms do not survive any of these).
 */
@AndroidEntryPoint
class RescheduleReceiver : BroadcastReceiver() {
    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var profileRepository: ProfileRepository
    @Inject lateinit var doseReconciler: DoseReconciler

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val since = reminderScheduler.lastAliveAt()
                // Every alarm may be gone or stale after these events: clear and rebuild all of
                // them, then repair snoozes and doses that came due in the meantime.
                reminderScheduler.cancelAll()
                profileRepository.ensureMainProfile()
                reminderScheduler.scheduleAll()
                doseReconciler.reconcile(since = since)
            } catch (error: Throwable) {
                Log.e(TAG, "Reschedule failed for ${intent.action}", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "RescheduleReceiver"
    }
}
