package dev.zakyymuh.timebox.ui.plants

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.zakyymuh.timebox.core.plants.PlantColour

/** Muted enough to sit on paper; lighter twins for the dark theme. */
@Composable
fun PlantColour.toColor(): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (this) {
        PlantColour.MOSS -> if (dark) Color(0xFF8FBF8F) else Color(0xFF5E8C61)
        PlantColour.OCHRE -> if (dark) Color(0xFFE0B26A) else Color(0xFFB7833E)
        PlantColour.DUSK -> if (dark) Color(0xFF8EB1D9) else Color(0xFF5B7FA6)
        PlantColour.CLAY -> if (dark) Color(0xFFE0937A) else Color(0xFFB5654A)
        PlantColour.PLUM -> if (dark) Color(0xFFC08FC2) else Color(0xFF8A5A8C)
        PlantColour.SLATE -> if (dark) Color(0xFF9AA6AE) else Color(0xFF5F6B73)
    }
}

/** A filled dot for something that happened; [hollow] for something planned. */
@Composable
fun PlantDot(colour: PlantColour, size: Dp = 10.dp, hollow: Boolean = false, modifier: Modifier = Modifier) {
    val c = colour.toColor()
    Box(
        modifier
            .size(size)
            .let { if (hollow) it.border(1.5.dp, c, CircleShape) else it.background(c, CircleShape) },
    )
}
