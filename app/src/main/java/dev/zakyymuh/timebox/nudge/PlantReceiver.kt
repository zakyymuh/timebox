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
import dev.zakyymuh.timebox.core.plants.PlantNotices
import dev.zakyymuh.timebox.core.soil.SampleStatus
import dev.zakyymuh.timebox.ui.Destinations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * At 18:00 posts one "Basil needs water" per plant that is due or overdue, then schedules
 * tomorrow's check. Overdue plants get one again every evening until watered.
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
                val notices = PlantNotices.evening(overview)
                // One notification per plant, each with its own buttons.
                clearStale(appContext, notices.map { PlantNotices.notificationId(it.plantId) }.toSet())
                notices.forEach { notify(appContext, reminder, it.title, it.plantId) }
                if (notices.isEmpty() && forced) notify(appContext, reminder, "No plants need water today", null)
                // Every 2 waterings since the last soil test: remind to re-check the sample.
                val soil = ServiceLocator.soilRepository(appContext).statusNow()
                if (soil is SampleStatus.Due) notifySoil(appContext, reminder, soil)
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
            plantId?.let(PlantNotices::openCode) ?: EveningReminder.REQUEST_CODE,
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
            builder.addAction(0, "Watered", action(context, PlantActions.ACTION_WATER, plantId, PlantNotices.waterCode(plantId)))
            builder.addAction(0, "Still moist", action(context, PlantActions.ACTION_CHECK, plantId, PlantNotices.checkCode(plantId)))
        }
        val id = plantId?.let(PlantNotices::notificationId) ?: EveningReminder.NOTIFICATION_ID
        context.getSystemService(NotificationManager::class.java)?.notify(id, builder.build())
    }

    /**
     * Takes down last evening's plant notifications that no longer apply (watered since,
     * snoozed, deleted) and the old single combined one, so only tonight's list shows.
     */
    private fun clearStale(context: Context, keep: Set<Int>) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.cancel(EveningReminder.NOTIFICATION_ID)
        manager.activeNotifications
            .filter { it.id >= PlantNotices.notificationId(0) && it.id !in keep }
            .forEach { manager.cancel(it.id) }
    }

    private fun notifySoil(context: Context, reminder: EveningReminder, status: SampleStatus.Due) {
        if (!canPostNotifications(context)) return
        reminder.ensureChannel()
        val open = PendingIntent.getActivity(
            context,
            REQUEST_SOIL,
            Destinations.intent(context, Destinations.SOIL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, EveningReminder.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("Time to check your soil sample")
            .setContentText("${status.wateringsSince} waterings since the last air-space test. Tap to test.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)?.notify(SOIL_NOTIFICATION_ID, notification)
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
        private const val REQUEST_SOIL = 203
        private const val SOIL_NOTIFICATION_ID = 3
    }
}
