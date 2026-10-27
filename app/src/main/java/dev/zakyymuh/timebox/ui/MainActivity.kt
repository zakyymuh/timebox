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
import android.content.Intent
import dev.zakyymuh.timebox.nudge.MorningReceiver
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
            initializer {
                TodayViewModel(
                    repo = repo,
                    // Same path the 06:30 alarm takes, so the test exercises the real
                    // receiver rather than a shortcut that could pass while it is broken.
                    sendTestReminder = {
                        sendBroadcast(
                            Intent(applicationContext, MorningReceiver::class.java)
                                .putExtra(MorningReceiver.EXTRA_FORCE, true),
                        )
                    },
                )
            }
        }

        setContent {
            MaterialTheme {
                TodayScreen(viewModel = viewModel(factory = factory))
            }
        }
    }
}
