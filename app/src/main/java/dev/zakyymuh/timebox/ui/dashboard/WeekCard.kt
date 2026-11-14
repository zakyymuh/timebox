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
import androidx.compose.ui.text.font.FontWeight
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

/** A day with a watering logged: a quiet leaf green that sits beside the rose. */
private val Sage: Color get() = if (Fresh.Dark) Color(0xFF2A3325) else Color(0xFFE1E9DA)
private val SageInk: Color get() = if (Fresh.Dark) Color(0xFFC4D6B5) else Color(0xFF4F6342)
private val dayName = DateTimeFormatter.ofPattern("EEE")
private val dayHeading = DateTimeFormatter.ofPattern("EEEE d MMMM")
private val RING = 5.dp
private val GAP = 4.dp

/** How much wider than the others today's pill is when a plant needs water today. */
private const val BIG = 1.3f

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
        corner = CARD_CORNER,
        onClick = onOpen,
        modifier = Modifier.shadow(
            elevation = 10.dp,
            shape = RoundedCornerShape(CARD_CORNER),
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
 * All seven pills fit inside the card, a small gap apart. When a plant needs water today,
 * today's pill is a little bigger and the others share what is left.
 */
@Composable
private fun DayPills(week: List<StripDay>, today: LocalDate, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val bigToday = week.any { it.date == today && it.isDueToday }
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val shares = week.size + if (bigToday) BIG - 1f else 0f
        val pill = (maxWidth - GAP * (week.size - 1)) / shares.coerceAtLeast(1f)
        Row(
            horizontalArrangement = Arrangement.spacedBy(GAP),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            week.forEach { day ->
                val big = bigToday && day.date == today
                DayPill(day, isToday = day.date == today, isSelected = day.date == selected, big = big, width = if (big) pill * BIG else pill) {
                    onSelect(day.date)
                }
            }
        }
    }
}

/** Today, with a plant still to water. */
private val StripDay.isDueToday: Boolean get() = border == DayBorder.ToDo

/**
 * Day name on top, the date in a circle at the bottom; the pill itself has no fill, so only
 * the circles carry colour. A watered day's circle is sage, a day with a plant still to do
 * has a rose ring, any other day is cream. Today sits in a blush pill; when a plant needs
 * water today it is [big] and its circle is solid rose. A selected day gets an outline.
 */
@Composable
private fun DayPill(day: StripDay, isToday: Boolean, isSelected: Boolean, big: Boolean, width: Dp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    val watered = day.border == DayBorder.Watered
    val todo = day.border == DayBorder.ToDo
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(width)
            .clip(shape)
            .background(if (isToday) Fresh.Pastel else Color.Transparent)
            .border(2.dp, if (isSelected && !isToday) Fresh.Ink else Color.Transparent, shape)
            .clickable(role = Role.Tab, onClickLabel = day.date.format(dayHeading), onClick = onClick)
            .padding(top = 14.dp, bottom = RING),
    ) {
        Text(
            day.date.format(dayName),
            style = Label,
            fontWeight = if (isToday) FontWeight.SemiBold else null,
            color = if (isToday) Fresh.GreenDeep else Fresh.InkSoft,
            maxLines = 1,
        )
        Spacer(Modifier.height(10.dp))
        val circle = Modifier.size(width - RING * 2).clip(CircleShape)
        Box(
            contentAlignment = Alignment.Center,
            modifier = when {
                big -> circle.background(Fresh.Green)
                watered -> circle.background(Sage)
                todo -> circle.background(Fresh.Page).border(2.dp, Fresh.Green, CircleShape)
                isToday -> circle.background(Fresh.Page)
                else -> circle.background(Fresh.Card)
            },
        ) {
            Text(
                day.date.dayOfMonth.toString(),
                style = if (big) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                color = when {
                    big -> Color.White
                    watered -> SageInk
                    todo -> Fresh.GreenDeep
                    else -> Fresh.Ink
                },
            )
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
