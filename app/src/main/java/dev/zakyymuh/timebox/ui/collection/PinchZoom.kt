package dev.zakyymuh.timebox.ui.collection

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

private const val MAX_ZOOM = 4f

/**
 * Two fingers zoom and move the picture; letting go springs it back. One finger is left
 * alone, so a tap and a swipe to the next photo still work.
 */
fun Modifier.pinchToZoom(): Modifier = composed {
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var zooming = false
            do {
                val event = awaitPointerEvent()
                if (event.changes.count { it.pressed } >= 2) {
                    zooming = true
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()
                    scope.launch {
                        scale.snapTo((scale.value * zoom).coerceIn(1f, MAX_ZOOM))
                        offset.snapTo(offset.value + pan)
                    }
                }
                // Once two fingers are down the whole gesture is a zoom: no tap, no swipe.
                if (zooming) event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
            if (zooming) {
                scope.launch { scale.animateTo(1f, spring(dampingRatio = 0.7f)) }
                scope.launch { offset.animateTo(Offset.Zero, spring(dampingRatio = 0.7f)) }
            }
        }
    }.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
        translationX = offset.value.x
        translationY = offset.value.y
    }
}
