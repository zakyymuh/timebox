package dev.zakyymuh.timebox.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.timebox.core.DayList
import dev.zakyymuh.timebox.core.TodoItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(viewModel: TodayViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(modifier = Modifier.fillMaxSize()) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .padding(horizontal = 20.dp),
        ) {
            Header(state)

            NoteField(
                note = state.note,
                onNoteChanged = viewModel::onNoteChanged,
            )

            AddItemField(
                draft = state.draft,
                onDraftChanged = viewModel::onDraftChanged,
                onAdd = viewModel::addDraft,
            )

            // Templates only earn their space on a morning with nothing written yet.
            if (state.isEmpty && state.templates.isNotEmpty()) {
                Templates(state.templates, viewModel::useTemplate)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (state.isEmpty) {
                Text(
                    text = "Nothing on the list yet. What do you want to get done today?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(state.items, key = { it.id }) { item ->
                        ItemRow(
                            item = item,
                            onToggle = { viewModel.toggle(item.id) },
                            onRemove = { viewModel.remove(item.id) },
                        )
                    }
                }
            }

            // The real reminder fires at 06:30, which is no use for checking it works
            // today. Remove once the notification has been seen on a real device.
            TextButton(onClick = viewModel::testReminder) {
                Text(
                    "Send the morning reminder now (test)",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun Header(state: TodayUiState) {
    Column(modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)) {
        Text(
            text = "Today",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = when {
                // Blank, not a placeholder date, until the database has answered.
                state.date == null -> ""
                state.summary.isEmpty ->
                    state.date.format(DateTimeFormatter.ofPattern("EEEE d MMMM"))
                state.summary.allDone -> "All ${state.summary.total} done"
                else -> "${state.summary.done} of ${state.summary.total} done"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoteField(note: String, onNoteChanged: (String) -> Unit) {
    OutlinedTextField(
        value = note,
        onValueChange = onNoteChanged,
        label = { Text("What is today for?") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        minLines = 2,
        maxLines = 4,
    )
}

@Composable
private fun AddItemField(draft: String, onDraftChanged: (String) -> Unit, onAdd: () -> Unit) {
    OutlinedTextField(
        value = draft,
        onValueChange = onDraftChanged,
        label = { Text("Add something to do") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        // Enter adds and keeps the field focused, so a list can be typed in one go.
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onAdd() }),
        trailingIcon = {
            if (draft.isNotBlank()) {
                TextButton(onClick = onAdd) { Text("Add") }
            }
        },
    )
}

@Composable
private fun Templates(templates: List<DayList>, onUse: (LocalDate) -> Unit) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            text = "Or start from a recent day",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            templates.forEach { day ->
                AssistChip(
                    onClick = { onUse(day.date) },
                    label = {
                        Text(
                            "${day.date.format(DateTimeFormatter.ofPattern("EEE d"))} · ${day.items.size}",
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ItemRow(item: TodoItem, onToggle: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.isDone, onCheckedChange = { onToggle() })
        Text(
            text = item.text,
            style = if (item.isDone) {
                // Struck through and dimmed, so finished things recede without vanishing.
                TextStyle(
                    textDecoration = TextDecoration.LineThrough,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                )
            } else {
                TextStyle(fontSize = MaterialTheme.typography.bodyLarge.fontSize)
            },
            modifier = Modifier.weight(1f),
        )
        // A text glyph rather than Icons.Default.Close, which would pull in
        // material-icons-extended for one character.
        TextButton(onClick = onRemove) {
            Text("\u00d7", style = MaterialTheme.typography.titleLarge)
        }
    }
}
