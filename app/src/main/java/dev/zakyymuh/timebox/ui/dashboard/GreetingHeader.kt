package dev.zakyymuh.timebox.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.zakyymuh.timebox.ui.theme.Fresh

/** The pale wash behind the header, top to bottom; deep violet and navy at night. */
private val WashTop: Color get() = if (Fresh.Dark) Color(0xFF1D1827) else Color(0xFFF3ECFD)
private val WashBottom: Color get() = if (Fresh.Dark) Color(0xFF151C24) else Color(0xFFF1F8FF)

/** Below the header the page fades in over this much, so cards scroll softly under it. */
private val FADE = 28.dp

/**
 * The dashboard's header, pinned at the top from under the status bar: soft blurred light
 * (lilac, mint, blue) over a pale wash, fading into the page at its bottom edge.
 */
@Composable
fun GreetingHeader(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(WashTop, WashBottom))),
        ) {
            Glow(Modifier.matchParentSize())
            Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 14.dp), content = content)
        }
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(FADE)
                .background(Brush.verticalGradient(listOf(WashBottom, Fresh.Page.copy(alpha = 0f)))),
        )
    }
}

@Composable
private fun Glow(modifier: Modifier) {
    BoxWithConstraints(modifier.clipToBounds().blur(36.dp, BlurredEdgeTreatment.Rectangle)) {
        // Dimmer at night, so the light stays soft on the dark page.
        val glow = if (Fresh.Dark) 0.35f else 1f
        Blob(Lilac.copy(alpha = glow), 180.dp, x = maxWidth * 0.55f, y = (-40).dp)
        Blob(Color(0xFFB8F0E6).copy(alpha = glow), 150.dp, x = (-30).dp, y = 30.dp)
        Blob(Color(0xFF9FD3FF).copy(alpha = glow), 130.dp, x = maxWidth * 0.3f, y = 70.dp)
    }
}

@Composable
private fun Blob(colour: Color, size: Dp, x: Dp, y: Dp) {
    Box(
        Modifier
            .offset(x = x, y = y)
            .size(size)
            .background(Brush.radialGradient(listOf(colour.copy(alpha = 0.55f * colour.alpha), colour.copy(alpha = 0f))), CircleShape),
    )
}
