package dev.zakyymuh.motor.nudge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Alarms do not survive a reboot, so the daily expiry check is re-armed on boot. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        ExpiryReminder(context.applicationContext).schedule()
    }
}
