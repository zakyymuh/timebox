package dev.zakyymuh.timebox.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.zakyymuh.timebox.core.plants.DayBorder
import dev.zakyymuh.timebox.core.plants.DayMark
import dev.zakyymuh.timebox.core.plants.PlantCopy
import dev.zakyymuh.timebox.core.plants.StripDay
import dev.zakyymuh.timebox.ui.common.RoundButton
import dev.zakyymuh.timebox.ui.common.SoftCard
import dev.zakyymuh.timebox.ui.plants.PlantDot
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import dev.zakyymuh.timebox.ui.theme.Small
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Today's pill and the collection tile, as in the owner's references. */
internal val Lilac = Color(0xFFB98AEB)
private val dayName = DateTimeFormatter.ofPattern("EEE")
private val dayHeading = DateTimeFormatter.ofPattern("EEEE d MMMM")
private val RING = 5.dp
private val GAP = 4.dp

/**
 * The plant schedule as a week: seven day pills with today in the middle, all inside
 * the card. Tapping a pill lists that day's plants underneath;
 * tapping anywhere else opens the schedule.
 */
@Composable
fun WeekCard(week: List<StripDay>, today: LocalDate, onOpen: () -> Unit) {
    var selected by remember(today) { mutableStateOf(today) }
    SoftCard(
        color = Fresh.Page,
        onClick = onOpen,
        modifier = Modifier.shadow(
            elevation = 10.dp,
            shape = RoundedCornerShape(28.dp),
            ambientColor = Fresh.Ink.copy(alpha = 0.08f),
            spotColor = Fresh.Ink.copy(alpha = 0.08f),
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Plant schedule", style = MaterialTheme.typography.titleLarge, color = Fresh.Ink, modifier = Modifier.weight(1f))
            RoundButton(glyph = "→", onClick = onOpen, container = Fresh.Card, size = 40.dp, description = "Open Plant schedule")
        }
        Spacer(Modifier.height(16.dp))
        DayPills(week, today, selected) { selected = it }
        Spacer(Modifier.height(16.dp))
        week.firstOrNull { it.date == selected }?.let { DayList(it, today) }
    }
}

/**
 * All seven pills fit inside the card, a small gap apart.
 */
@Composable
private fun DayPills(week: List<StripDay>, today: LocalDate, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val pill = (maxWidth - GAP * (week.size - 1)) / week.size.coerceAtLeast(1)
        Row(horizontalArrangement = Arrangement.spacedBy(GAP), modifier = Modifier.fillMaxWidth()) {
            week.forEach { day ->
                DayPill(day, isToday = day.date == today, isSelected = day.date == selected, width = pill) {
                    onSelect(day.date)
                }
            }
        }
    }
}

/**
 * Day name on top, the date in a white circle at the bottom. The circle sits [RING] in
 * from the sides and the bottom, so it shares a centre with the pill's round end and the
 * ring around it is even. The border tells the watering: solid green once watered, light
 * green while a plant is still to do. A selected day's border is thicker.
 */
@Composable
private fun DayPill(day: StripDay, isToday: Boolean, isSelected: Boolean, width: Dp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    val border = when (day.border) {
        DayBorder.Watered -> Fresh.Green
        DayBorder.ToDo -> Fresh.Green.copy(alpha = 0.45f)
        DayBorder.None -> if (isSelected && !isToday) Fresh.Ink else Color.White
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(width)
            .clip(shape)
            .background(if (isToday) Lilac else Fresh.Card)
            .border(if (isSelected) 3.5.dp else 2.dp, border, shape)
            .clickable(role = Role.Tab, onClickLabel = day.date.format(dayHeading), onClick = onClick)
            .padding(top = 14.dp, bottom = RING),
    ) {
        Text(day.date.format(dayName), style = Label, color = if (isToday) Color.White else Fresh.InkSoft, maxLines = 1)
        Spacer(Modifier.height(10.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(width - RING * 2).clip(CircleShape).background(Color.White),
        ) {
            Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, color = Fresh.Ink)
        }
    }
}

/** The selected day's plants, by name. */
@Composable
private fun DayList(day: StripDay, today: LocalDate) {
    Text(
        if (day.date == today) "Today" else day.date.format(dayHeading),
        style = Label,
        color = Fresh.InkSoft,
    )
    Spacer(Modifier.height(8.dp))
    if (day.plants.isEmpty()) {
        Text(PlantCopy.emptyDay(day.date, today), style = MaterialTheme.typography.bodyLarge, color = Fresh.Ink)
        return
    }
    day.plants.forEach { entry ->
        val late = (entry.mark as? DayMark.Due)?.let { it.daysLate > 0 } == true
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            PlantDot(entry.plant.colour, size = 10.dp, hollow = entry.mark != DayMark.Watered)
            Spacer(Modifier.width(10.dp))
            Text(
                entry.plant.name,
                style = MaterialTheme.typography.bodyLarge,
                color = Fresh.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(PlantCopy.mark(entry.mark), style = Small, color = if (late) Fresh.Error else Fresh.InkSoft)
        }
    }
}
