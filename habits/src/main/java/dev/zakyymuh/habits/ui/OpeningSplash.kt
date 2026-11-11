package dev.zakyymuh.habits.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.zakyymuh.timebox.ui.theme.Fresh
import kotlin.math.pow

private const val RUN_MS = 1_600f
private const val FADE_MS = 350

/**
 * The opening: a dark circle with a calendar in it that turns into a checklist, ticks drawing
 * in one by one, then fades into the app. Played every time the app comes to the front.
 * Swallows touches while it shows.
 *
 * The drawing works in a 48×48 grid, the same as the app icon's checklist.
 */
@Composable
fun OpeningSplash(onFinished: () -> Unit) {
    val clock = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        clock.animateTo(RUN_MS, tween(RUN_MS.toInt(), easing = LinearEasing))
        fade.animateTo(0f, tween(FADE_MS))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .background(Fresh.Page)
            .blockTouches(true),
        contentAlignment = Alignment.Center,
    ) {
        val ms = clock.value
        fun phase(start: Float, length: Float) = ((ms - start) / length).coerceIn(0f, 1f)
        // A little overshoot as the circle pops in.
        val pop = phase(0f, 400f).let { p -> 1f + 2.70158f * (p - 1f).pow(3) + 1.70158f * (p - 1f).pow(2) }

        Canvas(
            Modifier
                .size(128.dp)
                .graphicsLayer {
                    scaleX = 0.6f + 0.4f * pop
                    scaleY = 0.6f + 0.4f * pop
                    alpha = phase(0f, 250f)
                },
        ) {
            drawCircle(Fresh.Ink)
            val icon = 64.dp.toPx()
            val u = icon / 48f
            val inset = (size.width - icon) / 2f
            translate(inset, inset) {
                val line = Stroke(width = 2.6f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
                fun at(x: Float, y: Float) = Offset(x * u, y * u)

                // The calendar, fading out.
                val cal = Fresh.Page.copy(alpha = 1f - phase(550f, 300f))
                drawRoundRect(cal, at(8f, 10f), Size(32f * u, 30f * u), CornerRadius(5f * u), style = line)
                drawLine(cal, at(8f, 18f), at(40f, 18f), line.width, StrokeCap.Round)
                drawLine(cal, at(16f, 6f), at(16f, 13f), line.width, StrokeCap.Round)
                drawLine(cal, at(32f, 6f), at(32f, 13f), line.width, StrokeCap.Round)
                listOf(16f to 25f, 24f to 25f, 32f to 25f, 16f to 33f, 24f to 33f).forEach { (x, y) ->
                    drawCircle(cal, 1.8f * u, at(x, y))
                }

                // The checklist, row by row, then a tick into each box.
                for (row in 0..2) {
                    val top = 11f + 10f * row
                    val rowIn = Fresh.Page.copy(alpha = phase(700f + 80f * row, 250f))
                    drawRoundRect(rowIn, at(11f, top), Size(6f * u, 6f * u), CornerRadius(1.5f * u), style = line)
                    drawLine(rowIn, at(22f, top + 3f), at(38f, top + 3f), line.width, StrokeCap.Round)
                    tick(top, u, phase(1_000f + 120f * row, 220f))
                }
            }
        }
    }
}

/** Draws the first [progress] of the tick in the box whose top edge is at [top]. */
private fun DrawScope.tick(top: Float, u: Float, progress: Float) {
    if (progress <= 0f) return
    val full = Path().apply {
        moveTo(12f * u, (top + 3.2f) * u)
        relativeLineTo(2f * u, 2f * u)
        relativeLineTo(3.6f * u, -4f * u)
    }
    val measure = PathMeasure().apply { setPath(full, false) }
    val part = Path()
    measure.getSegment(0f, measure.length * progress, part, true)
    drawPath(part, Fresh.Green, style = Stroke(width = 3f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
