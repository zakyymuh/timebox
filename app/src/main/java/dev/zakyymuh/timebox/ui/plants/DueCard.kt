package dev.zakyymuh.timebox.ui.plants

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.zakyymuh.timebox.core.plants.PlantCopy
import dev.zakyymuh.timebox.core.plants.PlantRow
import dev.zakyymuh.timebox.core.plants.PlantStatus
import dev.zakyymuh.timebox.core.plants.Recurrence
import dev.zakyymuh.timebox.data.PlantRepository
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import dev.zakyymuh.timebox.ui.theme.Small
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** The watered card: a darker green, a little lighter at the top. */
private val SoakTop = Color(0xFF4FA36F)
private val SoakBottom = Color(0xFF2E7A4D)

private const val SOAK_MS = 1_000
/** How long the watered card stays put before it moves down to Next. */
private const val SETTLE_MS = 1_200L

private val BUTTON = 96.dp
/** Room under the card for the lower half of the button and its label. */
private val UNDER = 76.dp

private val shortDate = DateTimeFormatter.ofPattern("EEE d MMM")

/**
 * A plant that needs water today. The big 💧 sits half off the card's bottom edge. Tapping
 * it floods the card darker green from top to bottom, turns the drop into a ✓, and after a
 * moment [onSettled] lets the card go. "Still moist" asks when instead.
 */
@Composable
internal fun DueCard(
    row: PlantRow,
    /** True once tapped: the watering is already saved, this card just plays it out. */
    watered: Boolean,
    today: LocalDate,
    onWater: () -> Unit,
    onMoist: () -> Unit,
    onEdit: (Long) -> Unit,
    onUseInterval: (Long, Int) -> Unit,
    onSettled: () -> Unit,
) {
    val soak = remember { Animatable(if (watered) 1f else 0f) }
    val settled by rememberUpdatedState(onSettled)
    LaunchedEffect(watered) {
        if (!watered) return@LaunchedEffect
        soak.animateTo(1f, tween(SOAK_MS, easing = FastOutSlowInEasing))
        delay(SETTLE_MS)
        settled()
    }
    // The words turn white once the green has passed them.
    val t = ((soak.value - 0.35f) / 0.5f).coerceIn(0f, 1f)
    val ink = lerp(Fresh.GreenDeep, Color.White, t)
    val pill = lerp(Fresh.Page, Color.White.copy(alpha = 0.2f), t)
    val pillText = lerp(Fresh.Ink, Color.White, t)

    val next = when (val r = row.plant.recurrence) {
        is Recurrence.EveryDays -> today.plusDays(r.days.toLong())
        is Recurrence.Once -> null
    }

    Box(Modifier.padding(top = 8.dp, bottom = 4.dp).fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(bottom = UNDER)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Fresh.Pastel),
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawBehind { soakDown(soak.value) },
            )
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                PlantName(row, onEdit, color = ink)
                Text(
                    if (watered) {
                        next?.let { "Watered just now · next on ${it.format(shortDate)}" } ?: "Watered just now"
                    } else {
                        PlantCopy.lastWatered(row.lastWatered, today)
                    },
                    style = Label,
                    color = ink.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        PlantCopy.interval(row.plant.recurrence),
                        style = Small,
                        color = pillText,
                        modifier = Modifier.clip(CircleShape).background(pill).padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                    val whenText = when {
                        !watered -> PlantCopy.whenLabel(row.status)
                        next != null -> PlantCopy.whenLabel(
                            PlantStatus.Upcoming(ChronoUnit.DAYS.between(today, next).toInt()),
                        )
                        else -> "done"
                    }
                    Text(
                        whenText,
                        style = Small,
                        color = pillText,
                        modifier = Modifier.clip(CircleShape).background(pill).padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                if (!watered) IntervalTip(row, onUseInterval)
                // The bottom strip: the button's upper half lands in the middle of it.
                Box(Modifier.fillMaxWidth().padding(top = 16.dp).height(BUTTON / 2)) {
                    if (!watered) {
                        Text(
                            "Still moist",
                            style = Small,
                            color = Fresh.GreenDeep,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(Fresh.Page)
                                .clickable(onClick = onMoist)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }

        // Centred on the card's bottom edge: half on it, half hanging below.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -(UNDER - BUTTON / 2))
                .size(BUTTON)
                .shadow(12.dp, CircleShape, ambientColor = Fresh.Green, spotColor = Fresh.Green)
                .clip(CircleShape)
                .background(Fresh.Page)
                .padding(6.dp)
                .clip(CircleShape)
                .background(lerp(Fresh.Green, SoakBottom, t))
                .clickable(enabled = !watered, onClick = onWater)
                .semantics { contentDescription = if (watered) "Watered" else "I watered ${row.plant.name}" },
        ) {
            Text(
                if (t > 0.5f) "✓" else "💧",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
        }
        Text(
            if (watered) "Watered" else "I watered it",
            style = Label,
            color = Fresh.GreenDeep,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** The darker green pouring down the card: [p] 0 is none, 1 is the whole card, with a soft front. */
private fun DrawScope.soakDown(p: Float) {
    if (p <= 0f) return
    val edge = size.height * 0.4f
    val front = p * (size.height + edge)
    drawRect(Brush.verticalGradient(listOf(SoakTop, SoakBottom)))
    drawRect(
        Brush.verticalGradient(listOf(Color.Black, Color.Transparent), startY = front - edge, endY = front),
        blendMode = BlendMode.DstIn,
    )
}

/**
 * "Still moist": when do you think it will need water? That day becomes its due day, and a
 * repeating plant's interval runs on from there. × or a tap outside closes it.
 */
@Composable
internal fun MoistDialog(row: PlantRow, today: LocalDate, onSet: (Int) -> Unit, onDismiss: () -> Unit) {
    var days by remember { mutableIntStateOf(2) }
    val range = PlantRepository.MOIST_DAYS
    Dialog(onDismissRequest = onDismiss) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Fresh.Page)
                .padding(20.dp),
        ) {
            Box(Modifier.fillMaxWidth()) {
                Text(
                    "×",
                    style = MaterialTheme.typography.titleLarge,
                    color = Fresh.InkSoft,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                        .semantics { contentDescription = "Close" }
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                )
            }
            Text("Still moist", style = MaterialTheme.typography.headlineSmall, color = Fresh.Ink)
            Text(
                "When do you think ${row.plant.name} will need water?",
                style = MaterialTheme.typography.bodyMedium,
                color = Fresh.InkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 22.dp)) {
                StepButton("−", "Fewer days", enabled = days > range.first) { days-- }
                Text(
                    "$days",
                    style = MaterialTheme.typography.displaySmall,
                    color = Fresh.Ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(84.dp),
                )
                StepButton("+", "More days", enabled = days < range.last) { days++ }
            }
            val on = today.plusDays(days.toLong())
            val then = (row.plant.recurrence as? Recurrence.EveryDays)?.let { ", then ${PlantCopy.interval(it)}" }.orEmpty()
            Text(
                "${if (days == 1) "in 1 day" else "in $days days"} · ${on.format(shortDate)}$then",
                style = Label,
                color = Fresh.GreenDeep,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(Fresh.Green)
                    .clickable { onSet(days) }
                    .padding(vertical = 15.dp),
            ) {
                Text("Water then", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
    }
}

@Composable
private fun StepButton(glyph: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Fresh.Card)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Text(glyph, style = MaterialTheme.typography.titleLarge, color = if (enabled) Fresh.Ink else Fresh.InkSoft.copy(alpha = 0.4f))
    }
}
