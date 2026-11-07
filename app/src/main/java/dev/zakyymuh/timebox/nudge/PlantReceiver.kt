package dev.zakyymuh.timebox.nudge

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
import dev.zakyymuh.timebox.ServiceLocator
import dev.zakyymuh.timebox.core.plants.PlantCopy
import dev.zakyymuh.timebox.ui.Destinations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Posts "Basil needs water" at 18:00 when anything is due or overdue, then schedules
 * tomorrow's check. Overdue plants keep being included every evening until watered.
 * Runs from a cold-started process, so it reaches storage through [ServiceLocator].
 */
class PlantReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        val forced = intent?.getBooleanExtra(EXTRA_FORCE, false) == true
        val reminder = EveningReminder(appContext)

        // Always re-arm first: if anything below throws, the chain must not end here.
        reminder.schedule()

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val overview = ServiceLocator.plantRepository(appContext).overviewNow()
                val text = PlantCopy.notification(overview)
                    ?: if (forced) "No plants need water today" else null
                // One plant due: offer "Watered" / "Still moist" right on the notification.
                // Several: just open the app — bulk actions make watering the wrong one easy.
                val only = overview.due.singleOrNull()?.plant?.id
                if (text != null) notify(appContext, reminder, text, only)
            } finally {
                pending.finish()
            }
        }
    }

    private fun notify(context: Context, reminder: EveningReminder, text: String, plantId: Long?) {
        if (!canPostNotifications(context)) return
        reminder.ensureChannel()

        val open = PendingIntent.getActivity(
            context,
            EveningReminder.REQUEST_CODE,
            Destinations.intent(context, Destinations.PLANTS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, EveningReminder.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(text)
            .setContentText("Tap to open your plant schedule.")
            .setContentIntent(open)
            .setAutoCancel(true)
        if (plantId != null) {
            builder.addAction(0, "Watered", action(context, PlantActions.ACTION_WATER, plantId, REQUEST_WATER))
            builder.addAction(0, "Still moist", action(context, PlantActions.ACTION_CHECK, plantId, REQUEST_CHECK))
        }
        val notification = builder.build()

        context.getSystemService(NotificationManager::class.java)
            ?.notify(EveningReminder.NOTIFICATION_ID, notification)
    }

    private fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun action(context: Context, action: String, plantId: Long, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, PlantActionReceiver::class.java)
                .setAction(action)
                .putExtra(PlantActions.EXTRA_PLANT, plantId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val EXTRA_FORCE = "force"
        private const val REQUEST_WATER = 201
        private const val REQUEST_CHECK = 202
    }
}
