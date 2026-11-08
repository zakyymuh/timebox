package dev.zakyymuh.habits

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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.zakyymuh.habits.nudge.MorningReminder
import dev.zakyymuh.habits.ui.TodayScreen
import dev.zakyymuh.habits.ui.TodayViewModel
import dev.zakyymuh.timebox.ui.theme.DailyTheme
import dev.zakyymuh.timebox.ui.theme.Fresh

/** One screen: today's list. There is nothing behind it, so Back leaves the app. */
class MainActivity : ComponentActivity() {

    private val askNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* either way, carry on */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Fresh.Page.toArgb(), Fresh.Page.toArgb()),
            navigationBarStyle = SystemBarStyle.light(Fresh.Page.toArgb(), Fresh.Page.toArgb()),
        )

        // Re-arming an already-armed alarm is harmless, and this covers a first launch
        // and any firing that was missed while the app was not running.
        MorningReminder(applicationContext).apply {
            ensureChannel()
            schedule()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            DailyTheme {
                val context = LocalContext.current.applicationContext
                val vm: TodayViewModel = viewModel(
                    factory = viewModelFactory { initializer { TodayViewModel(ServiceLocator.repository(context)) } },
                )
                TodayScreen(viewModel = vm)
            }
        }
    }

    companion object {
        /** How the widget and the morning notification open the list. */
        fun open(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
