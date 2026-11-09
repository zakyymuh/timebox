package dev.zakyymuh.timebox.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.zakyymuh.timebox.ui.theme.Small
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FabPosition
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.timebox.ui.collection.DexPhoto
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import java.time.format.DateTimeFormatter

private val heading = DateTimeFormatter.ofPattern("EEEE d MMMM")
private const val GREETING = "Hello, Zaky 👋"
private val TILE = 88.dp

/**
 * The front door: a greeting, the plant schedule as a week, the collection beside a line
 * about something that happened, and a plant fact, with "Add plant" floating at the
 * bottom centre.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenPlants: () -> Unit,
    onOpenCollection: () -> Unit,
    onAddPlant: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        containerColor = Fresh.Page,
        floatingActionButton = { AddPlantButton(onAddPlant) },
        floatingActionButtonPosition = FabPosition.Center,
    ) { insets ->
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

            state.date?.let { WeekCard(state.week, it, onOpenPlants) }

            Row(Modifier.padding(top = 14.dp).height(IntrinsicSize.Min)) {
                CollectionTile(state.cover, onOpenCollection)
                state.event?.let {
                    Spacer(Modifier.width(14.dp))
                    EventCard(it, viewModel::shuffleEvent, Modifier.weight(1f).fillMaxHeight())
                }
            }

            state.fact?.let { FactLine(it.text, viewModel::shuffleFact) }
            // Room for the floating button, so the last card can scroll clear of it.
            Spacer(Modifier.height(96.dp))
        }
    }
}

/** The way into the collection: a square with the newest photo, or 🪴 before there is one. */
@Composable
private fun CollectionTile(cover: String?, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(TILE)
            .clip(shape)
            .background(Fresh.Card)
            .clickable(role = Role.Button, onClickLabel = "Open collection", onClick = onOpen)
            .semantics { contentDescription = "Open collection" },
    ) {
        if (cover == null) Text("🪴", fontSize = 36.sp)
        else DexPhoto(cover, Modifier.size(TILE), side = 240)
    }
}

/** "You last watered Basil 4 days ago." A tap shows another. */
@Composable
private fun EventCard(line: String, onShuffle: () -> Unit, modifier: Modifier) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Fresh.Pastel)
            .clickable(onClickLabel = "Show another", onClick = onShuffle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(line, style = MaterialTheme.typography.bodyLarge, color = Fresh.GreenDeep, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

/** A plant fact, small and quiet. A tap shows another. */
@Composable
private fun FactLine(text: String, onShuffle: () -> Unit) {
    Text(
        text,
        style = Small,
        color = Fresh.InkSoft,
        modifier = Modifier
            .padding(top = 18.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = "Show another fact", onClick = onShuffle)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    )
}

/** The floating "+ Add plant" pill at the bottom centre. */
@Composable
private fun AddPlantButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .shadow(12.dp, shape, ambientColor = Fresh.Ink.copy(alpha = 0.2f), spotColor = Fresh.Ink.copy(alpha = 0.2f))
            .clip(shape)
            .background(Fresh.Ink)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text("+", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(Modifier.width(10.dp))
        Text("Add plant", style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}
