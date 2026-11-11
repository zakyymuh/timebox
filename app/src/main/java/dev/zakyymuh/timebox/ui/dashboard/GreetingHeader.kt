package dev.zakyymuh.timebox.ui.dashboard

import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
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

/**
 * The top of the dashboard, from under the status bar: soft blurred light (lilac, mint,
 * blue) gathered at the very top, thinning out until it is the page itself by the
 * header's bottom edge, so there is no line between it and the cards. It is part of the
 * page and scrolls away with it.
 */
@Composable
fun GreetingHeader(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    // Strong at the top, gone at the bottom: the page shows through.
                    drawRect(
                        Brush.verticalGradient(0f to Color.Black, 0.35f to Color.Black, 1f to Color.Transparent),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(WashTop, WashBottom))))
            Glow(Modifier.matchParentSize())
        }
        Column(
            Modifier.statusBarsPadding().padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 24.dp),
            content = content,
        )
    }
}

@Composable
private fun Glow(modifier: Modifier) {
    BoxWithConstraints(modifier.clipToBounds().blur(36.dp, BlurredEdgeTreatment.Rectangle)) {
        // Dimmer at night, so the light stays soft on the dark page.
        val glow = if (Fresh.Dark) 0.35f else 1f
        // Gathered up high, under the status bar, so the light comes from the top.
        Blob(Lilac.copy(alpha = glow), 200.dp, x = maxWidth * 0.55f, y = (-80).dp)
        Blob(Color(0xFFB8F0E6).copy(alpha = glow), 170.dp, x = (-40).dp, y = (-30).dp)
        Blob(Color(0xFF9FD3FF).copy(alpha = glow), 140.dp, x = maxWidth * 0.3f, y = 10.dp)
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
