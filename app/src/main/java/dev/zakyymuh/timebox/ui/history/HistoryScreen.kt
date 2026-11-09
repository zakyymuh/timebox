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
import java.time.format.DateTimeFormatter

private val shortDate = DateTimeFormatter.ofPattern("d MMM")

/** Everything that happened, newest first, under date headers, with search at the top. */
@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // The field keeps its own text so typing never waits on the database.
    var text by rememberSaveable { mutableStateOf(state.query) }

    Scaffold(containerColor = Fresh.Page) { insets ->
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
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun EventRow(event: HistoryEvent, showDate: Boolean) {
    val icon = if (event.kind == HistoryKind.Watered) "💧" else "🪴"
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
