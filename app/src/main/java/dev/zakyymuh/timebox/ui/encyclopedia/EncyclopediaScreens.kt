package dev.zakyymuh.timebox.ui.encyclopedia

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.zakyymuh.timebox.core.plants.Encyclopedia
import dev.zakyymuh.timebox.core.plants.PlantInfo
import dev.zakyymuh.timebox.ui.common.SectionTitle
import dev.zakyymuh.timebox.ui.common.SoftCard
import dev.zakyymuh.timebox.ui.common.TopBar
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import dev.zakyymuh.timebox.ui.theme.Small

/** Every entry, as a card: name, other names, one line. */
@Composable
fun EncyclopediaScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    Page {
        TopBar(title = "Encyclopedia", onBack = onBack)
        // Plants have name keywords; guides (watering, grafting, soil…) don't.
        val (plants, guides) = Encyclopedia.entries.partition { it.keywords.isNotEmpty() }
        SectionTitle("Plants", Modifier.padding(top = 0.dp))
        EntryCards(plants, onOpen)
        SectionTitle("Guides")
        EntryCards(guides, onOpen)
    }
}

@Composable
private fun EntryCards(entries: List<PlantInfo>, onOpen: (String) -> Unit) {
    run {
        entries.forEach { entry ->
            SoftCard(corner = 20.dp, onClick = { onOpen(entry.id) }, modifier = Modifier.padding(bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(entry.name, style = MaterialTheme.typography.titleLarge, color = Fresh.Ink)
                        Text(
                            entry.otherNames,
                            style = Label,
                            color = Fresh.InkSoft,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Text(
                            entry.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Fresh.Ink,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("→", style = MaterialTheme.typography.titleMedium, color = Fresh.InkSoft)
                }
            }
        }
    }
}

/** One entry: what it is, sun, water, care, forms, fun facts and sources. */
@Composable
fun EntryScreen(id: String, onBack: () -> Unit) {
    val entry = Encyclopedia.byId(id)
    Page {
        TopBar(title = entry?.name ?: "Not found", onBack = onBack)
        if (entry == null) return@Page
        Text(entry.otherNames, style = Label, color = Fresh.InkSoft)
        if (entry.origin != "—") {
            Text("From: ${entry.origin}", style = Label, color = Fresh.InkSoft, modifier = Modifier.padding(top = 2.dp))
        }
        Text(
            entry.description,
            style = MaterialTheme.typography.bodyLarge,
            color = Fresh.Ink,
            modifier = Modifier.padding(top = 14.dp),
        )

        CareCard("☀️  Sun", entry.sun)
        CareCard("💧  Water", entry.water)

        if (entry.tips.isNotEmpty()) {
            SectionTitle("Care tips")
            entry.tips.forEach { Bullet(it) }
        }
        if (entry.varieties.isNotEmpty()) {
            SectionTitle("Forms and varieties")
            entry.varieties.forEach { v ->
                SoftCard(corner = 20.dp, modifier = Modifier.padding(bottom = 10.dp)) {
                    Text(v.name, style = MaterialTheme.typography.titleMedium, color = Fresh.Ink)
                    Text(v.note, style = MaterialTheme.typography.bodyMedium, color = Fresh.Ink, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        SectionTitle("Fun facts")
        entry.funFacts.forEach { Bullet(it, mark = "🌵") }

        Text(
            "Sources: " + entry.sources.joinToString(" · "),
            style = Small,
            color = Fresh.InkSoft,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}

@Composable
private fun CareCard(title: String, text: String) {
    SoftCard(color = Fresh.Pastel, corner = 20.dp, modifier = Modifier.padding(top = 14.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Fresh.GreenDeep)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Fresh.GreenDeep, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun Bullet(text: String, mark: String = "•") {
    Row(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(mark, style = MaterialTheme.typography.bodyMedium, color = Fresh.Green)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Fresh.Ink)
    }
}

@Composable
private fun Page(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Scaffold(containerColor = Fresh.Page) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            content()
            Spacer(Modifier.height(32.dp))
        }
    }
}
