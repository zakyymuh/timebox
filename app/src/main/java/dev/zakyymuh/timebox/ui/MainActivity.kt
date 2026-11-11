package dev.zakyymuh.timebox.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import dev.zakyymuh.timebox.ui.theme.Appearance
import dev.zakyymuh.timebox.ui.theme.Fresh
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import dev.zakyymuh.timebox.nudge.EveningReminder
import dev.zakyymuh.timebox.ui.theme.DailyTheme

class MainActivity : ComponentActivity() {

    private val askNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* either way, carry on */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light or dark, as picked in Settings; the system bars match the page.
        Appearance.start(this, Fresh.Garden, Fresh.GardenDark)

        // Re-arming an already-armed alarm is harmless, and this covers a first launch
        // and any firing that was missed while the app was not running.
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
