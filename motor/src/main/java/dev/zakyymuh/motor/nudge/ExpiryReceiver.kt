package dev.zakyymuh.motor.nudge

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dev.zakyymuh.motor.MainActivity
import dev.zakyymuh.motor.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Posts every expiry reminder due today, then schedules tomorrow's check.
 *
 * Runs from a cold-started process with no Activity alive, so it reaches the data
 * through [ServiceLocator] rather than anything UI-scoped.
 */
class ExpiryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        val reminder = ExpiryReminder(appContext)

        // Always re-arm first: if anything below throws, the chain must not end here.
        reminder.schedule()

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Without permission nothing is posted, and nothing is marked as sent,
                // so the reminder still goes out once notifications are allowed.
                if (!canPostNotifications(appContext)) return@launch
                reminder.ensureChannel()
                val repo = ServiceLocator.reminderRepository(appContext)
                val manager = appContext.getSystemService(NotificationManager::class.java) ?: return@launch
                repo.dueToday().forEach { due ->
                    val open = PendingIntent.getActivity(
                        appContext,
                        0,
                        MainActivity.open(appContext),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    val notification = NotificationCompat.Builder(appContext, ExpiryReminder.CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_popup_reminder)
                        .setContentTitle(due.title)
                        .setContentText(due.text)
                        .setContentIntent(open)
                        .setAutoCancel(true)
                        .build()
                    // One notification per date: the week warning replaces the month one.
                    manager.notify(NOTIFICATION_BASE + due.expiryId.toInt(), notification)
                    repo.markSent(due)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val NOTIFICATION_BASE = 3_000
    }
}
