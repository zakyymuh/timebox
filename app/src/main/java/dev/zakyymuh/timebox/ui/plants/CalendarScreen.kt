package dev.zakyymuh.timebox.ui.plants

import androidx.compose.foundation.border
import dev.zakyymuh.timebox.ui.theme.Fresh
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.timebox.core.plants.Mark
import dev.zakyymuh.timebox.core.plants.MarkKind
import dev.zakyymuh.timebox.ui.common.RoundButton
import dev.zakyymuh.timebox.ui.common.SoftCard
import dev.zakyymuh.timebox.ui.common.TopBar
import dev.zakyymuh.timebox.ui.theme.Label
import dev.zakyymuh.timebox.ui.theme.Small
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val monthTitle = DateTimeFormatter.ofPattern("MMMM yyyy")
private val dayTitle = DateTimeFormatter.ofPattern("EEEE d MMMM")
private val weekdays = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
private const val MAX_DOTS = 3

@Composable
fun CalendarScreen(viewModel: CalendarViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(containerColor = Fresh.Page) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .padding(horizontal = 20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            TopBar(title = "Calendar", onBack = onBack)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 14.dp)) {
                RoundButton("‹", onClick = viewModel::previousMonth, description = "Previous month")
                Text(
                    text = state.month.atDay(1).format(monthTitle),
                    style = MaterialTheme.typography.titleLarge,
                    color = Fresh.Ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                RoundButton("›", onClick = viewModel::nextMonth, description = "Next month")
            }

            Row(Modifier.fillMaxWidth()) {
                weekdays.forEach {
                    Text(it, style = Small, color = Fresh.InkSoft, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(6.dp))

            // Monday-first grid: blank cells before the 1st, then the month in weeks.
            val first = state.month.atDay(1)
            val blanks = first.dayOfWeek.value - 1
            val cells: List<LocalDate?> = List(blanks) { null } + (1..state.month.lengthOfMonth()).map(state.month::atDay)
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        DayCell(
                            date = date,
                            isToday = date == state.today,
                            isSelected = date != null && date == state.selected,
                            marks = date?.let { state.marks[it] }.orEmpty(),
                            onClick = { viewModel.select(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("● watered", style = Small, color = Fresh.InkSoft)
                Spacer(Modifier.width(16.dp))
                Text("○ planned", style = Small, color = Fresh.InkSoft)
            }

            val selected = state.selected
            if (selected != null) {
                Spacer(Modifier.height(18.dp))
                SoftCard(corner = 20.dp) {
                    Text(selected.format(dayTitle), style = MaterialTheme.typography.titleMedium, color = Fresh.Ink)
                    if (state.selectedWaterings.isEmpty()) {
                        Text(
                            "Nothing watered this day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Fresh.InkSoft,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                    state.selectedWaterings.forEach { (plant, watering) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            PlantDot(plant.colour)
                            Spacer(Modifier.width(10.dp))
                            Text(plant.name, style = MaterialTheme.typography.bodyMedium, color = Fresh.Ink, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.removeWatering(watering.id) }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    isToday: Boolean,
    isSelected: Boolean,
    marks: List<Mark>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .height(58.dp)
            .let { if (date != null) it.clickable(onClick = onClick) else it }
            .padding(top = 4.dp),
    ) {
        if (date == null) return@Column
        // Today is a filled pastel green circle; a selected day gets an ink ring — the
        // one outline in the app, because it marks a selection.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isToday) Fresh.Pastel else Color.Transparent)
                .let { if (isSelected) it.border(1.5.dp, Fresh.Ink, CircleShape) else it },
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (isToday) Fresh.GreenDeep else Fresh.Ink,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            marks.take(MAX_DOTS).forEach { mark ->
                PlantDot(mark.colour, size = 6.dp, hollow = mark.kind == MarkKind.PROJECTED)
            }
            if (marks.size > MAX_DOTS) {
                Text("+${marks.size - MAX_DOTS}", style = Small, color = Fresh.InkSoft)
            }
        }
    }
}
