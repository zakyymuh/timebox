package dev.zakyymuh.timebox.ui.soil

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zakyymuh.timebox.core.soil.SampleCheck
import dev.zakyymuh.timebox.core.soil.SampleStatus
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label

private val WaterTop = Color(0xFFE6F5FF)
private val WaterBottom = Color(0xFFBFE2FF)
private val Ink = Color(0xFF0E3A5C)
private val CORNER = 32.dp

/**
 * The soil air-space reminder, on water: a pale blue that drifts softly behind blurred
 * light, a big 🧪 in a white circle, and when the sample is next due. "Due now" when it is.
 * Tappable when [onClick] is given.
 */
@Composable
fun SoilWaterCard(status: SampleStatus, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val due = status is SampleStatus.Due
    val shape = RoundedCornerShape(CORNER)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = WaterBottom, spotColor = WaterBottom)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(WaterTop, WaterBottom)))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
    ) {
        Water(Modifier.matchParentSize())
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(20.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(68.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)),
            ) {
                Text("🧪", fontSize = 32.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Soil air space", style = Label, color = Ink.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
                    if (due) {
                        Text(
                            "Due now",
                            style = Label,
                            color = Color.White,
                            modifier = Modifier.clip(CircleShape).background(Ink).padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                Text(SampleCheck.label(status), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
            }
            if (onClick != null) {
                Spacer(Modifier.width(8.dp))
                Text("→", style = MaterialTheme.typography.titleMedium, color = Ink.copy(alpha = 0.6f))
            }
        }
    }
}

/** Soft blobs of light that drift slowly, blurred like light through water. */
@Composable
private fun Water(modifier: Modifier) {
    val drift = rememberInfiniteTransition(label = "water")
    val t by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )
    BoxWithConstraints(modifier.blur(28.dp, BlurredEdgeTreatment.Unbounded)) {
        Blob(Color(0xFF8FD0FF), 140.dp, x = maxWidth * (0.55f + 0.15f * t), y = (-30).dp + 20.dp * t)
        Blob(Color.White, 120.dp, x = maxWidth * (0.05f + 0.1f * t), y = 40.dp - 25.dp * t)
        Blob(Color(0xFFB8F0E6), 110.dp, x = maxWidth * (0.3f - 0.1f * t), y = 70.dp + 10.dp * t)
    }
}

@Composable
private fun Blob(colour: Color, size: Dp, x: Dp, y: Dp) {
    Box(
        Modifier
            .offset(x = x, y = y)
            .size(size)
            .background(Brush.radialGradient(listOf(colour.copy(alpha = 0.85f), colour.copy(alpha = 0f))), CircleShape),
    )
}
