package dev.zakyymuh.habits.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.zakyymuh.timebox.core.lastdone.Event
import dev.zakyymuh.timebox.core.lastdone.LastDone
import dev.zakyymuh.timebox.ui.common.RoundButton
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import dev.zakyymuh.timebox.ui.theme.Small
import java.time.LocalDate

/**
 * A random line about something logged in "Last time" — "Your last “laundry” was 7 days
 * ago." Tap the line for another; tap "Last time ›" to log or look things up.
 */
@Composable
fun FunFactCard(
    fact: String?,
    tracked: Int,
    onShuffle: () -> Unit,
    onOpen: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Fresh.Card)
            .clickable { if (fact == null) onOpen() else onShuffle() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (fact == null) "Last time" else "Fun fact  ·  tap for another",
                style = Label,
                color = Fresh.InkSoft,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (tracked == 0) "Start ›" else "Last time · $tracked ›",
                style = Label,
                color = Fresh.GreenDeep,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Fresh.Pastel)
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = fact ?: "Keep track of when you last did things — laundry, a haircut, new bedsheets.",
            style = MaterialTheme.typography.bodyLarge,
            color = if (fact == null) Fresh.InkSoft else Fresh.Ink,
        )
    }
}

/**
 * Everything logged, the longest ago first. Type a name and press enter to log it as done
 * now; "Did it" logs a known one again; ↶ takes back a mistaken tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastTimeSheet(
    ui: LastDoneUi,
    onDidIt: (String) -> Unit,
    onUndo: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    val haptics = LocalHapticFeedback.current

    fun log(name: String) {
        if (name.isBlank()) return
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onDidIt(name)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Fresh.Page,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Last time",
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
                        Text("What did you just do?", style = MaterialTheme.typography.bodyLarge, color = Fresh.InkSoft)
                    }
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Fresh.Ink),
                        cursorBrush = SolidColor(Fresh.Green),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { log(draft); draft = "" }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.width(10.dp))
                RoundButton(
                    glyph = "✓",
                    onClick = { log(draft); draft = "" },
                    container = if (draft.isBlank()) Fresh.Card else Fresh.Green,
                    content = if (draft.isBlank()) Fresh.InkSoft else Color.White,
                    size = 52.dp,
                    description = "Log as done now",
                )
            }
            Text(
                text = "Logged as done just now",
                style = Small,
                color = Fresh.InkSoft,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            )

            Spacer(Modifier.height(16.dp))

            if (ui.events.isEmpty() || ui.today == null) {
                Text(
                    "Nothing logged yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Fresh.InkSoft,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(ui.events, key = { it.key }) { event ->
                        EventRow(event, ui.today, onDidIt = { log(event.name) }, onUndo = { onUndo(event.key) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: Event, today: LocalDate, onDidIt: () -> Unit, onUndo: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Fresh.Card)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(event.name, style = MaterialTheme.typography.bodyLarge, color = Fresh.Ink)
            val usually = event.averageGapDays?.takeIf { it > 0 }?.let { "  ·  usually every $it ${if (it == 1) "day" else "days"}" }.orEmpty()
            Text(
                text = LastDone.ago(event.lastDay, today).replaceFirstChar { it.uppercase() } + usually,
                style = Small,
                color = Fresh.InkSoft,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = "↶",
            style = MaterialTheme.typography.titleMedium,
            color = Fresh.InkSoft,
            modifier = Modifier
                .alpha(0.6f)
                .clip(CircleShape)
                .clickable(onClick = onUndo)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
        Text(
            text = "Did it",
            style = Label,
            color = Fresh.GreenDeep,
            modifier = Modifier
                .clip(CircleShape)
                .background(Fresh.Pastel)
                .clickable(onClick = onDidIt)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
