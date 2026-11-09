package dev.zakyymuh.motor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.toArgb
import dev.zakyymuh.motor.nudge.ExpiryReminder
import dev.zakyymuh.timebox.ui.theme.DailyTheme
import dev.zakyymuh.timebox.ui.theme.Fresh

class MainActivity : ComponentActivity() {

    private val askNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* either way, carry on */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Always light, like papacti: dark bar icons on the white page.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Fresh.Page.toArgb(), Fresh.Page.toArgb()),
            navigationBarStyle = SystemBarStyle.light(Fresh.Page.toArgb(), Fresh.Page.toArgb()),
        )

        // Re-arming an already-armed alarm is harmless, and this covers a first launch
        // and any check that was missed while the app was not running.
        ExpiryReminder(applicationContext).apply {
            ensureChannel()
            schedule()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            DailyTheme {
                MotorNav(onClose = ::finish)
            }
        }
    }

    companion object {
        /** How an expiry reminder opens the app. */
        fun open(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
