package dev.zakyymuh.timebox.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.zakyymuh.timebox.ServiceLocator
import dev.zakyymuh.timebox.ui.today.TodayScreen
import dev.zakyymuh.timebox.ui.today.TodayViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
