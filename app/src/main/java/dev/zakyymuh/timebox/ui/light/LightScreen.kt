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
import androidx.compose.material3.LargeFloatingActionButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.zakyymuh.timebox.core.light.LightBand
import dev.zakyymuh.timebox.core.light.LightLevel
import dev.zakyymuh.timebox.data.SavedLight
import dev.zakyymuh.timebox.ui.common.RoundButton
import dev.zakyymuh.timebox.ui.common.SoftCard
import dev.zakyymuh.timebox.ui.common.TopBar
import dev.zakyymuh.timebox.ui.theme.Fresh
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
 * Light check: the readings you saved, newest first, and a big ☀️ that saves the light right
 * now. ↺ clears the list (after asking); ? explains what each number and word means.
 */
@Composable
fun LightScreen(
    readings: List<SavedLight>,
    onBack: () -> Unit,
    onSave: suspend (Float) -> Unit,
    onClear: suspend () -> Unit,
) {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val sensor = remember { manager?.getDefaultSensor(Sensor.TYPE_LIGHT) }
    var lux by remember { mutableStateOf<Float?>(null) }
    var showInfo by remember { mutableStateOf(false) }
    var askClear by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (manager != null && sensor != null) {
        DisposableEffect(sensor) {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    lux = LightLevel.smooth(lux, event.values[0])
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
                LargeFloatingActionButton(
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
                    Text("☀️", style = MaterialTheme.typography.displaySmall)
                }
            }
        },
    ) { insets ->
        Column(
            Modifier.padding(insets).padding(horizontal = 20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            TopBar(title = "Light check", onBack = onBack) {
                if (readings.isNotEmpty()) {
                    RoundButton("↺", onClick = { askClear = true }, size = 40.dp, description = "Clear saved light")
                    Spacer(Modifier.width(8.dp))
                }
                RoundButton("?", onClick = { showInfo = true }, size = 40.dp, description = "What the numbers mean")
            }
            if (sensor == null) {
                Text(
                    "This phone has no light sensor, so it can't measure light.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Fresh.InkSoft,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            if (readings.isNotEmpty()) SavedList(readings)
            Spacer(Modifier.height(140.dp))
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            containerColor = Fresh.Page,
            title = { Text("Information") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { BandGuide() } },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) { Text("Close", color = Fresh.GreenDeep) }
            },
        )
    }

    if (askClear) {
        AlertDialog(
            onDismissRequest = { askClear = false },
            containerColor = Fresh.Page,
            title = { Text("Clear all saved light?") },
            text = { Text("Every reading in the list is deleted. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    askClear = false
                    scope.launch { runCatching { onClear() } }
                }) { Text("Clear", color = Fresh.GreenDeep) }
            },
            dismissButton = {
                TextButton(onClick = { askClear = false }) { Text("Cancel", color = Fresh.InkSoft) }
            },
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

/** Every band: its swatch, name and lux range, and what it means for a plant. */
@Composable
private fun BandGuide() {
    val bands = LightBand.entries
    bands.forEachIndexed { i, band ->
        val next = bands.getOrNull(i + 1)
        val range = when (next) {
            null -> "${LightLevel.format(band.fromLux)}+ lux"
            else -> "${LightLevel.format(band.fromLux)}–${LightLevel.format(next.fromLux)} lux"
        }
        Column(Modifier.padding(vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).background(band.colour(), CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(
                    band.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Fresh.Ink,
                    modifier = Modifier.weight(1f),
                )
                Text(range, style = Small, color = Fresh.InkSoft)
            }
            Text(
                band.verdict,
                style = MaterialTheme.typography.bodyMedium,
                color = Fresh.InkSoft,
                modifier = Modifier.padding(start = 22.dp, top = 2.dp),
            )
        }
    }
}
