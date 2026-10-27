package dev.zakyymuh.timebox.nudge

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import dev.zakyymuh.timebox.ServiceLocator
import dev.zakyymuh.timebox.widget.TodayWidget
import dev.zakyymuh.timebox.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Posts the morning nudge, then schedules tomorrow's.
 *
 * Runs from a cold-started process with no Activity alive, so it reaches the repository
 * through [ServiceLocator] rather than anything UI-scoped.
 */
class MorningReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        val reminder = MorningReminder(appContext)

        // Always re-arm first: if anything below throws, the chain must not end here.
        reminder.schedule()

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val day = ServiceLocator.repository(appContext).todayNow()

                // The date has rolled over, so yesterday's list is no longer today's.
                // This daily firing is what returns the widget to its empty state;
                // a separate midnight alarm would buy nothing a user is awake for.
                TodayWidget().updateAll(appContext)

                // Nothing to nudge about if the list is already written.
                if (day.items.isEmpty()) {
                    notify(appContext, reminder)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun notify(context: Context, reminder: MorningReminder) {
        if (!canPostNotifications(context)) return
        reminder.ensureChannel()

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification: Notification = NotificationCompat.Builder(context, MorningReminder.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("What do you want to get done today?")
            .setContentText("Write today's list.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            ?.notify(MorningReminder.NOTIFICATION_ID, notification)
    }

    private fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}
