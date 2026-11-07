package dev.zakyymuh.timebox.nudge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Alarms do not survive a reboot, so both daily reminders are re-armed on boot. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        MorningReminder(context.applicationContext).schedule()
        EveningReminder(context.applicationContext).schedule()
    }
}
