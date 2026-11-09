package dev.zakyymuh.timebox.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
private val ICON = 56.dp

/**
 * The front door: a greeting, the plant schedule as a week, two tiles (the collection and
 * a random thing that happened, which opens the history), and a plant fact.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenPlants: () -> Unit,
    onOpenCollection: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        containerColor = Fresh.Page,
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

            Row(Modifier.padding(top = 14.dp)) {
                CollectionTile(state.cover, state.owned, onOpenCollection, Modifier.weight(1f))
                Spacer(Modifier.width(14.dp))
                EventTile(state.event, onOpenHistory, Modifier.weight(1f))
            }

            state.fact?.let { FactLine(it.text, viewModel::shuffleFact) }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/**
 * A square tile in the reference's style: an icon in a circle at the top, a small
 * "• label" and a bold title at the bottom.
 */
@Composable
private fun Tile(
    color: Color,
    ink: Color,
    label: String,
    title: String,
    onClick: () -> Unit,
    description: String,
    modifier: Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
    icon: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(32.dp)
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(10.dp, shape, ambientColor = Fresh.Ink.copy(alpha = 0.08f), spotColor = Fresh.Ink.copy(alpha = 0.08f))
            .clip(shape)
            .background(color)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(18.dp),
    ) {
        icon()
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(ink, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(label, style = Label, color = ink.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(4.dp))
        Text(title, style = titleStyle, fontWeight = FontWeight.Bold, color = ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

/** The way into the collection: the newest photo (or 🪴) and how many plants. */
@Composable
private fun CollectionTile(cover: String?, owned: String, onOpen: () -> Unit, modifier: Modifier) {
    Tile(Lilac, Color.White, owned, "Collection", onOpen, "Open collection", modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(ICON).clip(CircleShape).background(Color.White),
        ) {
            if (cover == null) Text("🪴", fontSize = 26.sp)
            else DexPhoto(cover, Modifier.size(ICON), side = 160)
        }
    }
}

/** A random thing that happened, new each time the app opens; the way into the history. */
@Composable
private fun EventTile(event: String?, onOpen: () -> Unit, modifier: Modifier) {
    Tile(
        color = Fresh.Page,
        ink = Fresh.Ink,
        label = "History",
        title = event ?: "Nothing yet",
        onClick = onOpen,
        description = "Open history",
        modifier = modifier,
        titleStyle = MaterialTheme.typography.titleSmall,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(ICON).clip(CircleShape).background(Lilac.copy(alpha = 0.18f)),
        ) {
            Text("🕘", fontSize = 24.sp)
        }
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
