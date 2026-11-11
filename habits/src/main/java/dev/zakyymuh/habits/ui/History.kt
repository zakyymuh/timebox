package dev.zakyymuh.habits.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.zakyymuh.timebox.core.lastdone.Event
import dev.zakyymuh.timebox.core.lastdone.LastDone
import dev.zakyymuh.timebox.ui.common.RoundButton
import dev.zakyymuh.timebox.ui.lastdone.LastDoneUi
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label

/**
 * Everything logged as "I did this", the longest ago first, opened from the ✨ card. Each row
 * can be done again; the floating + logs something new. × or Back closes it.
 */
@Composable
fun HistoryScreen(
    ui: LastDoneUi,
    onLog: (String) -> Unit,
    onClose: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var logging by remember { mutableStateOf(false) }
    BackHandler(onBack = onClose)

    Box(
        Modifier
            .fillMaxSize()
            .background(Fresh.Page)
            .systemBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("History", style = MaterialTheme.typography.headlineLarge, color = Fresh.Ink)
                    Text(
                        "Things you've done, and how long ago",
                        style = Label,
                        color = Fresh.InkSoft,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                RoundButton(glyph = "×", onClick = onClose, size = 40.dp, description = "Close")
            }

            if (ui.events.isEmpty()) {
                Text(
                    "Nothing logged yet. Tap + when you've just done something, like laundry or a haircut.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Fresh.InkSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 80.dp, start = 16.dp, end = 16.dp),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    // Room under the last row for the round + button.
                    contentPadding = PaddingValues(top = 20.dp, bottom = 110.dp),
                ) {
                    items(ui.events, key = { it.key }) { event ->
                        HistoryRow(event, ui) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLog(event.name)
                        }
                    }
                }
            }
        }

        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
            GlowingAddButton(glowing = false, onClick = { logging = true })
        }
    }

    if (logging) {
        LogDialog(
            onLog = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onLog(it)
                logging = false
            },
            onDismiss = { logging = false },
        )
    }
}

@Composable
private fun HistoryRow(event: Event, ui: LastDoneUi, onAgain: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Fresh.Card)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(event.name, style = MaterialTheme.typography.titleMedium, color = Fresh.Ink)
            Text(
                text = ui.today?.let { "Last time ${LastDone.ago(event.lastDay, it)}" }.orEmpty(),
                style = Label,
                color = Fresh.InkSoft,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            "Did it again",
            style = Label,
            color = Fresh.GreenDeep,
            modifier = Modifier
                .clip(CircleShape)
                .background(Fresh.Pastel)
                .clickable(onClick = onAgain)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

/** "What did you just do?" — logs it as done now. × or a tap outside closes it. */
@Composable
private fun LogDialog(onLog: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    fun submit() {
        if (name.isNotBlank()) onLog(name.trim())
    }

    Dialog(onDismissRequest = onDismiss) {
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
            Text(
                "What did you just do?",
                style = MaterialTheme.typography.headlineSmall,
                color = Fresh.Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Fresh.Card)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                if (name.isEmpty()) {
                    Text("Laundry, a haircut…", style = MaterialTheme.typography.bodyLarge, color = Fresh.InkSoft)
                }
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Fresh.Ink),
                    cursorBrush = SolidColor(Fresh.Green),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
            val can = name.isNotBlank()
            Box(
                modifier = Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(if (can) Fresh.Green else Fresh.Card)
                    .clickable(enabled = can) { submit() }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Log it", style = MaterialTheme.typography.labelLarge, color = if (can) Color.White else Fresh.InkSoft)
            }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}
