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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
import dev.zakyymuh.timebox.core.collection.Breeding
import dev.zakyymuh.timebox.core.collection.Cross
import dev.zakyymuh.timebox.ui.soil.SoilWaterCard
import dev.zakyymuh.timebox.core.soil.SampleStatus
import dev.zakyymuh.timebox.ui.common.SoftCard
import dev.zakyymuh.timebox.ui.plants.PlantDot
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val GREETING = "Hello, Zaky 👋"

/** One padding, corner and gap for every dashboard card, so their edges line up. */
internal val CARD_PADDING = 20.dp
internal val CARD_CORNER = 32.dp
internal val CARD_GAP = 14.dp
private val ICON = 56.dp
private val SMALL_ICON = 40.dp

/**
 * The front door: a greeting with a plant fact, the plant schedule as a week, two tiles
 * (the collection and a random thing that happened, which opens the history), the soil
 * air-space check, and breeding.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    soil: SampleStatus?,
    onOpenSoil: () -> Unit,
    crosses: List<Cross>?,
    onOpenBreeding: () -> Unit,
    onOpenPlants: () -> Unit,
    onOpenCollection: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val density = LocalDensity.current
    // The pinned header's height, so the cards start below it and then scroll under it.
    var header by remember { mutableStateOf(0.dp) }

    // The header draws under the status bar itself, so the page takes no top inset here.
    Scaffold(containerColor = Fresh.Page, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { insets ->
        Box(Modifier.padding(insets).fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            Spacer(Modifier.height(header))

            state.date?.let { WeekCard(state.week, it, onOpenPlants) }

            Row(Modifier.padding(top = CARD_GAP)) {
                CollectionTile(state.cover, state.owned, onOpenCollection, Modifier.weight(1f))
                Spacer(Modifier.width(CARD_GAP))
                EventTile(state.event, onOpenHistory, Modifier.weight(1f))
            }

            soil?.let { SoilWaterCard(it, onOpenSoil, Modifier.padding(top = CARD_GAP)) }
            crosses?.let { BreedingCard(it.firstOrNull(), onOpenBreeding) }
            Spacer(Modifier.height(32.dp))
        }
        GreetingHeader(Modifier.onSizeChanged { header = with(density) { it.height.toDp() } - 12.dp }) {
            Text(GREETING, style = MaterialTheme.typography.headlineLarge, color = Fresh.Ink)
            FactLine(state.fact?.text.orEmpty(), viewModel::shuffleFact)
        }
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
    val shape = RoundedCornerShape(CARD_CORNER)
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
        Column(Modifier.matchParentSize().padding(CARD_PADDING)) {
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

/** The way into breeding: full width, a heart, and a line about the latest cross. */
@Composable
private fun BreedingCard(latest: Cross?, onOpen: () -> Unit) {
    SoftCard(
        color = Fresh.Page,
        corner = CARD_CORNER,
        onClick = onOpen,
        modifier = Modifier
            .padding(top = CARD_GAP)
            .shadow(10.dp, RoundedCornerShape(CARD_CORNER), ambientColor = Fresh.Ink.copy(alpha = 0.08f), spotColor = Fresh.Ink.copy(alpha = 0.08f)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(SMALL_ICON).clip(CircleShape).background(Color(0xFFFFE3EA)),
            ) {
                Text("❤️", fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Text("Breeding", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Fresh.Ink, modifier = Modifier.weight(1f))
            Text("→", style = MaterialTheme.typography.titleMedium, color = Fresh.InkSoft)
        }
        Spacer(Modifier.height(10.dp))
        Text(Breeding.teaser(latest), style = MaterialTheme.typography.bodyMedium, color = Fresh.InkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** A plant fact under the greeting. A tap shows another. */
@Composable
private fun FactLine(text: String, onShuffle: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = Fresh.InkSoft,
        modifier = Modifier
            .padding(top = 4.dp)
            .fillMaxWidth()
            .clickable(onClickLabel = "Show another fact", onClick = onShuffle),
    )
}
