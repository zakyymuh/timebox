package dev.zakyymuh.habits.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label

/**
 * The two half-width cards under the greeting. On the left, dark: the nearest earlier day and
 * how it went; tap to look at it, and from there tap again to come back to today. On the
 * right, light: a random line about something logged; tap to open History.
 */
@Composable
fun TopTiles(
    isToday: Boolean,
    /** The label and value for the earlier day, or null when there is none yet. */
    earlierLabel: String?,
    earlierValue: String?,
    fact: String?,
    onCalendar: () -> Unit,
    onHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            !isToday -> Tile(
                icon = "📅", label = "Back to", value = "Today", dark = true,
                onClick = onCalendar, modifier = Modifier.weight(1f),
            )
            earlierLabel != null -> Tile(
                icon = "📅", label = earlierLabel, value = earlierValue.orEmpty(), dark = true,
                onClick = onCalendar, modifier = Modifier.weight(1f),
            )
            else -> Tile(
                icon = "📅", label = "Earlier days", value = "None yet", dark = true,
                onClick = null, modifier = Modifier.weight(1f),
            )
        }
        Tile(
            icon = "✨",
            label = if (fact == null) "History" else "Did you know",
            value = fact ?: "Log what you do",
            dark = false,
            small = true,
            onClick = onHistory,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Tile(
    icon: String,
    label: String,
    value: String,
    dark: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    /** A sentence rather than a figure: smaller, and up to two lines. */
    small: Boolean = false,
) {
    val ink = if (dark) Color.White else Fresh.Ink
    val soft = if (dark) Color.White.copy(alpha = 0.6f) else Fresh.InkSoft
    Column(
        modifier = modifier
            .height(136.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (dark) Fresh.Ink else Fresh.Card)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (dark) Color.White.copy(alpha = 0.12f) else Fresh.Page),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.weight(1f))
        Text(label, style = Label, color = soft, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            text = value,
            style = if (small) {
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            } else {
                MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            },
            color = ink,
            maxLines = if (small) 2 else 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
