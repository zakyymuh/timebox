package dev.zakyymuh.timebox.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.graphics.Brush
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
import dev.zakyymuh.timebox.core.plants.WateringMonth
import dev.zakyymuh.timebox.ui.common.SoftCard
import dev.zakyymuh.timebox.ui.plants.PlantDot
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val heading = DateTimeFormatter.ofPattern("EEEE d MMMM")
private const val GREETING = "Hello, Zaky 👋"
private val ICON = 56.dp
private val SMALL_ICON = 40.dp

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

            state.date?.let { date -> state.month?.let { MonthCard(it, date, onOpenHistory) } }

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
    backdrop: @Composable BoxScope.() -> Unit = {},
    icon: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(10.dp, shape, ambientColor = Fresh.Ink.copy(alpha = 0.08f), spotColor = Fresh.Ink.copy(alpha = 0.08f))
            .clip(shape)
            .background(color)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        backdrop()
        Column(Modifier.matchParentSize().padding(18.dp)) {
        icon()
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(ink, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(label, style = Label, color = ink.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(4.dp))
        Text(title, style = titleStyle, fontWeight = FontWeight.Bold, color = ink, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * The way into the collection: a random photo from it fills the tile (new each open),
 * darkened at the bottom so the white text reads. Lilac until there is a photo.
 */
@Composable
private fun CollectionTile(cover: String?, owned: String, onOpen: () -> Unit, modifier: Modifier) {
    Tile(
        Lilac, Color.White, owned, "Collection", onOpen, "Open collection", modifier,
        backdrop = {
            if (cover != null) {
                DexPhoto(cover, Modifier.matchParentSize(), side = 480)
                Box(
                    Modifier.matchParentSize().background(
                        Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.6f)),
                    ),
                )
            }
        },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(ICON).clip(CircleShape).background(Color.White),
        ) {
            Text("🪴", fontSize = 26.sp)
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
        // Smaller than the collection's icon, so a whole line about what happened fits.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(SMALL_ICON).clip(CircleShape).background(Lilac.copy(alpha = 0.18f)),
        ) {
            Text("🕘", fontSize = 18.sp)
        }
    }
}

/** This month's watering: how many so far, against last month, and per plant. Opens the history. */
@Composable
private fun MonthCard(month: WateringMonth, today: LocalDate, onOpen: () -> Unit) {
    SoftCard(
        color = Fresh.Page,
        corner = 32.dp,
        onClick = onOpen,
        modifier = Modifier
            .padding(top = 14.dp)
            .shadow(10.dp, RoundedCornerShape(32.dp), ambientColor = Fresh.Ink.copy(alpha = 0.08f), spotColor = Fresh.Ink.copy(alpha = 0.08f)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(Fresh.Green, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text("💧 This month", style = Label, color = Fresh.InkSoft)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                WateringMonth.headline(month),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Fresh.Ink,
                modifier = Modifier.weight(1f),
            )
            Text(WateringMonth.compare(month, today), style = Label, color = Fresh.InkSoft)
        }
        if (month.perPlant.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            month.perPlant.forEach { (plant, count) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    PlantDot(plant.colour, size = 10.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        plant.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Fresh.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text("×$count", style = Label, color = Fresh.InkSoft)
                }
            }
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
