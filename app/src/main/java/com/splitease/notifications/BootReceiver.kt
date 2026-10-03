package com.splitease.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.splitease.SplitEaseApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Listens for [Intent.ACTION_BOOT_COMPLETED] and [ACTION_QUICKBOOT_POWERON] (HTC devices).
 *
 * After a device reboot all pending [android.app.AlarmManager] alarms are lost.
 * This receiver re-schedules all active future reminders so they still fire as expected.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        val app = context.applicationContext as SplitEaseApplication
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val reminders = app.reminderRepository.getActiveFutureReminders(now)
                reminders.forEach { reminder ->
                    ReminderNotificationScheduler.schedule(context, reminder, now)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
