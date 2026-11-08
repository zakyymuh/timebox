package dev.zakyymuh.habits.nudge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dev.zakyymuh.timebox.nudge.DailyAlarm
import java.time.Clock
import java.time.LocalTime

/**
 * The one notification this app sends: a nudge each morning to write today's list.
 *
 * Scheduled with AlarmManager rather than WorkManager because WorkManager's minimum
 * interval is fifteen minutes with no guarantee of when inside it — fine for sync, no
 * use for "at 06:30". Each firing schedules the next, since Android has no reliable
 * repeating exact alarm.
 */
class MorningReminder(
    private val context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    private val alarm = DailyAlarm(context, REQUEST_CODE, MorningReceiver::class.java, clock)

    fun schedule(at: LocalTime = DEFAULT_TIME) = alarm.schedule(at)

    fun cancel() = alarm.cancel()

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Morning reminder",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "A nudge each morning to write today's list."
            },
        )
    }

    companion object {
        const val CHANNEL_ID = "timebox_morning"
        const val NOTIFICATION_ID = 1
        private const val REQUEST_CODE = 100
        val DEFAULT_TIME: LocalTime = LocalTime.of(6, 30)
    }
}
