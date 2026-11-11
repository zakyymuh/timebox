package dev.zakyymuh.timebox.ui.breeding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zakyymuh.timebox.ui.theme.Fresh
import kotlinx.coroutines.delay

private val SeedColour = Color(0xFFF48FB1)
private val PollenColour = Color(0xFFFFC94D)
private val SproutBlue = Color(0xFFCFE6FF)
private val CIRCLE = 96.dp

/**
 * Plays once a cross is saved, over the whole screen: two circles come in from the top
 * and the bottom and merge in the middle, spin while they shrink, turn into a heart, and
 * the heart becomes a sprout on a blue circle. Then [onDone]. Taps are swallowed meanwhile.
 */
@Composable
fun BreedAnimation(onDone: () -> Unit) {
    val meet = remember { Animatable(0f) }
    val spin = remember { Animatable(0f) }
    val heart = remember { Animatable(0f) }
    val sprout = remember { Animatable(0f) }
    val finish by rememberUpdatedState(onDone)

    LaunchedEffect(Unit) {
        meet.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        spin.animateTo(1f, tween(600, easing = FastOutLinearInEasing))
        heart.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f))
        delay(350)
        sprout.animateTo(1f, tween(450))
        delay(700)
        finish()
    }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Fresh.Page)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        // Start just inside the top and bottom edges.
        val travel = (maxHeight - CIRCLE) / 2 - 24.dp

        if (spin.value < 1f) {
            // Merged but a little apart sideways, so the spin shows; together by the end.
            val apart = 18.dp * meet.value * (1f - spin.value)
            val shrink = 1f - 0.8f * spin.value
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.graphicsLayer {
                    rotationZ = 360f * spin.value
                    scaleX = shrink
                    scaleY = shrink
                },
            ) {
                Circle(SeedColour, x = -apart, y = -travel * (1f - meet.value))
                Circle(PollenColour, x = apart, y = travel * (1f - meet.value))
            }
        }

        if (spin.value >= 1f) {
            Text(
                "❤️",
                fontSize = 72.sp,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = heart.value
                        scaleY = heart.value
                    }
                    .alpha(1f - sprout.value),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer {
                        val s = 0.6f + 0.4f * sprout.value
                        scaleX = s
                        scaleY = s
                    }
                    .alpha(sprout.value)
                    .size(140.dp)
                    .background(SproutBlue, CircleShape),
            ) {
                Text("🌱", fontSize = 64.sp)
            }
            Text(
                "A new cross!",
                style = MaterialTheme.typography.titleLarge,
                color = Fresh.Ink,
                modifier = Modifier.align(Alignment.Center).padding(top = 220.dp).alpha(sprout.value),
            )
        }
    }
}

@Composable
private fun Circle(colour: Color, x: Dp, y: Dp) {
    Box(
        Modifier
            .offset(x = x, y = y)
            .size(CIRCLE)
            .alpha(0.85f)
            .background(colour, CircleShape),
    )
}
