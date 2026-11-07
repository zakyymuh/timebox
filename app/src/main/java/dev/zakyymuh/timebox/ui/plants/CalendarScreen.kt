package dev.zakyymuh.timebox.ui.plants

import androidx.compose.foundation.border
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.timebox.core.plants.Mark
import dev.zakyymuh.timebox.core.plants.MarkKind
import dev.zakyymuh.timebox.ui.common.GlyphButton
import dev.zakyymuh.timebox.ui.common.PaperCard
import dev.zakyymuh.timebox.ui.common.PaperTopBar
import dev.zakyymuh.timebox.ui.theme.MonoLabel
import dev.zakyymuh.timebox.ui.theme.MonoSmall
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val monthTitle = DateTimeFormatter.ofPattern("MMMM yyyy")
private val dayTitle = DateTimeFormatter.ofPattern("EEEE d MMMM")
private val weekdays = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
private const val MAX_DOTS = 3

@Composable
fun CalendarScreen(viewModel: CalendarViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .padding(horizontal = 22.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            PaperTopBar(title = "Calendar", onBack = onBack)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 14.dp)) {
                GlyphButton("‹", onClick = viewModel::previousMonth, description = "Previous month")
                Text(
                    text = state.month.atDay(1).format(monthTitle),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                GlyphButton("›", onClick = viewModel::nextMonth, description = "Next month")
            }

            Row(Modifier.fillMaxWidth()) {
                weekdays.forEach {
                    Text(it, style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
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
                Text("● watered", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Text("○ planned", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            val selected = state.selected
            if (selected != null) {
                Spacer(Modifier.height(18.dp))
                PaperCard {
                    Text(selected.format(dayTitle).uppercase(), style = MonoLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.selectedWaterings.isEmpty()) {
                        Text(
                            "Nothing watered this day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                    state.selectedWaterings.forEach { (plant, watering) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            PlantDot(plant.colour)
                            Spacer(Modifier.width(10.dp))
                            Text(plant.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
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
    val shape = RoundedCornerShape(8.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .height(54.dp)
            .padding(2.dp)
            .let { if (isToday || isSelected) it.border(1.dp, MaterialTheme.colorScheme.onSurface, shape) else it }
            .let { if (date != null) it.clickable(onClick = onClick) else it }
            .padding(top = 6.dp),
    ) {
        if (date == null) return@Column
        Text(
            text = date.dayOfMonth.toString(),
            style = MonoSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(5.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            marks.take(MAX_DOTS).forEach { mark ->
                PlantDot(mark.colour, size = 7.dp, hollow = mark.kind == MarkKind.PROJECTED)
            }
            if (marks.size > MAX_DOTS) {
                Text("+${marks.size - MAX_DOTS}", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
