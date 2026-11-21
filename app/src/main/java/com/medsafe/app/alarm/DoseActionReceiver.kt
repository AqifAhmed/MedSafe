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

/** Handles Taken / Snooze / Skip from the notification shade without opening the app. */
@AndroidEntryPoint
class DoseActionReceiver : BroadcastReceiver() {
    @Inject lateinit var doseActionHandler: DoseActionHandler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val medId = intent.getLongExtra(ReminderActions.EXTRA_MED_ID, -1L)
        val scheduledAt = intent.getLongExtra(ReminderActions.EXTRA_SCHEDULED_AT, -1L)
        if (medId <= 0L || scheduledAt <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (action) {
                    ReminderActions.TAKE -> doseActionHandler.markTaken(medId, scheduledAt)
                    ReminderActions.SKIP -> doseActionHandler.markSkipped(medId, scheduledAt)
                    ReminderActions.SNOOZE -> doseActionHandler.snooze(medId, scheduledAt)
                }
            } catch (error: Throwable) {
                Log.e(TAG, "Dose action failed: $action", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "DoseActionReceiver"
    }
}
