package dev.zakyymuh.timebox.ui.plants

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.timebox.core.plants.PlantCopy
import dev.zakyymuh.timebox.core.plants.PlantRow
import dev.zakyymuh.timebox.core.plants.PlantsOverview
import dev.zakyymuh.timebox.ui.common.RoundButton
import dev.zakyymuh.timebox.ui.common.SoftCard
import dev.zakyymuh.timebox.ui.common.TopBar
import dev.zakyymuh.timebox.ui.common.SectionTitle
import dev.zakyymuh.timebox.ui.theme.Label
import dev.zakyymuh.timebox.ui.theme.Small
import java.time.format.DateTimeFormatter

private val nextDate = DateTimeFormatter.ofPattern("EEEE d MMM")

/**
 * Shows what matters today, not a calendar: the big watering button when a plant is
 * due, otherwise when the next watering is. The calendar is behind the 📅 button.
 */
@Composable
fun PlantsScreen(
    viewModel: PlantsViewModel,
    onBack: () -> Unit,
    onCalendar: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.undo.collect { wateringId ->
            val result = snackbar.showSnackbar("Watered", actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) viewModel.undo(wateringId)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .padding(horizontal = 22.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            TopBar(title = "Plant schedule", onBack = onBack) {
                RoundButton("📅", onClick = onCalendar, description = "Calendar")
                Spacer(Modifier.width(8.dp))
                RoundButton("+", onClick = onAdd, description = "Add a plant")
            }

            val o = overview ?: return@Column
            when {
                o.isEmpty -> Empty()
                o.due.isNotEmpty() -> DueToday(o, viewModel::water, onEdit)
                else -> NothingDue(o, viewModel::water, onEdit)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Empty() {
    Text(
        text = "No plants yet.",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 24.dp),
    )
    Text(
        text = "Tap + to add one, with how often it needs water.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun DueToday(o: PlantsOverview, onWater: (Long) -> Unit, onEdit: (Long) -> Unit) {
    SectionTitle("Due today")
    o.due.forEach { row ->
        SoftCard(modifier = Modifier.padding(bottom = 12.dp)) {
            PlantTitle(row, onEdit)
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { onWater(row.plant.id) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = row.plant.colour.toColor(),
                    contentColor = Color.White,
                ),
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) {
                Text("💧  I watered it", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    if (o.upcoming.isNotEmpty()) {
        SectionTitle("Next")
        o.upcoming.forEach { PlantLine(it, onWater, onEdit) }
    }
}

@Composable
private fun NothingDue(o: PlantsOverview, onWater: (Long) -> Unit, onEdit: (Long) -> Unit) {
    val next = o.next ?: return
    Spacer(Modifier.height(8.dp))
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("NEXT WATERING", style = Label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            WaterEarly(next, onWater)
        }
        Spacer(Modifier.height(14.dp))
        PlantTitle(next, onEdit)
        Text(
            text = "${next.nextDue?.format(nextDate) ?: ""}  ·  ${PlantCopy.whenLabel(next.status)}",
            style = Small,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
    val later = o.upcoming.drop(1)
    if (later.isNotEmpty()) {
        SectionTitle("Later")
        later.forEach { PlantLine(it, onWater, onEdit) }
    }
}

@Composable
private fun PlantTitle(row: PlantRow, onEdit: (Long) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onEdit(row.plant.id) },
    ) {
        PlantDot(row.plant.colour, size = 12.dp)
        Spacer(Modifier.width(10.dp))
        Text(row.plant.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(10.dp))
        Text(PlantCopy.interval(row.plant.recurrence), style = Small, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** One plant that is not due: name, when, and a small button for watering early. */
@Composable
private fun PlantLine(row: PlantRow, onWater: (Long) -> Unit, onEdit: (Long) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).clickable { onEdit(row.plant.id) },
        ) {
            PlantDot(row.plant.colour)
            Spacer(Modifier.width(10.dp))
            Text(row.plant.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(PlantCopy.whenLabel(row.status), style = Small, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        WaterEarly(row, onWater)
    }
}

@Composable
private fun WaterEarly(row: PlantRow, onWater: (Long) -> Unit) {
    RoundButton("💧", onClick = { onWater(row.plant.id) }, description = "Watered ${row.plant.name} early")
}
