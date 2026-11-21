package com.medsafe.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Fires when an [android.app.AlarmManager] alarm goes off and hands off to [DoseReminderProcessor]. */
@AndroidEntryPoint
class DoseAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var processor: DoseReminderProcessor

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val scheduledAt = intent.getLongExtra(ReminderActions.EXTRA_SCHEDULED_AT, -1L)
        if (scheduledAt <= 0L) return
        val medId = intent.getLongExtra(ReminderActions.EXTRA_MED_ID, ReminderActions.DIGEST_MED_ID)
        if (action != ReminderActions.DIGEST && medId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                processor.process(action, medId, scheduledAt)
            } catch (error: Throwable) {
                Log.e(TAG, "Alarm handling failed: $action", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "DoseAlarmReceiver"
    }
}
