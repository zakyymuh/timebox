package dev.zakyymuh.timebox.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import dev.zakyymuh.timebox.ui.theme.Fresh
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import dev.zakyymuh.timebox.nudge.EveningReminder
import dev.zakyymuh.timebox.nudge.MorningReminder
import dev.zakyymuh.timebox.ui.theme.DailyTheme

class MainActivity : ComponentActivity() {

    private val askNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* either way, carry on */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Always light: dark status-bar and navigation-bar icons on white, whatever the
        // phone's dark-mode setting.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Fresh.Page.toArgb(), Fresh.Page.toArgb()),
            navigationBarStyle = SystemBarStyle.light(Fresh.Page.toArgb(), Fresh.Page.toArgb()),
        )

        val reminder = MorningReminder(applicationContext)
        reminder.ensureChannel()
        // Re-arming an already-armed alarm is harmless, and this covers a first launch
        // and any firing that was missed while the app was not running.
        reminder.schedule()
        EveningReminder(applicationContext).apply {
            ensureChannel()
            schedule()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            DailyTheme {
                AppNav(start = Destinations.from(intent))
            }
        }
    }
}
