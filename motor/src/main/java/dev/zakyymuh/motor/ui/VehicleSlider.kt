package dev.zakyymuh.motor.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.zakyymuh.motor.R
import dev.zakyymuh.motor.data.PhotoStore
import dev.zakyymuh.timebox.core.lastdone.LastDone
import dev.zakyymuh.timebox.core.motor.Money
import dev.zakyymuh.timebox.core.motor.ServiceLog
import dev.zakyymuh.timebox.core.motor.VehicleType
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import kotlinx.coroutines.launch
import java.time.LocalDate

private val CardHeight = 360.dp
private val ArrowSize = 52.dp

/** The drawing that stands in for a vehicle without a photo. */
private fun VehicleType.drawing(): Int = when (this) {
    VehicleType.MOTORCYCLE -> R.drawable.ic_launcher_foreground
    VehicleType.CAR -> R.drawable.intro_car
    VehicleType.OTHER -> R.drawable.intro_gear
}

/**
 * One big card per vehicle, swiped sideways or stepped with the round arrows on its
 * edges. Tap a card for its service log, ✎ to edit it.
 */
@Composable
fun VehicleSlider(
    cards: List<VehicleCard>,
    today: LocalDate,
    photos: PhotoStore,
    onOpen: (Long) -> Unit,
    onEdit: (Long) -> Unit,
) {
    val pager = rememberPagerState(pageCount = { cards.size })
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pager,
            pageSpacing = 12.dp,
            key = { cards[it].vehicle.id },
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            HeroCard(cards[page], today, photos, onOpen = { onOpen(cards[page].vehicle.id) }, onEdit = { onEdit(cards[page].vehicle.id) })
        }
        if (pager.currentPage > 0) {
            Arrow(
                glyph = "←",
                description = "Previous vehicle",
                container = Fresh.Ink,
                modifier = Modifier.align(Alignment.CenterStart).offset(x = -(ArrowSize / 3)),
            ) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }
        }
        if (pager.currentPage < cards.size - 1) {
            Arrow(
                glyph = "→",
                description = "Next vehicle",
                container = Fresh.Green,
                modifier = Modifier.align(Alignment.CenterEnd).offset(x = ArrowSize / 3),
            ) { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }
        }
    }

    if (cards.size > 1) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            repeat(cards.size) { i ->
                val current = i == pager.currentPage
                Box(
                    Modifier
                        .height(6.dp)
                        .width(if (current) 18.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (current) Fresh.GreenDeep else Fresh.Pastel),
                )
            }
        }
    }
}

/**
 * The vehicle edge to edge: its photo with a dark wash top and bottom so white text
 * reads on any picture, or its drawing on chrome.
 */
@Composable
private fun HeroCard(card: VehicleCard, today: LocalDate, photos: PhotoStore, onOpen: () -> Unit, onEdit: () -> Unit) {
    val vehicle = card.vehicle
    val bitmap by produceState<ImageBitmap?>(null, vehicle.photo) {
        value = vehicle.photo?.let { photos.load(it)?.asImageBitmap() }
    }
    val image = bitmap
    val onPhoto = image != null
    val ink = if (onPhoto) Color.White else Fresh.GreenDeep
    val soft = if (onPhoto) Color.White.copy(alpha = 0.75f) else Fresh.GreenDeep.copy(alpha = 0.7f)

    Box(
        Modifier
            .fillMaxWidth()
            .height(CardHeight)
            .clip(RoundedCornerShape(28.dp))
            .let { if (onPhoto) it.background(Fresh.Ink) else it.background(Chrome) }
            .clickable(onClick = onOpen),
    ) {
        if (image != null) {
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.35f to Color.Transparent,
                        0.6f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.7f),
                    ),
                ),
            )
        } else {
            Image(
                painter = painterResource(vehicle.type.drawing()),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(260.dp),
            )
        }

        Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(vehicle.name, style = MaterialTheme.typography.headlineMedium, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    vehicle.plate.ifEmpty { vehicle.type.label },
                    style = MaterialTheme.typography.titleMedium,
                    color = soft,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (onPhoto) Color.White.copy(alpha = 0.18f) else Fresh.Page.copy(alpha = 0.7f))
                    .clickable(onClick = onEdit)
                    .semantics { contentDescription = "Edit ${vehicle.name}" },
            ) {
                Text("✎", style = MaterialTheme.typography.titleMedium, color = ink)
            }
        }

        val last = ServiceLog.newestFirst(card.services).firstOrNull()
        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(22.dp)) {
            Stat(
                label = "Last service",
                value = last?.let { LastDone.ago(it.date, today).replaceFirstChar(Char::uppercase) } ?: "None yet",
                ink = ink,
                soft = soft,
                modifier = Modifier.weight(1f),
            )
            Stat(
                label = "Spent in ${today.year}",
                value = Money.rupiah(ServiceLog.spentIn(card.services, today.year)),
                ink = ink,
                soft = soft,
                alignEnd = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String, ink: Color, soft: Color, modifier: Modifier = Modifier, alignEnd: Boolean = false) {
    Column(modifier, horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = ink)
        Text(value, style = MaterialTheme.typography.titleLarge, color = soft, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A round arrow with a white ring, half over the card's edge. */
@Composable
private fun Arrow(glyph: String, description: String, container: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(ArrowSize)
            .clip(CircleShape)
            .background(container)
            .border(3.dp, Color.White, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Text(glyph, style = MaterialTheme.typography.titleLarge, color = Color.White)
    }
}
