package dev.zakyymuh.timebox.ui.today

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.timebox.core.DayList
import dev.zakyymuh.timebox.core.TodoItem
import dev.zakyymuh.timebox.ui.theme.MonoLabel
import dev.zakyymuh.timebox.ui.theme.MonoSmall
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dayHeading = DateTimeFormatter.ofPattern("EEE d MMM")
private val fullHeading = DateTimeFormatter.ofPattern("EEEE, MMMM d")

@Composable
fun TodayScreen(viewModel: TodayViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .padding(horizontal = 22.dp)
                .fillMaxSize(),
        ) {
            Masthead()
            Header(state, viewModel)

            Spacer(Modifier.height(14.dp))

            NoteField(
                note = state.note,
                editable = state.editable,
                onNoteChanged = viewModel::onNoteChanged,
            )

            Spacer(Modifier.height(16.dp))

            if (state.isEmpty) {
                EmptyDay(state, viewModel)
            } else {
                Items(state, viewModel)
            }

            if (state.editable) {
                AddItemField(
                    draft = state.draft,
                    onDraftChanged = viewModel::onDraftChanged,
                    onAdd = viewModel::addDraft,
                )
            } else {
                ReadOnlyFooter()
            }

            // The real reminder fires at 06:30, which cannot verify anything today.
            // Remove once the notification has been seen working on a device.
            Text(
                text = "Send the morning reminder now (test)",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .clickable { viewModel.testReminder() },
            )
        }
    }
}

@Composable
private fun Masthead() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "ONE DAY AT A TIME",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(3.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "OFFLINE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun Header(state: DayUiState, viewModel: TodayViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = if (state.isToday) "Today" else "Record",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = when {
                    state.date == null -> ""
                    state.summary.isEmpty -> state.date.format(fullHeading)
                    else -> "${state.date.format(dayHeading)}  ·  ${state.summary.done} of ${state.summary.total} done"
                },
                style = MonoLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Stepping back through the record, and a way straight home again.
        StepButton("‹", enabled = state.hasEarlier, onClick = viewModel::goEarlier)
        Spacer(Modifier.width(6.dp))
        StepButton("›", enabled = state.hasLater, onClick = viewModel::goLater)
    }

    if (!state.isToday) {
        Text(
            text = "Jump to Today",
            style = MonoSmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable { viewModel.jumpToToday() },
        )
    }
}

@Composable
private fun StepButton(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .alpha(if (enabled) 1f else 0.3f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoteField(note: String, editable: Boolean, onNoteChanged: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (note.isEmpty()) {
            Text(
                text = if (editable) "What is today for?" else "No note for this day",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (editable) {
            BasicTextField(
                value = note,
                onValueChange = onNoteChanged,
                textStyle = LocalTextStyle.current.merge(
                    MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(
                    MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        } else if (note.isNotEmpty()) {
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun EmptyDay(state: DayUiState, viewModel: TodayViewModel) {
    Column(Modifier.weight(1f)) {
        Text(
            text = if (state.isToday) "Clean slate for today." else "Nothing was written this day.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (state.isToday) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Write what you want to get done, or copy a recent day in one tap.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.templates.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "COPY A RECENT DAY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.templates.forEach { TemplateChip(it, viewModel::useTemplate) }
                }
            }
        }
    }
}

@Composable
private fun TemplateChip(day: DayList, onUse: (LocalDate) -> Unit) {
    Row(
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp))
            .clickable { onUse(day.date) }
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text = "${day.date.format(dayHeading)} · ${day.items.size}",
            style = MonoSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Items(state: DayUiState, viewModel: TodayViewModel) {
    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
        items(state.items, key = { it.id }) { item ->
            ItemRow(
                item = item,
                editable = state.editable,
                onToggle = { viewModel.toggle(item.id) },
                onRemove = { viewModel.remove(item.id) },
            )
        }
    }
}

@Composable
private fun ItemRow(
    item: TodoItem,
    editable: Boolean,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A hand-drawn circle rather than a Material checkbox: the design is paper, and
        // a filled square is the loudest thing Material owns.
        Box(
            modifier = Modifier
                .size(19.dp)
                .border(
                    1.5.dp,
                    if (item.isDone) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    CircleShape,
                )
                .clickable(enabled = editable) {
                    // Haptic rather than the prototype's sound: on Android a tick you
                    // can feel is the convention, and silent.
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                },
            contentAlignment = Alignment.Center,
        ) {
            if (item.isDone) {
                Text(
                    text = "✓",
                    style = MonoSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        Spacer(Modifier.width(13.dp))

        Text(
            text = buildAnnotatedString {
                if (item.isDone) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(item.text)
                    }
                } else {
                    append(item.text)
                }
            },
            style = MaterialTheme.typography.bodyLarge,
            color = if (item.isDone) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
        )

        if (editable) {
            Text(
                text = "×",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .alpha(0.6f)
                    .clickable { onRemove() }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun AddItemField(draft: String, onDraftChanged: (String) -> Unit, onAdd: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp),
        ) {
            if (draft.isEmpty()) {
                Text(
                    text = "Add something to do",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = draft,
                onValueChange = onDraftChanged,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(
                    MaterialTheme.colorScheme.onSurface,
                ),
                // Enter adds and keeps the keyboard up, so a whole list can be typed
                // without reaching for the screen between lines.
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAdd() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = "Press enter to add & keep typing",
            style = MonoSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp),
        )
    }
}

@Composable
private fun ReadOnlyFooter() {
    Text(
        text = "Past record (read-only) · Yesterday is not a backlog",
        style = MonoSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 14.dp),
    )
}
