package dev.zakyymuh.habits.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zakyymuh.timebox.core.TodoItem
import dev.zakyymuh.timebox.ui.common.RoundButton
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import dev.zakyymuh.timebox.ui.theme.Small
import kotlinx.coroutines.delay

/** The emoji a card can wear. Picked in the add sheet; the first is the default. */
object CardIcons {
    val all = listOf("📝", "🧺", "🏃", "📚", "💻", "🧹", "🍳", "💊", "📞", "🛒", "🌱", "🎯")
    val default = all.first()
}

private val DotIdle = Color(0xFFD6DAD3)

/**
 * The day's things to do as cards, one at a time, swiped sideways.
 *
 * A card starts with "Working on it". Tapping it turns the card dark and locks the swipe
 * until Done is tapped, so the one thing in hand stays on screen. The lock lives in the
 * database rather than here, so it holds after the app is closed and reopened. After Done
 * the cards slide on to the next unfinished one.
 */
@Composable
fun TaskCards(
    items: List<TodoItem>,
    editable: Boolean,
    onStart: (Long) -> Unit,
    onDone: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val working = items.firstOrNull { it.isWorking }
    val pager = rememberPagerState(initialPage = startPage(items)) { items.size }
    var lastWorking by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(working?.id) {
        val finished = lastWorking
        lastWorking = working?.id
        if (working != null) {
            pager.animateScrollToPage(items.indexOfFirst { it.id == working.id })
        } else if (finished != null) {
            val from = items.indexOfFirst { it.id == finished }
            val next = items.indices.firstOrNull { it > from && !items[it].isDone }
                ?: items.indices.firstOrNull { !items[it].isDone }
            if (next != null) {
                // Let the tick land before moving on.
                delay(350)
                pager.animateScrollToPage(next)
            }
        }
    }

    Column(modifier) {
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = 36.dp),
            pageSpacing = 14.dp,
            userScrollEnabled = working == null,
            key = { items[it].id },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            val item = items[page]
            // While one card is in hand the others all but vanish into the dark, and go deaf.
            val sidelined = working != null && !item.isWorking
            val fade by animateFloatAsState(if (sidelined) 0.12f else 1f, tween(450), label = "fade")
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TaskCard(
                    item = item,
                    index = page,
                    count = items.size,
                    editable = editable,
                    onStart = onStart,
                    onDone = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .fillMaxHeight()
                        .graphicsLayer { alpha = fade }
                        .blockTouches(sidelined)
                        .then(
                            // The card is as dark as the app around it now; a faint edge keeps it apart.
                            if (item.isWorking) {
                                Modifier
                                    .shadow(24.dp, RoundedCornerShape(28.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(28.dp))
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Dots(items, pager.currentPage, dark = working != null)
        Text(
            text = if (working != null) "Finish this one first" else "",
            style = Small,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
    }
}

/**
 * Swallows every touch on this element and its children while [on]. The cards' lock: while
 * one card is being worked on, nothing else on the screen answers.
 */
fun Modifier.blockTouches(on: Boolean): Modifier =
    if (!on) this else pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
        }
    }

/** Open on the card in hand, else the first not yet done, else the first. */
private fun startPage(items: List<TodoItem>): Int =
    items.indexOfFirst { it.isWorking }.takeIf { it >= 0 }
        ?: items.indexOfFirst { !it.isDone }.takeIf { it >= 0 }
        ?: 0

@Composable
private fun TaskCard(
    item: TodoItem,
    index: Int,
    count: Int,
    editable: Boolean,
    onStart: (Long) -> Unit,
    onDone: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val working = item.isWorking
    val background by animateColorAsState(if (working) Fresh.Ink else Fresh.Card, label = "card")
    val ink by animateColorAsState(if (working) Color.White else Fresh.Ink, label = "ink")
    val soft = if (working) Color.White.copy(alpha = 0.65f) else Fresh.InkSoft

    Column(
        modifier = modifier
            .alpha(if (item.isDone) 0.6f else 1f)
            .clip(RoundedCornerShape(28.dp))
            .background(background)
            .padding(22.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text("${index + 1} of $count", style = Label, color = soft, modifier = Modifier.weight(1f))
            if (working) Text("In progress", style = Label, color = soft)
        }

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(128.dp)
                .clip(CircleShape)
                .background(if (working) Color.White.copy(alpha = 0.1f) else Fresh.Page),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.icon ?: item.text.take(1).uppercase(),
                style = TextStyle(fontSize = 58.sp),
                color = ink,
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = item.text,
            style = MaterialTheme.typography.headlineSmall,
            color = ink,
            textAlign = TextAlign.Center,
            textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        item.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = soft,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        when {
            item.isDone -> Badge("✓ Done")
            !editable -> Badge("Not done")
            working -> CardButton("Done", Fresh.Green) { onDone(item.id) }
            else -> CardButton("Working on it", Fresh.Ink) { onStart(item.id) }
        }
    }
}

@Composable
private fun CardButton(label: String, container: Color, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(container)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

@Composable
private fun Badge(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Fresh.Pastel)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Fresh.GreenDeep)
    }
}

/** Where you are among the cards; finished ones turn green. */
@Composable
private fun Dots(items: List<TodoItem>, current: Int, dark: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        items.forEachIndexed { i, item ->
            Box(
                Modifier
                    .height(7.dp)
                    .width(if (i == current) 18.dp else 7.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            dark && i == current -> Color.White
                            dark -> Color.White.copy(alpha = 0.18f)
                            i == current -> Fresh.Ink
                            item.isDone -> Fresh.Green
                            else -> DotIdle
                        },
                    ),
            )
        }
    }
}

/** The round + that pulses green while the day is still empty, inviting the first tap. */
@Composable
fun GlowingAddButton(glowing: Boolean, onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "glow")
    val p by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Restart),
        label = "glow",
    )
    Box(contentAlignment = Alignment.Center) {
        if (glowing) {
            Box(
                Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        scaleX = 1f + 0.7f * p
                        scaleY = 1f + 0.7f * p
                        alpha = 0.55f * (1f - p)
                    }
                    .background(Fresh.Green, CircleShape),
            )
            Box(
                Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        scaleX = 1.18f
                        scaleY = 1.18f
                        alpha = 0.3f
                    }
                    .background(Fresh.Green, CircleShape),
            )
        }
        RoundButton(
            glyph = "+",
            onClick = onClick,
            container = Fresh.Ink,
            content = Color.White,
            size = 64.dp,
            description = "Add something to do",
        )
    }
}
