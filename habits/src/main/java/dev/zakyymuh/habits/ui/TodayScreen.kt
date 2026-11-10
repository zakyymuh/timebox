package dev.zakyymuh.habits.ui

import androidx.compose.foundation.background
import dev.zakyymuh.timebox.ui.lastdone.FunFactCard
import dev.zakyymuh.timebox.ui.lastdone.LastTimeSheet
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.key
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.FabPosition
import androidx.compose.material3.ExperimentalMaterial3Api
import dev.zakyymuh.timebox.ui.common.RoundButton
import dev.zakyymuh.timebox.ui.theme.Fresh
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.timebox.core.DayList
import dev.zakyymuh.timebox.core.Motivation
import dev.zakyymuh.timebox.ui.theme.Label
import dev.zakyymuh.timebox.ui.theme.Small
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dayHeading = DateTimeFormatter.ofPattern("EEE d MMM")
private val fullHeading = DateTimeFormatter.ofPattern("EEEE d MMMM")

@Composable
fun TodayScreen(viewModel: TodayViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lastDone by viewModel.lastDone.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    var lastTimeOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Fresh.Page,
        // One round + in the middle instead of a typing box: tap it to write.
        floatingActionButton = {
            if (state.editable) {
                // Glows only on an empty morning, inviting the first tap.
                GlowingAddButton(glowing = state.isEmpty && state.date != null, onClick = { adding = true })
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .fillMaxSize(),
        ) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Header(state, viewModel)

                Spacer(Modifier.height(20.dp))

                // Only on today: the record of a past day is about that day, not about now.
                if (state.isToday && state.date != null) {
                    FunFactCard(
                        fact = lastDone.fact,
                        tracked = lastDone.events.size,
                        onShuffle = viewModel::shuffleFact,
                        onOpen = { lastTimeOpen = true },
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            if (state.isEmpty) {
                EmptyDay(state, viewModel, Modifier.weight(1f))
            } else {
                // A fresh set of cards per day, so stepping through the record starts each
                // day on its own first card.
                key(state.date) {
                    TaskCards(
                        items = state.items,
                        editable = state.editable,
                        onStart = viewModel::startWorking,
                        onDone = viewModel::finish,
                        // Room under the cards for the round + button.
                        modifier = Modifier.weight(1f).padding(bottom = if (state.editable) 96.dp else 0.dp),
                    )
                }
            }

            if (!state.editable) Box(Modifier.padding(horizontal = 20.dp)) { ReadOnlyFooter() }
        }
    }

    if (lastTimeOpen) {
        LastTimeSheet(
            ui = lastDone,
            onDidIt = viewModel::markDone,
            onUndo = viewModel::undoLastMark,
            onDismiss = { lastTimeOpen = false },
        )
    }

    if (adding && state.editable) {
        AddSheet(
            draft = state.draft,
            added = state.items.size,
            onDraftChanged = viewModel::onDraftChanged,
            onIcon = viewModel::onDraftIcon,
            onDescription = viewModel::onDraftDescription,
            onAdd = viewModel::addDraft,
            onDismiss = { adding = false },
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
                style = MaterialTheme.typography.headlineLarge,
                color = Fresh.Ink,
            )
            Text(
                text = when {
                    state.date == null -> ""
                    state.summary.isEmpty -> state.date.format(fullHeading)
                    else -> "${state.date.format(dayHeading)}  ·  ${state.summary.done} of ${state.summary.total} done"
                },
                style = Label,
                modifier = Modifier.padding(top = 4.dp),
                color = Fresh.InkSoft,
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
            style = Small,
            color = Fresh.Green,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable { viewModel.jumpToToday() },
        )
    }
}

@Composable
private fun StepButton(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    RoundButton(
        glyph = glyph,
        onClick = { if (enabled) onClick() },
        size = 40.dp,
        modifier = Modifier.alpha(if (enabled) 1f else 0.3f),
    )
}

@Composable
private fun EmptyDay(
    state: DayUiState,
    viewModel: TodayViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 36.dp).padding(bottom = 96.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!state.isToday || state.date == null) {
            Text(
                text = "Nothing was written this day.",
                style = MaterialTheme.typography.titleMedium,
                color = Fresh.InkSoft,
            )
        }
        if (state.isToday && state.date != null) {
            // A quiet line instead of a blank page; a different one each day.
            Text(
                text = Motivation.forDay(state.date),
                style = MaterialTheme.typography.headlineSmall,
                color = Fresh.InkSoft.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tap + to begin.",
                style = MaterialTheme.typography.bodyMedium,
                color = Fresh.InkSoft.copy(alpha = 0.6f),
            )
            if (state.templates.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Copy a recent day",
                    style = Label,
                    color = Fresh.InkSoft,
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
            .clip(CircleShape)
            .background(Fresh.Card)
            .clickable { onUse(day.date) }
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            text = "${day.date.format(dayHeading)} · ${day.items.size}",
            style = Small,
            color = Fresh.InkSoft,
        )
    }
}

/**
 * The popup for writing. The keyboard comes up straight away; Enter adds the item and keeps
 * the popup open for the next one, so a whole morning list is typed in one go. Swipe it down
 * or tap Done when finished.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddSheet(
    draft: Draft,
    added: Int,
    onDraftChanged: (String) -> Unit,
    onIcon: (String) -> Unit,
    onDescription: (String) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Fresh.Page,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Add to today",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Fresh.Ink,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Done",
                    style = MaterialTheme.typography.labelLarge,
                    color = Fresh.GreenDeep,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Fresh.Pastel)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Fresh.Card)
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                ) {
                    if (draft.text.isEmpty()) {
                        Text("What do you want to do?", style = MaterialTheme.typography.bodyLarge, color = Fresh.InkSoft)
                    }
                    BasicTextField(
                        value = draft.text,
                        onValueChange = onDraftChanged,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Fresh.Ink),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Fresh.Green),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onAdd() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
                Spacer(Modifier.width(10.dp))
                RoundButton(
                    glyph = "↑",
                    onClick = onAdd,
                    container = if (draft.text.isBlank()) Fresh.Card else Fresh.Green,
                    content = if (draft.text.isBlank()) Fresh.InkSoft else Color.White,
                    size = 52.dp,
                    description = "Add",
                )
            }
            Text(
                text = if (added == 0) "Press enter to add & keep typing" else "$added on today's list · enter to add another",
                style = Small,
                color = Fresh.InkSoft,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            )

            Text("Icon", style = Label, color = Fresh.InkSoft, modifier = Modifier.padding(top = 16.dp, start = 4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                CardIcons.all.forEach { icon ->
                    val picked = icon == draft.icon
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (picked) Fresh.Pastel else Fresh.Card)
                            .border(2.dp, if (picked) Fresh.Green else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { onIcon(icon) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(icon, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            Text(
                "Description (optional)",
                style = Label,
                color = Fresh.InkSoft,
                modifier = Modifier.padding(top = 16.dp, start = 4.dp),
            )
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Fresh.Card)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                if (draft.description.isEmpty()) {
                    Text("Anything to remember about it", style = MaterialTheme.typography.bodyMedium, color = Fresh.InkSoft)
                }
                BasicTextField(
                    value = draft.description,
                    onValueChange = onDescription,
                    minLines = 2,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Fresh.Ink),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Fresh.Green),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

@Composable
private fun ReadOnlyFooter() {
    Text(
        text = "Past record (read-only) · Yesterday is not a backlog",
        style = Small,
        color = Fresh.InkSoft,
        modifier = Modifier.padding(vertical = 14.dp),
    )
}
