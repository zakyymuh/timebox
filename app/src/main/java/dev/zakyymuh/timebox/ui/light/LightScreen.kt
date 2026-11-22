package dev.zakyymuh.timebox.ui.light

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.zakyymuh.timebox.core.light.LightBand
import dev.zakyymuh.timebox.core.light.LightLevel
import dev.zakyymuh.timebox.data.SavedLight
import dev.zakyymuh.timebox.ui.common.RoundButton
import dev.zakyymuh.timebox.ui.common.SectionTitle
import dev.zakyymuh.timebox.ui.common.SoftCard
import dev.zakyymuh.timebox.ui.common.TopBar
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import dev.zakyymuh.timebox.ui.theme.Small
import kotlinx.coroutines.launch

/** Each band's swatch, from shade to full sun. */
internal fun LightBand.colour(): Color = when (this) {
    LightBand.Dark -> Color(0xFF5B6270)
    LightBand.Low -> Color(0xFF8A9A8C)
    LightBand.Medium -> Color(0xFF9DBF8F)
    LightBand.Bright -> Color(0xFFF2C46B)
    LightBand.Sun -> Color(0xFFF59E42)
}

/**
 * Light check: the readings you saved, newest first, and a ☀️ that saves the light right
 * now. The live meter (the reading, the brightest seen since opening with ↺ to reset it,
 * the scale and how to hold the phone) lives in the ? card.
 */
@Composable
fun LightScreen(readings: List<SavedLight>, onBack: () -> Unit, onSave: suspend (Float) -> Unit) {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val sensor = remember { manager?.getDefaultSensor(Sensor.TYPE_LIGHT) }
    var lux by remember { mutableStateOf<Float?>(null) }
    var peak by remember { mutableStateOf<Float?>(null) }
    var showInfo by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (manager != null && sensor != null) {
        DisposableEffect(sensor) {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val now = LightLevel.smooth(lux, event.values[0])
                    lux = now
                    peak = maxOf(peak ?: now, now)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { manager.unregisterListener(listener) }
        }
    }

    Scaffold(
        containerColor = Fresh.Page,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            if (sensor != null) {
                FloatingActionButton(
                    onClick = {
                        lux?.let { now ->
                            // A failed save (disk full, say) just adds no line instead of crashing.
                            scope.launch { runCatching { onSave(now) } }
                        }
                    },
                    containerColor = Fresh.Green,
                    shape = CircleShape,
                    modifier = Modifier.semantics { contentDescription = "Save the light right now" },
                ) {
                    Text("☀️", style = MaterialTheme.typography.titleLarge)
                }
            }
        },
    ) { insets ->
        Column(
            Modifier.padding(insets).padding(horizontal = 20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            TopBar(title = "Light check", onBack = onBack) {
                RoundButton("?", onClick = { showInfo = true }, description = "About the light meter")
            }
            if (readings.isNotEmpty()) SavedList(readings)
            Spacer(Modifier.height(96.dp))
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            containerColor = Fresh.Page,
            confirmButton = {
                TextButton(onClick = { showInfo = false }) { Text("Close", color = Fresh.GreenDeep) }
            },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { MeterInfo(sensor != null, lux, peak, onReset = { peak = lux }) } },
        )
    }
}

/** Each saved reading: its band's swatch, the lux, and what kind of spot that is. */
@Composable
private fun SavedList(readings: List<SavedLight>) {
    SoftCard(corner = 24.dp) {
        readings.forEach { r ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                Box(Modifier.size(12.dp).background(r.band.colour(), CircleShape))
                Spacer(Modifier.width(12.dp))
                Text(
                    "${LightLevel.format(r.lux)} lux",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Fresh.Ink,
                    modifier = Modifier.weight(1f),
                )
                Text(r.band.title, style = MaterialTheme.typography.bodyMedium, color = Fresh.InkSoft)
            }
        }
    }
}

/** What the page used to show: the live reading, the brightest, the scale, how to measure. */
@Composable
private fun MeterInfo(hasSensor: Boolean, lux: Float?, peak: Float?, onReset: () -> Unit) {
    if (!hasSensor) {
        SoftCard(corner = 24.dp) {
            Text("No light sensor", style = MaterialTheme.typography.titleMedium, color = Fresh.Ink)
            Spacer(Modifier.height(4.dp))
            Text(
                "This phone doesn't have a light sensor, so it can't measure a spot. A plant's sun needs are still in the encyclopedia.",
                style = MaterialTheme.typography.bodyMedium,
                color = Fresh.InkSoft,
            )
        }
    } else {
        Reading(lux)
        peak?.let {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 8.dp)) {
                Text(
                    "Brightest since you opened this: ${LightLevel.format(it)} lux · ${LightLevel.band(it).title.lowercase()}",
                    style = Small,
                    color = Fresh.InkSoft,
                    modifier = Modifier.weight(1f),
                )
                RoundButton("↺", onClick = onReset, size = 36.dp, description = "Reset brightest")
            }
        }
        SectionTitle("What the numbers mean")
        Scale(lux?.let(LightLevel::band))
    }

    SectionTitle("How to measure")
    SoftCard(corner = 20.dp) {
        listOf(
            "Put the phone where the plant sits, at the height of its leaves.",
            "Face the screen towards the window or lamp: the sensor sits next to the front camera.",
            "Keep your hand and shadow off the top of the phone and wait a few seconds for the number to settle.",
            "Light changes through the day. Check around midday, and again in the afternoon, to know a spot.",
        ).forEachIndexed { i, step ->
            Row(Modifier.padding(bottom = 8.dp)) {
                Text("${i + 1}.", style = MaterialTheme.typography.bodyMedium, color = Fresh.Green, modifier = Modifier.width(22.dp))
                Text(step, style = MaterialTheme.typography.bodyMedium, color = Fresh.Ink)
            }
        }
        Text("Phones differ: treat it as a guide, not a meter.", style = Small, color = Fresh.InkSoft)
    }
}

@Composable
private fun Reading(lux: Float?) {
    val band = lux?.let(LightLevel::band)
    SoftCard(color = band?.colour()?.copy(alpha = 0.22f) ?: Fresh.Card, corner = 28.dp) {
        Text("Right now", style = Label, color = Fresh.Ink.copy(alpha = 0.7f))
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Text(
                lux?.let(LightLevel::format) ?: "…",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Fresh.Ink,
            )
            Spacer(Modifier.width(8.dp))
            Text("lux", style = MaterialTheme.typography.titleMedium, color = Fresh.InkSoft, modifier = Modifier.padding(bottom = 10.dp))
        }
        if (band != null) {
            Text(band.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Fresh.Ink)
            Spacer(Modifier.height(2.dp))
            Text(band.verdict, style = MaterialTheme.typography.bodyMedium, color = Fresh.Ink)
        } else {
            Text("Waiting for the sensor…", style = MaterialTheme.typography.bodyMedium, color = Fresh.InkSoft)
        }
    }
}

/** Every band with its range; the one the reading is in stands out. */
@Composable
private fun Scale(current: LightBand?) {
    SoftCard(corner = 20.dp) {
        val bands = LightBand.entries
        bands.forEachIndexed { i, band ->
            val next = bands.getOrNull(i + 1)
            val range = when (next) {
                null -> "${LightLevel.format(band.fromLux)}+ lux"
                else -> "${LightLevel.format(band.fromLux)}–${LightLevel.format(next.fromLux)} lux"
            }
            val here = band == current
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 5.dp)) {
                Box(Modifier.size(if (here) 14.dp else 10.dp).background(band.colour(), CircleShape))
                Spacer(Modifier.width(12.dp))
                Text(
                    band.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (here) FontWeight.Bold else FontWeight.Normal,
                    color = if (here) Fresh.Ink else Fresh.InkSoft,
                    modifier = Modifier.weight(1f),
                )
                Text(range, style = Small, color = if (here) Fresh.Ink else Fresh.InkSoft)
            }
        }
    }
}
