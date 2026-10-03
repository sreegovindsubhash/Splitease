package com.splitease.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.splitease.domain.model.Reminder

/**
 * Schedules and cancels one-time local notifications for Reminders using [AlarmManager].
 *
 * - Uses [AlarmManager.setExactAndAllowWhileIdle] so the alarm fires even in Doze mode.
 * - Each reminder maps to a distinct PendingIntent keyed by [Reminder.id].
 * - Completed reminders and past reminders are never scheduled.
 */
object ReminderNotificationScheduler {

    const val CHANNEL_ID = "splitease_reminders"
    private const val CHANNEL_NAME = "Expense Reminders"
    private const val CHANNEL_DESCRIPTION = "Notifies you when a scheduled expense reminder is due."

    /** Creates the notification channel (safe to call repeatedly; idempotent on API 26+). */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * Schedules an alarm for [reminder] if it is active (not completed) and its time is in
     * the future relative to [nowMs].  Silently skips completed or past reminders.
     */
    fun schedule(context: Context, reminder: Reminder, nowMs: Long = System.currentTimeMillis()) {
        if (reminder.isCompleted) return
        if (reminder.scheduledAt <= nowMs) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = buildPendingIntent(context, reminder)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Cannot schedule exact alarms; skip silently — the permission is optional.
                return
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminder.scheduledAt,
            intent,
        )
    }

    /** Cancels any pending alarm for [reminderId]. Safe to call even if no alarm is pending. */
    fun cancel(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = buildPendingIntentById(context, reminderId)
        alarmManager.cancel(intent)
        intent.cancel()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildPendingIntent(context: Context, reminder: Reminder): PendingIntent =
        buildPendingIntentById(context, reminder.id, reminder.title, reminder.note)

    internal fun buildPendingIntentById(
        context: Context,
        reminderId: Long,
        title: String = "",
        note: String = "",
    ): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderAlarmReceiver.EXTRA_TITLE, title)
            putExtra(ReminderAlarmReceiver.EXTRA_NOTE, note)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
