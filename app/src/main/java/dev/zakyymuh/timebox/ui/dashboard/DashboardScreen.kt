package dev.zakyymuh.timebox.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.timebox.core.TodayAgenda
import dev.zakyymuh.timebox.core.TodayEntry
import dev.zakyymuh.timebox.core.plants.FunFact
import dev.zakyymuh.timebox.core.plants.PlantCopy
import dev.zakyymuh.timebox.ui.common.Pill
import dev.zakyymuh.timebox.ui.common.RoundButton
import dev.zakyymuh.timebox.ui.common.SectionTitle
import dev.zakyymuh.timebox.ui.common.SoftCard
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import java.time.format.DateTimeFormatter

private val heading = DateTimeFormatter.ofPattern("EEEE d MMMM")
private const val GREETING = "Hello, Zaky 👋"

/** The front door: a greeting, two big cards, and everything that needs you today. */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenHabit: () -> Unit,
    onOpenPlants: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onOpenEncyclopedia: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.undo.collect { event ->
            val message = when (event) {
                is DashboardUndo.Watered -> PlantCopy.watered(event.name)
                is DashboardUndo.Ticked -> TodayAgenda.ticked(event.text)
            }
            val result = snackbar.showSnackbar(message, actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) viewModel.undo(event)
        }
    }

    Scaffold(containerColor = Fresh.Page, snackbarHost = { SnackbarHost(snackbar) }) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(20.dp))
            Text(GREETING, style = MaterialTheme.typography.headlineLarge, color = Fresh.Ink)
            Text(
                text = state.date?.format(heading) ?: "",
                style = Label,
                color = Fresh.InkSoft,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.height(180.dp)) {
                DoorCard(
                    title = "Daily habit",
                    status = state.habit,
                    dark = true,
                    onClick = onOpenHabit,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                DoorCard(
                    title = "Plant schedule",
                    status = state.plants,
                    dark = false,
                    onClick = onOpenPlants,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }

            state.fact?.let { FactCard(it, onOpenEntry, onOpenEncyclopedia) }

            SectionTitle("Today")
            val today = state.today
            when {
                today == null -> Unit
                today.isEmpty() -> SoftCard(corner = 20.dp) {
                    Text(TodayAgenda.ALL_DONE, style = MaterialTheme.typography.titleMedium, color = Fresh.Ink)
                }
                else -> today.forEach { entry ->
                    when (entry) {
                        is TodayEntry.Water -> WaterCard(entry, onWater = viewModel::water, onOpen = onOpenPlants)
                        is TodayEntry.Tick -> TickCard(entry, onTick = viewModel::tick, onOpen = onOpenHabit)
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** One of the two big doors: dark for the daily list, light for plants, like the reference. */
@Composable
private fun DoorCard(title: String, status: String, dark: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val text = if (dark) Color.White else Fresh.Ink
    val soft = if (dark) Color.White.copy(alpha = 0.7f) else Fresh.InkSoft
    SoftCard(
        color = if (dark) Fresh.Ink else Fresh.Page,
        onClick = onClick,
        modifier = modifier.shadow(
            elevation = if (dark) 0.dp else 10.dp,
            shape = RoundedCornerShape(28.dp),
            ambientColor = Fresh.Ink.copy(alpha = 0.08f),
            spotColor = Fresh.Ink.copy(alpha = 0.08f),
        ),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = status,
                style = Label,
                color = soft,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            RoundButton(
                glyph = "→",
                onClick = onClick,
                container = if (dark) Fresh.Pastel else Fresh.Card,
                content = if (dark) Fresh.GreenDeep else Fresh.Ink,
                size = 40.dp,
                description = "Open $title",
            )
        }
    }
}

@Composable
private fun WaterCard(entry: TodayEntry.Water, onWater: (Long) -> Unit, onOpen: () -> Unit) {
    val plant = entry.row.plant
    SoftCard(color = Fresh.Pastel, corner = 20.dp, onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Water ${plant.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Fresh.GreenDeep,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(PlantCopy.interval(plant.recurrence))
                    Pill(PlantCopy.whenLabel(entry.row.status))
                }
            }
            Spacer(Modifier.width(12.dp))
            RoundButton(
                glyph = "💧",
                onClick = { onWater(plant.id) },
                container = Fresh.Page,
                description = "Watered ${plant.name}",
            )
        }
    }
}

@Composable
private fun TickCard(entry: TodayEntry.Tick, onTick: (Long) -> Unit, onOpen: () -> Unit) {
    SoftCard(corner = 20.dp, onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                entry.item.text,
                style = MaterialTheme.typography.titleMedium,
                color = Fresh.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            RoundButton(
                glyph = "✓",
                onClick = { onTick(entry.item.id) },
                container = Fresh.Page,
                content = Fresh.Green,
                description = "Tick ${entry.item.text}",
            )
        }
    }
}

/** "Did you know?": one plant fact a day; tap for the entry, or open the whole encyclopedia. */
@Composable
private fun FactCard(fact: FunFact, onOpenEntry: (String) -> Unit, onOpenEncyclopedia: () -> Unit) {
    SoftCard(
        color = Fresh.Pastel,
        onClick = { onOpenEntry(fact.entry.id) },
        modifier = Modifier.padding(top = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🌵  Did you know?", style = MaterialTheme.typography.titleMedium, color = Fresh.GreenDeep, modifier = Modifier.weight(1f))
            Pill(
                text = "Encyclopedia",
                color = Fresh.Page,
                textColor = Fresh.GreenDeep,
                modifier = Modifier.clickable(onClick = onOpenEncyclopedia),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(fact.text, style = MaterialTheme.typography.bodyLarge, color = Fresh.GreenDeep)
        Spacer(Modifier.height(8.dp))
        Text("${fact.entry.name} →", style = Label, color = Fresh.GreenDeep.copy(alpha = 0.75f))
    }
}
