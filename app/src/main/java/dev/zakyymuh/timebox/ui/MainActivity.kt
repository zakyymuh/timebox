package dev.zakyymuh.timebox.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Task 1 skeleton: proves AGP 9, compileSdk 37 and Compose build and run together.
 * Task 4 replaces this with the nav host and the Today screen.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Skeleton()
            }
        }
    }
}

@Composable
private fun Skeleton() {
    Scaffold(modifier = Modifier.fillMaxSize()) { insets ->
        Column(modifier = Modifier.padding(insets).padding(24.dp)) {
            Text(text = "Timebox", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Give the day a shape before it starts.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
