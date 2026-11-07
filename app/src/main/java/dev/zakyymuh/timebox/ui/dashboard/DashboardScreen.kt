package dev.zakyymuh.timebox.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.timebox.ui.common.PaperCard
import dev.zakyymuh.timebox.ui.theme.MonoLabel
import java.time.format.DateTimeFormatter

private val heading = DateTimeFormatter.ofPattern("EEEE d MMMM")

/** The front door: two cards, each one a way in. */
@Composable
fun DashboardScreen(viewModel: DashboardViewModel, onOpenHabit: () -> Unit, onOpenPlants: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .padding(horizontal = 22.dp)
                .fillMaxSize(),
        ) {
            Spacer(Modifier.height(18.dp))
            Text("DAILY", style = MonoLabel, color = MaterialTheme.colorScheme.tertiary)
            Text(
                text = state.date?.format(heading) ?: "",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 6.dp, bottom = 22.dp),
            )

            DoorCard(title = "Daily habit", status = state.habit, onClick = onOpenHabit)
            Spacer(Modifier.height(14.dp))
            DoorCard(title = "Plant schedule", status = state.plants, onClick = onOpenPlants)
        }
    }
}

@Composable
private fun DoorCard(title: String, status: String, onClick: () -> Unit) {
    PaperCard(onClick = onClick) {
        Text(title.uppercase(), style = MonoLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = status,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text("→", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
