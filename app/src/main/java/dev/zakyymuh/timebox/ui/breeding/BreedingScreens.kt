package dev.zakyymuh.timebox.ui.breeding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.timebox.core.collection.Breeding
import dev.zakyymuh.timebox.core.collection.BreedingForm
import dev.zakyymuh.timebox.core.collection.Cross
import dev.zakyymuh.timebox.core.collection.Dex
import dev.zakyymuh.timebox.core.collection.DexEntry
import dev.zakyymuh.timebox.ui.collection.DexPhoto
import dev.zakyymuh.timebox.ui.common.AddButton
import dev.zakyymuh.timebox.ui.common.AddButtonClearance
import dev.zakyymuh.timebox.ui.common.DateField
import dev.zakyymuh.timebox.ui.common.SoftCard
import dev.zakyymuh.timebox.ui.common.TopBar
import dev.zakyymuh.timebox.ui.common.softFieldColors
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import dev.zakyymuh.timebox.ui.theme.Small

private const val SEED = "Seed parent"
private const val POLLEN = "Pollen parent"

/** Every cross, newest first: its name, code and date. */
@Composable
fun BreedingListScreen(viewModel: BreedingListViewModel, onBack: () -> Unit, onOpen: (Long) -> Unit, onAdd: () -> Unit) {
    val crosses by viewModel.crosses.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = Fresh.Page,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = { AddButton("New cross", onAdd) },
    ) { insets ->
        Column(Modifier.padding(insets).fillMaxSize().padding(horizontal = 20.dp)) {
            TopBar(title = "Breeding", onBack = onBack)
            val list = crosses ?: return@Column
            if (list.isEmpty()) {
                Text("No crosses yet", style = MaterialTheme.typography.headlineSmall, color = Fresh.InkSoft.copy(alpha = 0.75f))
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tap New cross below to pick two parents from your collection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Fresh.InkSoft.copy(alpha = 0.6f),
                )
                return@Column
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { CrossRow(it) { onOpen(it.id) } }
                item { Spacer(Modifier.height(AddButtonClearance)) }
            }
        }
    }
}

@Composable
private fun CrossRow(cross: Cross, onClick: () -> Unit) {
    SoftCard(corner = 24.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("❤️", fontSize = 20.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(cross.name, style = MaterialTheme.typography.titleMedium, color = Fresh.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${cross.code} · ${Breeding.date(cross.date)}", style = Label, color = Fresh.InkSoft)
            }
            Text("→", style = MaterialTheme.typography.titleMedium, color = Fresh.InkSoft)
        }
    }
}

/** Pick two parents from the collection; the name fills itself in; pick the day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedingNewScreen(viewModel: BreedingNewViewModel, onBack: () -> Unit, onSaved: (Long) -> Unit) {
    val parents by viewModel.parents.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val date by viewModel.date.collectAsStateWithLifecycle()
    val mother by viewModel.mother.collectAsStateWithLifecycle()
    val father by viewModel.father.collectAsStateWithLifecycle()
    // Which slot the plant list is open for: SEED, POLLEN, or none.
    var picking by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(containerColor = Fresh.Page) { insets ->
        Column(
            Modifier.padding(insets).padding(horizontal = 20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            TopBar(title = "New cross", onBack = onBack)
            val owned = parents ?: return@Column
            if (owned.size < 2) {
                Text(
                    "A cross needs two plants in your collection. Add another one first.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Fresh.InkSoft,
                )
                return@Column
            }
            ParentPair(
                mother = mother,
                father = father,
                onMother = { picking = SEED },
                onFather = { picking = POLLEN },
            )
            Spacer(Modifier.height(20.dp))
            Text("Name", style = Label, color = Fresh.InkSoft, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            TextField(
                value = form.name,
                onValueChange = viewModel::rename,
                placeholder = { Text("Fills in when both parents are picked", color = Fresh.InkSoft) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = softFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            DateField("Crossed on", date, viewModel::setDate)
            Spacer(Modifier.height(10.dp))
            Text(
                "Its code will be ${Breeding.code(date, emptyList()).take(4)}··· (month, year, then the count)",
                style = Small,
                color = Fresh.InkSoft,
                modifier = Modifier.padding(start = 4.dp),
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { viewModel.save(onSaved) },
                enabled = form.isValid,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Fresh.Ink, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text("Save cross", style = MaterialTheme.typography.labelLarge)
            }
            if (form.motherId != null && form.motherId == form.fatherId) {
                Text(
                    "Pick two different plants",
                    style = Small,
                    color = Fresh.Error,
                    modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally),
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    picking?.let { slot ->
        ModalBottomSheet(onDismissRequest = { picking = null }, containerColor = Fresh.Page) {
            Text(
                "Choose the ${slot.lowercase()}",
                style = MaterialTheme.typography.titleMedium,
                color = Fresh.Ink,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            LazyColumn(Modifier.padding(bottom = 24.dp)) {
                items(parents.orEmpty(), key = { it.id }) { entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (slot == SEED) viewModel.pickMother(entry) else viewModel.pickFather(entry)
                                picking = null
                            }
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                    ) {
                        DexPhoto(entry.photo, Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)), side = 160, emojiSize = 20.sp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(Dex.label(entry.number), style = Small, color = Fresh.InkSoft)
                            Text(entry.name, style = MaterialTheme.typography.titleMedium, color = Fresh.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

/** The two parents side by side with a heart between. Each slot is tappable when [onMother]/[onFather] are given. */
@Composable
private fun ParentPair(
    mother: DexEntry?,
    father: DexEntry?,
    onMother: (() -> Unit)?,
    onFather: (() -> Unit)?,
    motherName: String? = null,
    fatherName: String? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ParentSlot(SEED, mother, motherName, onMother, Modifier.weight(1f))
        Text("❤️", fontSize = 22.sp, modifier = Modifier.padding(horizontal = 8.dp))
        ParentSlot(POLLEN, father, fatherName, onFather, Modifier.weight(1f))
    }
}

@Composable
private fun ParentSlot(role: String, entry: DexEntry?, fallbackName: String?, onClick: (() -> Unit)?, modifier: Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Column(modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(Fresh.Card)
                .border(2.dp, Color.White, shape)
                .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        ) {
            if (entry != null) {
                DexPhoto(entry.photo, Modifier.matchParentSize(), side = 480, emojiSize = 40.sp)
            } else if (onClick != null) {
                Text("+", style = MaterialTheme.typography.headlineLarge, color = Fresh.InkSoft)
            } else {
                // A parent that has left the collection: only its name is kept.
                Text("🪴", fontSize = 36.sp, modifier = Modifier.padding(8.dp))
            }
        }
        Text(role, style = Small, color = Fresh.InkSoft, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
        Text(
            entry?.name ?: fallbackName ?: "Pick one",
            style = MaterialTheme.typography.titleMedium,
            color = if (entry != null || fallbackName != null) Fresh.Ink else Fresh.InkSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp),
        )
        entry?.let { Text(Dex.label(it.number), style = Small, color = Fresh.InkSoft, modifier = Modifier.padding(start = 4.dp)) }
    }
}

/** One cross: its code big, its name and date, and both parents' photos. A parent opens its page. */
@Composable
fun BreedingDetailScreen(viewModel: BreedingDetailViewModel, onBack: () -> Unit, onOpenPlant: (Long) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state) { if (state.loaded && state.detail == null) onBack() }

    Scaffold(containerColor = Fresh.Page) { insets ->
        Column(
            Modifier.padding(insets).padding(horizontal = 20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            val d = state.detail
            TopBar(title = "Cross", onBack = onBack)
            if (d == null) return@Column
            SoftCard(color = Fresh.Pastel, corner = 28.dp) {
                Text("ID", style = Label, color = Fresh.GreenDeep.copy(alpha = 0.75f))
                Text(d.cross.code, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Fresh.GreenDeep)
                Spacer(Modifier.height(4.dp))
                Text(d.cross.name, style = MaterialTheme.typography.titleLarge, color = Fresh.GreenDeep)
                Text("Crossed on ${Breeding.date(d.cross.date)}", style = Label, color = Fresh.GreenDeep.copy(alpha = 0.75f))
            }
            Spacer(Modifier.height(20.dp))
            ParentPair(
                mother = d.mother,
                father = d.father,
                onMother = d.mother?.let { m -> { onOpenPlant(m.id) } },
                onFather = d.father?.let { f -> { onOpenPlant(f.id) } },
                motherName = d.cross.motherName,
                fatherName = d.cross.fatherName,
            )
            if (d.mother == null || d.father == null) {
                Text(
                    "A parent is no longer in your collection; its name is kept here.",
                    style = Small,
                    color = Fresh.InkSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
