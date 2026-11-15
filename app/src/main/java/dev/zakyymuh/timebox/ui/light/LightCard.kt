package dev.zakyymuh.timebox.ui.light

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import dev.zakyymuh.timebox.ui.theme.Label

private val SunTop = Color(0xFFFFF6DC)
private val SunBottom = Color(0xFFFFE1B0)
private val Ink = Color(0xFF5C3A0E)

/**
 * The way into the light check: a square tile on a warm sunny gradient, ☀️ in a white
 * circle on top, to sit beside the soil tile on the dashboard.
 */
@Composable
fun LightCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(10.dp, shape, ambientColor = SunBottom, spotColor = SunBottom)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(SunTop, SunBottom)))
            .clickable(role = Role.Button, onClickLabel = "Open light check", onClick = onClick),
    ) {
        Column(Modifier.matchParentSize().padding(18.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)),
            ) {
                Text("☀️", fontSize = 24.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("Light check", style = Label, color = Ink.copy(alpha = 0.7f), maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(
                "How bright is this spot?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Ink,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
