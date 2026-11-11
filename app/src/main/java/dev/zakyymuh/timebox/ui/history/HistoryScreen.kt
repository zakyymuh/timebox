package dev.zakyymuh.timebox.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.timebox.core.plants.HistoryEvent
import dev.zakyymuh.timebox.core.plants.HistoryKind
import dev.zakyymuh.timebox.ui.common.SoftCard
import dev.zakyymuh.timebox.ui.common.TopBar
import dev.zakyymuh.timebox.ui.common.softFieldColors
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import dev.zakyymuh.timebox.data.NoteRepository
import dev.zakyymuh.timebox.ui.common.DateField
import dev.zakyymuh.timebox.ui.common.AddButton
import dev.zakyymuh.timebox.ui.common.AddButtonClearance
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val shortDate = DateTimeFormatter.ofPattern("d MMM")

/** Everything that happened, newest first, under date headers, with search at the top. */
@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // The field keeps its own text so typing never waits on the database.
    var text by rememberSaveable { mutableStateOf(state.query) }
    var adding by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = Fresh.Page,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = { AddButton("Add an event") { adding = true } },
    ) { insets ->
        Column(Modifier.padding(insets).fillMaxSize().padding(horizontal = 20.dp)) {
            TopBar(title = "History", onBack = onBack)
            TextField(
                value = text,
                onValueChange = { text = it; viewModel.search(it) },
                placeholder = { Text("Search a plant or event", color = Fresh.InkSoft) },
                leadingIcon = { Text("🔍") },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = softFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            val empty = state.empty
            if (state.loaded && empty != null) {
                Text(empty, style = MaterialTheme.typography.bodyLarge, color = Fresh.InkSoft, modifier = Modifier.padding(top = 24.dp))
            }
            LazyColumn(Modifier.fillMaxSize()) {
                state.groups.forEach { group ->
                    item(key = "h-${group.title}") {
                        Text(
                            group.title,
                            style = Label,
                            color = Fresh.InkSoft,
                            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
                        )
                    }
                    // A month header covers many days, so its rows say which day.
                    val showDate = group.events.map { it.date }.distinct().size > 1
                    items(group.events) { EventRow(it, showDate) }
                }
                item { Spacer(Modifier.height(AddButtonClearance)) }
            }
        }
    }

    if (adding) {
        AddEventDialog(
            today = viewModel.today(),
            onAdd = { note, date -> adding = false; viewModel.addNote(note, date) },
            onDismiss = { adding = false },
        )
    }
}

/** Write down anything for the history: "Repotted the Monstera", on a day (today by default). */
@Composable
private fun AddEventDialog(today: LocalDate, onAdd: (String, LocalDate) -> Unit, onDismiss: () -> Unit) {
    var note by rememberSaveable { mutableStateOf("") }
    var date by remember { mutableStateOf(today) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add an event") },
        text = {
            Column {
                TextField(
                    value = note,
                    onValueChange = { note = it.take(NoteRepository.MAX_TEXT) },
                    placeholder = { Text("e.g. Repotted the Monstera", color = Fresh.InkSoft) },
                    shape = RoundedCornerShape(20.dp),
                    colors = softFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                DateField("When", date) { date = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(note, date) }, enabled = note.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun EventRow(event: HistoryEvent, showDate: Boolean) {
    val icon = when (event.kind) {
        HistoryKind.Watered -> "💧"
        HistoryKind.Joined -> "🪴"
        HistoryKind.Note -> "📝"
    }
    SoftCard(corner = 18.dp, modifier = Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon)
            Spacer(Modifier.width(12.dp))
            Text(
                event.text,
                style = MaterialTheme.typography.bodyLarge,
                color = Fresh.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showDate) {
                Spacer(Modifier.width(8.dp))
                Text(event.date.format(shortDate), style = Label, color = Fresh.InkSoft)
            }
        }
    }
}
