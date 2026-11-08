package dev.zakyymuh.timebox.nudge

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dev.zakyymuh.timebox.core.NextFire
import java.time.Clock
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * One alarm a day at a wall-clock time, delivered to [receiver]. Each firing schedules
 * the next, since Android has no reliable repeating exact alarm.
 *
 * AlarmManager rather than WorkManager: WorkManager's minimum interval is fifteen minutes
 * with no guarantee of when inside it — no use for "at 06:30" or "at 18:00".
 */
class DailyAlarm(
    private val context: Context,
    private val requestCode: Int,
    private val receiver: Class<out BroadcastReceiver>,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    private val alarms: AlarmManager? = context.getSystemService(AlarmManager::class.java)

    fun schedule(at: LocalTime) {
        val manager = alarms ?: return
        val next = NextFire.compute(at, ZonedDateTime.now(clock))
        val triggerAt = next.toInstant().toEpochMilli()
        val intent = pendingIntent()

        // Exact alarms need a permission the app does not ask for; without it Android
        // may deliver up to an hour late. Fall back rather than crash.
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
            requestCode,
            Intent(context, receiver),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
