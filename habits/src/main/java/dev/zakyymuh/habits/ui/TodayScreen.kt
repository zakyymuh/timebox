package dev.zakyymuh.habits.ui

import androidx.compose.foundation.background
import dev.zakyymuh.timebox.ui.lastdone.FunFactCard
import dev.zakyymuh.timebox.ui.lastdone.LastTimeSheet
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
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
import androidx.compose.material3.FabPosition
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
    // The card being worked on, or -1. While there is one, the whole app is locked to it.
    val inHand = state.items.indexOfFirst { it.isWorking }

    // "Working on it" locks the app to that card: everything around it turns as dark as the
    // card, nothing else answers a tap, and back does nothing, until Done.
    val locked = state.editable && inHand >= 0
    val page by animateColorAsState(if (locked) Fresh.Ink else Fresh.Page, tween(450), label = "page")
    val factFade by animateFloatAsState(if (locked) 0.2f else 1f, tween(450), label = "fact")
    BackHandler(enabled = locked) { /* stay here until Done */ }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = page,
        // One round + in the middle instead of a typing box: tap it to write.
        floatingActionButton = {
            if (state.editable && inHand < 0) {
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
            Column(Modifier.padding(horizontal = 20.dp).blockTouches(locked)) {
                Header(state, viewModel, locked)

                Spacer(Modifier.height(20.dp))

                // Only on today: the record of a past day is about that day, not about now.
                if (state.isToday && state.date != null) {
                    Box(Modifier.alpha(factFade)) {
                        FunFactCard(
                            fact = lastDone.fact,
                            tracked = lastDone.events.size,
                            onShuffle = viewModel::shuffleFact,
                            onOpen = { lastTimeOpen = true },
                        )
                    }
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
                        cards = state.items,
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

    if (adding && state.editable && inHand < 0) {
        AddDialog(
            draft = state.draft,
            onDraftChanged = viewModel::onDraftChanged,
            onIcon = viewModel::onDraftIcon,
            onDescription = viewModel::onDraftDescription,
            onAdd = viewModel::addDraft,
            onDismiss = { adding = false },
        )
    }
}

@Composable
private fun Header(state: DayUiState, viewModel: TodayViewModel, locked: Boolean) {
    val title by animateColorAsState(if (locked) Color.White else Fresh.Ink, tween(450), label = "title")
    val sub by animateColorAsState(
        if (locked) Color.White.copy(alpha = 0.55f) else Fresh.InkSoft, tween(450), label = "sub",
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = if (state.isToday) "Today" else "Record",
                style = MaterialTheme.typography.headlineLarge,
                color = title,
            )
            Text(
                text = when {
                    state.date == null -> ""
                    state.summary.isEmpty -> state.date.format(fullHeading)
                    else -> "${state.date.format(dayHeading)}  ·  ${state.summary.done} of ${state.summary.total} done"
                },
                style = Label,
                modifier = Modifier.padding(top = 4.dp),
                color = sub,
            )
        }

        // Stepping back through the record, and a way straight home again.
        StepButton("‹", enabled = state.hasEarlier, locked = locked, onClick = viewModel::goEarlier)
        Spacer(Modifier.width(6.dp))
        StepButton("›", enabled = state.hasLater, locked = locked, onClick = viewModel::goLater)
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
private fun StepButton(glyph: String, enabled: Boolean, locked: Boolean, onClick: () -> Unit) {
    RoundButton(
        glyph = glyph,
        onClick = { if (enabled) onClick() },
        size = 40.dp,
        modifier = Modifier.alpha(
            when {
                locked -> 0.15f
                enabled -> 1f
                else -> 0.3f
            },
        ),
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
 * The popup for writing, in the middle of the screen. Add saves the card, says so for a
 * moment, then comes back empty for the next one. The × at the top right closes it, and
 * so does a tap outside the popup.
 */
@Composable
private fun AddDialog(
    draft: Draft,
    onDraftChanged: (String) -> Unit,
    onIcon: (String) -> Unit,
    onDescription: (String) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val focus = remember { FocusRequester() }
    var ready by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Fresh.Page)
                .padding(20.dp),
        ) {
            Box(Modifier.fillMaxWidth()) {
                Text(
                    "×",
                    style = MaterialTheme.typography.titleLarge,
                    color = Fresh.InkSoft,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                        .semantics { contentDescription = "Close" }
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                )
            }

            if (ready) {
                ReadyToGo()
            } else {
                Text(
                    "What is your idea?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Fresh.Ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                // One line of icons; swipe it sideways for more.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
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

                Field(
                    value = draft.text,
                    onValueChange = onDraftChanged,
                    placeholder = "What do you want to do?",
                    singleLine = true,
                    modifier = Modifier.padding(top = 14.dp).focusRequester(focus),
                )
                Field(
                    value = draft.description,
                    onValueChange = onDescription,
                    placeholder = "Description (optional)",
                    singleLine = false,
                    modifier = Modifier.padding(top = 10.dp),
                )

                val canAdd = draft.text.isNotBlank()
                Box(
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(if (canAdd) Fresh.Green else Fresh.Card)
                        .clickable(enabled = canAdd) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAdd()
                            ready = true
                        }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Add", style = MaterialTheme.typography.labelLarge, color = if (canAdd) Color.White else Fresh.InkSoft)
                }
            }
        }
    }

    LaunchedEffect(ready) {
        if (ready) {
            delay(1_200)
            ready = false
        } else {
            focus.requestFocus()
        }
    }
}

@Composable
private fun ReadyToGo() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(Fresh.Pastel),
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", style = MaterialTheme.typography.headlineMedium, color = Fresh.GreenDeep)
        }
        Text(
            "Your task is ready to go",
            style = MaterialTheme.typography.titleMedium,
            color = Fresh.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Fresh.Card)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = Fresh.InkSoft)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 2,
            maxLines = if (singleLine) 1 else 4,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Fresh.Ink),
            cursorBrush = SolidColor(Fresh.Green),
            keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Next else ImeAction.Default),
            modifier = Modifier.fillMaxWidth(),
        )
    }
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
