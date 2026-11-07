package dev.zakyymuh.timebox.nudge

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.zakyymuh.timebox.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles "Watered" / "Still moist" tapped on the 6pm notification, without opening the app. */
class PlantActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        val plantId = intent?.getLongExtra(PlantActions.EXTRA_PLANT, 0L) ?: 0L
        val action = intent?.action
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PlantActions.handle(ServiceLocator.plantRepository(appContext), action, plantId)
                appContext.getSystemService(NotificationManager::class.java)
                    ?.cancel(EveningReminder.NOTIFICATION_ID)
            } finally {
                pending.finish()
            }
        }
    }
}
