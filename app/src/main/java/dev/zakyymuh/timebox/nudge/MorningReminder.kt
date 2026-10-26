package dev.zakyymuh.timebox.nudge

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dev.zakyymuh.timebox.core.NextFire
import java.time.Clock
import java.time.LocalTime
import java.time.ZonedDateTime

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

    private val alarms: AlarmManager? = context.getSystemService(AlarmManager::class.java)

    fun schedule(at: LocalTime = DEFAULT_TIME) {
        val manager = alarms ?: return
        val next = NextFire.compute(at, ZonedDateTime.now(clock))
        val triggerAt = next.toInstant().toEpochMilli()
        val intent = pendingIntent()

        // setExactAndAllowWhileIdle needs permission on API 31-32 and can be revoked;
        // an inexact alarm a few minutes late is perfectly acceptable for this, so fall
        // back rather than crash.
        val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            manager.canScheduleExactAlarms()

        if (canBeExact) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        }
    }

    fun cancel() {
        alarms?.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, MorningReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

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
