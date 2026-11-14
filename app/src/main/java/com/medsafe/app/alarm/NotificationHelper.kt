package com.medsafe.app.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.medsafe.app.MainActivity
import com.medsafe.app.R
import com.medsafe.app.data.local.entity.MedicationEntity
import com.medsafe.app.ui.components.PillPainter
import com.medsafe.app.util.Format
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All notifications live on two urgency-based channels. The "Taken / Snooze / Skip"
 * actions go to [DoseActionReceiver], so they work with the app closed.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationManager: NotificationManager =
        context.getSystemService(NotificationManager::class.java)
    private val compatManager = NotificationManagerCompat.from(context)

    fun ensureChannels(soundEnabled: Boolean) {
        notificationManager.createNotificationChannel(doseChannel(soundEnabled))
        notificationManager.createNotificationChannel(lowStockChannel())
    }

    /** Recreates the dose channel because Android cannot change a channel's sound in place. */
    fun updateDoseChannelSound(soundEnabled: Boolean) {
        notificationManager.deleteNotificationChannel(CHANNEL_DOSE)
        notificationManager.createNotificationChannel(doseChannel(soundEnabled))
    }

    fun showDoseReminder(
        profileName: String,
        medication: MedicationEntity,
        scheduledAt: Long,
        missed: Boolean,
    ) {
        val body = buildString {
            append(Format.doseAmount(medication))
            medication.notes?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
            if (missed) append("\nMissed dose — tap Taken to log it late.")
        }
        val title = "$profileName: time for ${medication.name}"
        val notification = NotificationCompat.Builder(context, CHANNEL_DOSE)
            .setSmallIcon(R.drawable.ic_stat_pill)
            .setLargeIcon(pillBitmap(medication))
            .setContentTitle(title)
            .setContentText(body.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setContentIntent(openAppIntent())
            .addAction(0, "Taken", doseActionIntent(AlarmActions.TAKE, medication.id, scheduledAt))
            .addAction(0, "Snooze", doseActionIntent(AlarmActions.SNOOZE, medication.id, scheduledAt))
            .addAction(0, "Skip", doseActionIntent(AlarmActions.SKIP, medication.id, scheduledAt))
            .build()
        notify(NotificationIds.dose(medication.id, scheduledAt), notification)
    }

    fun cancelDose(medId: Long, scheduledAt: Long) {
        compatManager.cancel(NotificationIds.dose(medId, scheduledAt))
    }

    fun showLowStock(profileName: String, medication: MedicationEntity) {
        val stock = medication.stock ?: return
        val notification = NotificationCompat.Builder(context, CHANNEL_LOW_STOCK)
            .setSmallIcon(R.drawable.ic_stat_pill)
            .setLargeIcon(pillBitmap(medication))
            .setContentTitle("$profileName: low stock")
            .setContentText("${medication.name} has $stock left")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${medication.name} has $stock left. Consider refilling soon."),
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()
        notify(NotificationIds.lowStock(medication.id), notification)
    }

    private fun notify(id: Int, notification: android.app.Notification) {
        if (!canPostNotifications()) return
        try {
            compatManager.notify(id, notification)
        } catch (_: SecurityException) {
            // Permission can be revoked between the check and the call.
        }
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun doseChannel(soundEnabled: Boolean): NotificationChannel =
        NotificationChannel(
            CHANNEL_DOSE,
            "Dose reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminders to take a scheduled dose"
            enableVibration(true)
            if (soundEnabled) {
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            } else {
                setSound(null, null)
            }
        }

    private fun lowStockChannel(): NotificationChannel =
        NotificationChannel(
            CHANNEL_LOW_STOCK,
            "Low stock",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Alerts when a medication is running low"
        }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun doseActionIntent(action: String, medId: Long, scheduledAt: Long): PendingIntent {
        val intent = Intent(context, DoseActionReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmActions.EXTRA_MED_ID, medId)
            putExtra(AlarmActions.EXTRA_SCHEDULED_AT, scheduledAt)
        }
        return PendingIntent.getBroadcast(
            context,
            "$action|$medId|$scheduledAt".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pillBitmap(medication: MedicationEntity, sizePx: Int = 128): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFF8F1")
        }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, background)
        PillPainter.paint(
            canvas = canvas,
            shape = medication.pillShape,
            color1 = medication.pillColor1,
            color2 = medication.pillColor2,
            sizePx = sizePx.toFloat(),
        )
        return bitmap
    }

    companion object {
        const val CHANNEL_DOSE = "dose_reminder"
        const val CHANNEL_LOW_STOCK = "low_stock"
    }
}

object NotificationIds {
    private const val DOSE_BASE = 10_000
    private const val STOCK_BASE = 100_000_000

    fun dose(medId: Long, scheduledAt: Long): Int =
        DOSE_BASE + (("$medId:$scheduledAt").hashCode() and 0x00FFFFFF)

    fun lowStock(medId: Long): Int = STOCK_BASE + (medId % 1_000_000).toInt()
}
