package dev.zakyymuh.motor.ui

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.zakyymuh.motor.R
import dev.zakyymuh.timebox.ui.theme.Fresh

/*
 * The opening: the motorcycle spins and shrinks away, a car spins out of it; the car
 * spins and shrinks away, a gear spins out of it; the gear turns on as the page fades
 * to home. Each swap happens at the smallest point, so it reads as one shape becoming
 * the next.
 *
 * The first frame is drawn to match the system splash exactly: Android 12+ shows the
 * adaptive icon at 240dp inside a 160dp chrome circle, so the drawings here use those sizes,
 * and the hand-off from the splash to this screen cannot be seen.
 */

private const val HOLD_BIKE = 250
private const val BIKE_OUT = 650
private const val CAR_IN = 1000
private const val HOLD_CAR = 1250
private const val CAR_OUT = 1650
private const val GEAR_IN = 2000
private const val END = 2350

/** How small a shape gets before it turns into the next one. */
private const val SMALLEST = 0.15f

/** [t] from [from] to [to] as 0..1, eased. */
private fun part(t: Float, from: Int, to: Int, easing: Easing): Float =
    easing.transform(((t - from) / (to - from)).coerceIn(0f, 1f))

private fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f

private data class Frame(val drawing: Int, val rotation: Float, val scale: Float, val alpha: Float)

private fun frameAt(t: Float): Frame = when {
    t < BIKE_OUT -> {
        val f = part(t, HOLD_BIKE, BIKE_OUT, FastOutLinearInEasing)
        Frame(R.drawable.ic_launcher_foreground, lerp(0f, 180f, f), lerp(1f, SMALLEST, f), 1f)
    }
    t < HOLD_CAR -> {
        val f = part(t, BIKE_OUT, CAR_IN, LinearOutSlowInEasing)
        Frame(R.drawable.intro_car, lerp(180f, 360f, f), lerp(SMALLEST, 1f, f), 1f)
    }
    t < CAR_OUT -> {
        val f = part(t, HOLD_CAR, CAR_OUT, FastOutLinearInEasing)
        Frame(R.drawable.intro_car, lerp(0f, 180f, f), lerp(1f, SMALLEST, f), 1f)
    }
    else -> {
        val grow = part(t, CAR_OUT, GEAR_IN, LinearOutSlowInEasing)
        // Then it keeps turning, slower, while the page fades.
        val turn = part(t, GEAR_IN, END, LinearOutSlowInEasing)
        val fade = part(t, GEAR_IN, END, LinearEasing)
        Frame(R.drawable.intro_gear, lerp(180f, 360f, grow) + 45f * turn, lerp(SMALLEST, 1f, grow), 1f - fade)
    }
}

/**
 * Plays once over home, which is already loading underneath. A tap or Back skips it.
 * With animations turned off in the phone's settings, it does not play at all.
 */
@Composable
fun LaunchIntro(onDone: () -> Unit) {
    val context = LocalContext.current
    val time = remember { Animatable(0f) }
    val animationsOff = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    LaunchedEffect(Unit) {
        if (!animationsOff) time.animateTo(END.toFloat(), tween(END, easing = LinearEasing))
        onDone()
    }
    BackHandler(onBack = onDone)

    val frame = frameAt(time.value)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = frame.alpha }
            .background(Fresh.Page)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDone),
    ) {
        Box(Modifier.size(160.dp).background(Chrome, CircleShape))
        Image(
            painter = painterResource(frame.drawing),
            contentDescription = null,
            modifier = Modifier
                .size(240.dp)
                .graphicsLayer {
                    rotationZ = frame.rotation
                    scaleX = frame.scale
                    scaleY = frame.scale
                },
        )
    }
}
