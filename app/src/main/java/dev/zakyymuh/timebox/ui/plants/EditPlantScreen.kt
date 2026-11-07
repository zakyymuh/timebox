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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.timebox.core.plants.PlantColour
import dev.zakyymuh.timebox.core.plants.PlantForm
import dev.zakyymuh.timebox.ui.common.PaperTopBar
import dev.zakyymuh.timebox.ui.common.SectionLabel
import dev.zakyymuh.timebox.ui.theme.MonoSmall
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateLabel = DateTimeFormatter.ofPattern("EEE d MMM yyyy")

@Composable
fun EditPlantScreen(viewModel: EditPlantViewModel, onDone: () -> Unit) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val canSave by viewModel.canSave.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .padding(horizontal = 22.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            PaperTopBar(title = if (viewModel.isNew) "New plant" else "Edit plant", onBack = onDone)
            if (!loaded) return@Column

            OutlinedTextField(
                value = form.name,
                onValueChange = { viewModel.update(form.copy(name = it.take(PlantForm.MAX_NAME))) },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("Colour")
            ColourRow(selected = form.colour, onPick = { viewModel.update(form.copy(colour = it)) })

            SectionLabel("Watering")
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
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("Save")
            }

            if (!viewModel.isNew) {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Delete plant", color = MaterialTheme.colorScheme.error)
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
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PlantColour.entries.forEach { colour ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(
                        width = if (colour == selected) 2.dp else 0.dp,
                        color = if (colour == selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.background,
                        shape = CircleShape,
                    )
                    .padding(4.dp)
                    .background(colour.toColor(), CircleShape)
                    .clickable { onPick(colour) },
            )
        }
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .background(if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.background, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun IntervalField(days: Int, onChange: (Int) -> Unit) {
    // Kept as text so the field can be empty while typing; an empty field counts as 0,
    // which the form rejects, so Save stays disabled until it is a real number.
    var text by remember { mutableStateOf(days.toString()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Every", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(10.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { value ->
                text = value.filter(Char::isDigit).take(2)
                onChange(text.toIntOrNull() ?: 0)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(84.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text("days", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
    Text("1 to 90", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, date: LocalDate, onChange: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { open = true }.padding(vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(date.format(dateLabel), style = MonoSmall, color = MaterialTheme.colorScheme.onSurface)
    }

    if (open) {
        // The picker speaks UTC-midnight millis; converting at UTC keeps the date exact.
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}
