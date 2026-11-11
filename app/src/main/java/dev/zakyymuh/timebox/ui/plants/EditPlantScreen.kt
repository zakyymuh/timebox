package dev.zakyymuh.timebox.ui.plants

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.timebox.core.plants.PlantColour
import dev.zakyymuh.timebox.core.plants.PlantForm
import dev.zakyymuh.timebox.ui.common.DateField
import dev.zakyymuh.timebox.ui.common.TopBar
import dev.zakyymuh.timebox.ui.common.softFieldColors
import dev.zakyymuh.timebox.ui.common.SectionTitle
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Small
import java.time.LocalDate


@Composable
fun EditPlantScreen(viewModel: EditPlantViewModel, onDone: () -> Unit) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val canSave by viewModel.canSave.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(containerColor = Fresh.Page) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .padding(horizontal = 20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            TopBar(title = if (viewModel.isNew) "New plant" else "Edit plant", onBack = onDone)
            if (!loaded) return@Column

            TextField(
                value = form.name,
                onValueChange = { viewModel.update(form.copy(name = it.take(PlantForm.MAX_NAME))) },
                placeholder = { Text("Plant name", color = Fresh.InkSoft) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = softFieldColors(),
                textStyle = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle("Colour")
            ColourRow(selected = form.colour, onPick = { viewModel.update(form.copy(colour = it)) })

            SectionTitle("Watering")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice("Repeats", selected = form.repeats) { viewModel.update(form.copy(repeats = true)) }
                Choice("One time", selected = !form.repeats) { viewModel.update(form.copy(repeats = false)) }
            }
            Spacer(Modifier.height(14.dp))

            if (form.repeats) {
                IntervalField(form.everyDays) { viewModel.update(form.copy(everyDays = it)) }
                // Only asked when adding: an edit never moves the schedule by itself.
                if (viewModel.isNew) {
                    Spacer(Modifier.height(14.dp))
                    DateField("Last watered", form.lastWatered) { viewModel.update(form.copy(lastWatered = it)) }
                }
            } else {
                DateField("Water on", form.onceDate) { viewModel.update(form.copy(onceDate = it)) }
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { viewModel.save(onDone) },
                enabled = canSave,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Fresh.Ink,
                    contentColor = Fresh.Page,
                    disabledContainerColor = Fresh.Card,
                    disabledContentColor = Fresh.InkSoft,
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text("Save", style = MaterialTheme.typography.labelLarge)
            }

            if (!viewModel.isNew) {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Delete plant", color = Fresh.Error)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${form.name.trim()}?") },
            text = { Text("Its watering history goes too. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete(onDone) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ColourRow(selected: PlantColour, onPick: (PlantColour) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PlantColour.entries.forEach { colour ->
            // The chosen colour gets a green ring with a white gap, so it reads as "picked".
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (colour == selected) Fresh.Green else Color.Transparent)
                    .clickable { onPick(colour) },
            ) {
                Box(
                    Modifier
                        .size(if (colour == selected) 36.dp else 40.dp)
                        .clip(CircleShape)
                        .background(Fresh.Page)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(colour.toColor()),
                )
            }
        }
    }
}

@Composable
internal fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Fresh.GreenDeep else Fresh.Ink,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Fresh.Pastel else Fresh.Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

@Composable
private fun IntervalField(days: Int, onChange: (Int) -> Unit) {
    // Kept as text so the field can be empty while typing; an empty field counts as 0,
    // which the form rejects, so Save stays disabled until it is a real number.
    var text by remember { mutableStateOf(days.toString()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Every", style = MaterialTheme.typography.bodyMedium, color = Fresh.Ink)
        Spacer(Modifier.width(10.dp))
        TextField(
            value = text,
            onValueChange = { value ->
                text = value.filter(Char::isDigit).take(2)
                onChange(text.toIntOrNull() ?: 0)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(20.dp),
            colors = softFieldColors(),
            textStyle = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(84.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text("days", style = MaterialTheme.typography.bodyMedium, color = Fresh.Ink)
    }
    Text("1 to 90", style = Small, color = Fresh.InkSoft, modifier = Modifier.padding(top = 4.dp))
}
