package dev.zakyymuh.timebox.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.zakyymuh.timebox.ServiceLocator
import dev.zakyymuh.timebox.nudge.MorningReminder
import dev.zakyymuh.timebox.ui.today.TodayScreen
import dev.zakyymuh.timebox.ui.today.TodayViewModel

class MainActivity : ComponentActivity() {

    private val askNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* either way, carry on */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reminder = MorningReminder(applicationContext)
        reminder.ensureChannel()
        // Re-arming an already-armed alarm is harmless, and this covers a first launch
        // and any firing that was missed while the app was not running.
        reminder.schedule()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val repo = ServiceLocator.repository(applicationContext)
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { TodayViewModel(repo) }
        }

        setContent {
            MaterialTheme {
                TodayScreen(viewModel = viewModel(factory = factory))
            }
        }
    }
}
