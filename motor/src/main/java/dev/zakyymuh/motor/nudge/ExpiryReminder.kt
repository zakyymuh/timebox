package dev.zakyymuh.motor.nudge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dev.zakyymuh.timebox.nudge.DailyAlarm
import java.time.Clock
import java.time.LocalTime

/**
 * One check a day, at [DEFAULT_TIME], for dates that run out in 30 or 7 days. A daily
 * check that compares today against every date is simpler than one alarm per date, and
 * it cannot drift out of step when a date is edited, renewed or deleted.
 */
class ExpiryReminder(
    private val context: Context,
    clock: Clock = Clock.systemDefaultZone(),
) {

    private val alarm = DailyAlarm(context, REQUEST_CODE, ExpiryReceiver::class.java, clock)

    fun schedule(at: LocalTime = DEFAULT_TIME) = alarm.schedule(at)

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Expiry reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "A month and a week before your SIM, STNK or other papers run out."
            },
        )
    }

    companion object {
        const val CHANNEL_ID = "motor_expiry"
        private const val REQUEST_CODE = 300
        val DEFAULT_TIME: LocalTime = LocalTime.of(9, 0)
    }
}
