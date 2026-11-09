package dev.zakyymuh.timebox.nudge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import java.time.LocalTime

/** The 18:00 check: one notification for each plant that is due or overdue. */
class EveningReminder(private val context: Context) {

    private val alarm = DailyAlarm(context, REQUEST_CODE, PlantReceiver::class.java)

    fun schedule() = alarm.schedule(DEFAULT_TIME)

    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Plants", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "When a plant needs water."
            },
        )
    }

    companion object {
        const val CHANNEL_ID = "timebox_plants"
        /** The "nothing due" test notification, and the old combined one. */
        const val NOTIFICATION_ID = 2
        const val REQUEST_CODE = 200
        val DEFAULT_TIME: LocalTime = LocalTime.of(18, 0)
    }
}
