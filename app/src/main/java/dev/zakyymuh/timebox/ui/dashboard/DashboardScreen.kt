package dev.zakyymuh.timebox.ui.dashboard

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.timebox.core.plants.FunFact
import dev.zakyymuh.timebox.ui.collection.DexPhoto
import dev.zakyymuh.timebox.ui.common.Pill
import dev.zakyymuh.timebox.ui.common.RoundButton
import dev.zakyymuh.timebox.ui.common.SoftCard
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import java.time.format.DateTimeFormatter

private val heading = DateTimeFormatter.ofPattern("EEEE d MMMM")
private const val GREETING = "Hello, Zaky 👋"

/** The front door: a greeting, the two big cards, the collection, and a fun fact. */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenHabit: () -> Unit,
    onOpenPlants: () -> Unit,
    onOpenCollection: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onOpenEncyclopedia: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(containerColor = Fresh.Page) { insets ->
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
                    dark = true,
                    onClick = onOpenHabit,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) { soft -> HabitProgress(state, soft) }
                DoorCard(
                    title = "Plant schedule",
                    dark = false,
                    onClick = onOpenPlants,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) { soft ->
                    Text(state.plants, style = Label, color = soft, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }

            CollectionCard(state, onOpenCollection)

            state.fact?.let { FactCard(it, onOpenEntry, onOpenEncyclopedia, viewModel::closeFact) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** "2 /5 done": what you did, big; what was planned, small; and a word of cheer. */
@Composable
private fun HabitProgress(state: DashboardState, soft: Color) {
    val habit = state.habit ?: return
    Column {
        if (!habit.isEmpty) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${habit.done}",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                )
                Text(
                    " /${habit.total} done",
                    style = Label,
                    color = soft,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
        }
        Text(state.cheer, style = Label, color = soft, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** One of the two big doors: dark for the daily list, light for plants, like the reference. */
@Composable
private fun DoorCard(
    title: String,
    dark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    status: @Composable (soft: Color) -> Unit,
) {
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
        Row(verticalAlignment = Alignment.Bottom) {
            Box(Modifier.weight(1f)) { status(soft) }
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

/** The plant dex: a few photos, the count, and the way in. */
@Composable
private fun CollectionCard(state: DashboardState, onOpen: () -> Unit) {
    SoftCard(corner = 24.dp, onClick = onOpen, modifier = Modifier.padding(top = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.thumbnails.isEmpty()) {
                DexPhoto(null, Modifier.size(44.dp).clip(CircleShape), emojiSize = 22.sp)
            } else {
                // Overlapping round photos, newest on top.
                Box(Modifier.width((44 + 22 * (state.thumbnails.size - 1)).dp).height(44.dp)) {
                    state.thumbnails.reversed().forEachIndexed { i, photo ->
                        DexPhoto(
                            photo,
                            Modifier
                                .padding(start = (22 * i).dp)
                                .size(44.dp)
                                .border(2.dp, Fresh.Card, CircleShape)
                                .clip(CircleShape),
                            side = 160,
                        )
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("My plant collection", style = MaterialTheme.typography.titleMedium, color = Fresh.Ink)
                Text(state.collection, style = Label, color = Fresh.InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            RoundButton("→", onClick = onOpen, container = Fresh.Page, size = 40.dp, description = "Open collection")
        }
    }
}

/** "Did you know?": a random plant fact each time the app opens; × hides it until next time. */
@Composable
private fun FactCard(
    fact: FunFact,
    onOpenEntry: (String) -> Unit,
    onOpenEncyclopedia: () -> Unit,
    onClose: () -> Unit,
) {
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
            Spacer(Modifier.width(6.dp))
            RoundButton(
                glyph = "×",
                onClick = onClose,
                container = Fresh.Page,
                content = Fresh.GreenDeep,
                size = 32.dp,
                description = "Close fun fact",
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(fact.text, style = MaterialTheme.typography.bodyLarge, color = Fresh.GreenDeep)
        Spacer(Modifier.height(8.dp))
        Text("${fact.entry.name} →", style = Label, color = Fresh.GreenDeep.copy(alpha = 0.75f))
    }
}
