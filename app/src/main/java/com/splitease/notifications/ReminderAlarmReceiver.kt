package com.splitease.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.splitease.MainActivity

/**
 * BroadcastReceiver that fires when an [AlarmManager] alarm for a reminder is due.
 * Builds and posts the notification to the system.
 *
 * Tapping the notification opens [MainActivity] which will navigate to the Reminders screen
 * via the deep-link intent extra [EXTRA_OPEN_REMINDERS].
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_NOTE = "reminder_note"
        const val EXTRA_OPEN_REMINDERS = "open_reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId < 0) return

        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val note = intent.getStringExtra(EXTRA_NOTE) ?: ""

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_REMINDERS, true)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val body = note.ifBlank { null }

        val notification = NotificationCompat.Builder(context, ReminderNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .apply { if (body != null) setContentText(body) }
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(reminderId.toInt(), notification)
    }
}
