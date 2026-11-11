package dev.zakyymuh.timebox.ui.plants

import dev.zakyymuh.timebox.core.soil.SampleStatus
import dev.zakyymuh.timebox.ui.soil.StatusCard
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FabPosition
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.timebox.core.plants.PlantCopy
import dev.zakyymuh.timebox.core.plants.PlantRow
import dev.zakyymuh.timebox.core.plants.PlantsOverview
import dev.zakyymuh.timebox.ui.common.Pill
import dev.zakyymuh.timebox.ui.common.AddButton
import dev.zakyymuh.timebox.ui.common.AddButtonClearance
import dev.zakyymuh.timebox.ui.common.RoundButton
import dev.zakyymuh.timebox.ui.common.SectionTitle
import dev.zakyymuh.timebox.ui.common.SoftCard
import dev.zakyymuh.timebox.ui.common.TopBar
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import java.time.format.DateTimeFormatter

private val nextDate = DateTimeFormatter.ofPattern("EEEE d MMMM")

/**
 * Shows what matters today, not a calendar: a pastel green card with the big watering
 * button when a plant is due, otherwise when the next watering is. Calendar behind 📅.
 */
@Composable
fun PlantsScreen(
    viewModel: PlantsViewModel,
    onBack: () -> Unit,
    onCalendar: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onEncyclopedia: () -> Unit,
    soil: SampleStatus?,
    onSoil: () -> Unit,
) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.undo.collect { event ->
            val message = when (event) {
                is PlantUndo.Watered -> PlantCopy.watered(event.name)
                is PlantUndo.Checked -> PlantCopy.checked(event.name)
            }
            val result = snackbar.showSnackbar(message, actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) viewModel.undo(event)
        }
    }

    Scaffold(
        containerColor = Fresh.Page,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = { AddButton("Add a plant", onAdd) },
    ) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            TopBar(title = "Plants", onBack = onBack) {
                RoundButton("📖", onClick = onEncyclopedia, description = "Encyclopedia")
                Spacer(Modifier.width(8.dp))
                RoundButton("🗓", onClick = onCalendar, description = "Calendar")
            }

            val o = overview ?: return@Column
            val today by viewModel.todayDate.collectAsStateWithLifecycle()
            val act = Actions(
                water = viewModel::water,
                snooze = viewModel::snooze,
                edit = onEdit,
                useInterval = viewModel::useInterval,
                today = today,
            )
            when {
                o.isEmpty -> Empty()
                o.due.isNotEmpty() -> DueToday(o, act)
                else -> NothingDue(o, act)
            }
            soil?.let {
                Spacer(Modifier.height(20.dp))
                StatusCard(it, onClick = onSoil)
            }
            Spacer(Modifier.height(AddButtonClearance))
        }
    }
}

@Composable
private fun Empty() {
    SoftCard(modifier = Modifier.padding(top = 8.dp)) {
        Text("No plants yet", style = MaterialTheme.typography.titleLarge, color = Fresh.Ink)
        Text(
            "Tap Add a plant below, with how often it needs water.",
            style = MaterialTheme.typography.bodyMedium,
            color = Fresh.InkSoft,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Everything a plant card can do, passed around as one. */
private class Actions(
    val water: (Long) -> Unit,
    val snooze: (Long) -> Unit,
    val edit: (Long) -> Unit,
    val useInterval: (Long, Int) -> Unit,
    val today: java.time.LocalDate,
)

@Composable
private fun DueToday(o: PlantsOverview, act: Actions) {
    o.due.forEach { row ->
        SoftCard(color = Fresh.Pastel, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
            PlantName(row, act.edit, color = Fresh.GreenDeep)
            Text(
                PlantCopy.lastWatered(row.lastWatered, act.today),
                style = Label,
                color = Fresh.GreenDeep.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill(PlantCopy.interval(row.plant.recurrence))
                Pill(PlantCopy.whenLabel(row.status))
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).clickable { act.water(row.plant.id) },
                ) {
                    RoundButton(
                        glyph = "💧",
                        onClick = { act.water(row.plant.id) },
                        container = Fresh.Green,
                        content = Color.White,
                        size = 56.dp,
                        description = "I watered ${row.plant.name}",
                    )
                    Spacer(Modifier.width(14.dp))
                    Text("I watered it", style = MaterialTheme.typography.titleMedium, color = Fresh.GreenDeep)
                }
                // Checked the soil and it is still moist: not today, ask again tomorrow.
                Pill(
                    text = "Still moist",
                    color = Fresh.Page,
                    textColor = Fresh.GreenDeep,
                    modifier = Modifier.clickable { act.snooze(row.plant.id) },
                )
            }
            IntervalTip(row, act)
        }
    }
    if (o.upcoming.isNotEmpty()) {
        SectionTitle("Next")
        o.upcoming.forEach { PlantLine(it, act) }
    }
}

@Composable
private fun NothingDue(o: PlantsOverview, act: Actions) {
    val next = o.next ?: return
    SoftCard(modifier = Modifier.padding(top = 8.dp)) {
        Text("Next watering", style = Label, color = Fresh.InkSoft)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                PlantName(next, act.edit)
                Text(
                    next.nextDue?.format(nextDate) ?: "",
                    style = Label,
                    color = Fresh.InkSoft,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    PlantCopy.lastWatered(next.lastWatered, act.today),
                    style = Label,
                    color = Fresh.InkSoft,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
                )
                Pill(PlantCopy.whenLabel(next.status), color = Fresh.Page)
            }
            WaterEarly(next, act.water)
        }
        IntervalTip(next, act)
    }
    val later = o.upcoming.drop(1)
    if (later.isNotEmpty()) {
        SectionTitle("Later")
        later.forEach { PlantLine(it, act) }
    }
}

/** "You usually water every 8 days — Use 8": learned from history, applied only on tap. */
@Composable
private fun IntervalTip(row: PlantRow, act: Actions) {
    val days = row.suggestedInterval ?: return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 14.dp).fillMaxWidth(),
    ) {
        Text(
            PlantCopy.usually(days),
            style = Label,
            color = Fresh.GreenDeep,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Pill(
            text = PlantCopy.use(days),
            color = Fresh.Green,
            textColor = Color.White,
            modifier = Modifier.clickable { act.useInterval(row.plant.id, days) },
        )
    }
}

@Composable
private fun PlantName(row: PlantRow, onEdit: (Long) -> Unit, color: Color = Fresh.Ink) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onEdit(row.plant.id) },
    ) {
        PlantDot(row.plant.colour, size = 12.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            row.plant.name,
            style = MaterialTheme.typography.titleLarge,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** One plant that is not due: name, when, last watered, and a small button for watering early. */
@Composable
private fun PlantLine(row: PlantRow, act: Actions) {
    SoftCard(corner = 20.dp, modifier = Modifier.padding(bottom = 10.dp), onClick = { act.edit(row.plant.id) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlantDot(row.plant.colour)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    row.plant.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Fresh.Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${PlantCopy.interval(row.plant.recurrence)} · ${PlantCopy.whenLabel(row.status)}",
                    style = Label,
                    color = Fresh.InkSoft,
                )
                Text(PlantCopy.lastWatered(row.lastWatered, act.today), style = Label, color = Fresh.InkSoft)
            }
            Spacer(Modifier.width(12.dp))
            WaterEarly(row, act.water)
        }
        IntervalTip(row, act)
    }
}

@Composable
private fun WaterEarly(row: PlantRow, onWater: (Long) -> Unit) {
    RoundButton("💧", onClick = { onWater(row.plant.id) }, container = Fresh.Page, description = "Watered ${row.plant.name} early")
}
