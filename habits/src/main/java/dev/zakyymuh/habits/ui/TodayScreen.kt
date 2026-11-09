package dev.zakyymuh.habits.ui

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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.FabPosition
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.PaddingValues
import dev.zakyymuh.timebox.ui.common.RoundButton
import dev.zakyymuh.timebox.ui.theme.Fresh
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
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
import dev.zakyymuh.timebox.core.Motivation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import dev.zakyymuh.timebox.core.TodoItem
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
                RoundButton(
                    glyph = "+",
                    onClick = { adding = true },
                    container = Fresh.Ink,
                    content = Color.White,
                    size = 64.dp,
                    description = "Add something to do",
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .padding(horizontal = 20.dp)
                .fillMaxSize(),
        ) {
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

            if (state.isEmpty) {
                EmptyDay(state, viewModel, Modifier.weight(1f))
            } else {
                Items(state, viewModel, Modifier.weight(1f))
            }

            if (!state.editable) ReadOnlyFooter()
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
    Column(modifier) {
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
 * Today's list. On today, hold the ≡ handle and drag a row up or down to put first what
 * you want to do first. Rows swap as you pass them; the new order is saved when you let go.
 */
@Composable
private fun Items(
    state: DayUiState,
    viewModel: TodayViewModel,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    // The order on screen while dragging; reset whenever the saved list changes.
    var order by remember(state.items) { mutableStateOf(state.items) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var fromIndex by remember { mutableIntStateOf(-1) }
    var offset by remember { mutableFloatStateOf(0f) }
    val canDrag = state.editable && order.size > 1

    fun dragBy(dy: Float) {
        val id = draggingId ?: return
        offset += dy
        val visible = listState.layoutInfo.visibleItemsInfo
        val current = visible.firstOrNull { it.key == id } ?: return
        val centre = current.offset + offset + current.size / 2f
        val target = visible.firstOrNull {
            it.key != id && it.key is Long && centre > it.offset && centre < it.offset + it.size
        } ?: return
        val from = order.indexOfFirst { it.id == id }
        val to = order.indexOfFirst { it.id == target.key }
        if (from < 0 || to < 0) return
        order = order.toMutableList().apply { add(to, removeAt(from)) }
        // The row now sits where the target was; keep it under the finger.
        offset += current.offset - target.offset
    }

    fun endDrag() {
        val id = draggingId ?: return
        val to = order.indexOfFirst { it.id == id }
        if (fromIndex >= 0 && to >= 0 && fromIndex != to) viewModel.move(fromIndex, to)
        draggingId = null
        fromIndex = -1
        offset = 0f
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        // Room under the last item for the round + button.
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        items(order, key = { it.id }) { item ->
            val dragging = item.id == draggingId
            ItemRow(
                item = item,
                editable = state.editable,
                onToggle = { viewModel.toggle(item.id) },
                onRemove = { viewModel.remove(item.id) },
                handle = if (!canDrag) null else Modifier.pointerInput(item.id, state.items) {
                    detectDragGestures(
                        onDragStart = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            draggingId = item.id
                            fromIndex = order.indexOfFirst { it.id == item.id }
                            offset = 0f
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragBy(amount.y)
                        },
                        onDragEnd = { endDrag() },
                        onDragCancel = { endDrag() },
                    )
                },
                modifier = if (dragging) {
                    Modifier
                        .zIndex(1f)
                        .graphicsLayer {
                            translationY = offset
                            scaleX = 1.02f
                            scaleY = 1.02f
                        }
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                } else {
                    Modifier.animateItem()
                },
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
    handle: Modifier? = null,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Fresh.Card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A round tick rather than a Material checkbox: white when open, green when done.
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (item.isDone) Fresh.Green else Fresh.Page)
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
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
            }
        }

        Spacer(Modifier.width(14.dp))

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
                Fresh.InkSoft
            } else {
                Fresh.Ink
            },
            modifier = Modifier.weight(1f),
        )

        if (handle != null) {
            Text(
                text = "≡",
                style = MaterialTheme.typography.titleLarge,
                color = Fresh.InkSoft,
                modifier = handle
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }

        if (editable) {
            Text(
                text = "×",
                style = MaterialTheme.typography.titleMedium,
                color = Fresh.InkSoft,
                modifier = Modifier
                    .alpha(0.6f)
                    .clickable { onRemove() }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

/**
 * The popup for writing. The keyboard comes up straight away; Enter adds the item and keeps
 * the popup open for the next one, so a whole morning list is typed in one go. Swipe it down
 * or tap Done when finished.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSheet(
    draft: String,
    added: Int,
    onDraftChanged: (String) -> Unit,
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
                    if (draft.isEmpty()) {
                        Text("What do you want to do?", style = MaterialTheme.typography.bodyLarge, color = Fresh.InkSoft)
                    }
                    BasicTextField(
                        value = draft,
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
                    container = if (draft.isBlank()) Fresh.Card else Fresh.Green,
                    content = if (draft.isBlank()) Fresh.InkSoft else Color.White,
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
