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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
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
import dev.zakyymuh.timebox.core.collection.CrossOutcome
import dev.zakyymuh.timebox.core.collection.Dex
import dev.zakyymuh.timebox.core.collection.DexEntry
import dev.zakyymuh.timebox.ui.collection.DexPhoto
import dev.zakyymuh.timebox.ui.common.AddButton
import dev.zakyymuh.timebox.ui.common.AddButtonClearance
import dev.zakyymuh.timebox.ui.common.DateField
import dev.zakyymuh.timebox.ui.common.Pill
import dev.zakyymuh.timebox.ui.plants.Choice
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
            Spacer(Modifier.width(8.dp))
            OutcomePill(cross.outcome)
        }
    }
}

/** Waiting is plain; a success is green, a failure red. */
@Composable
private fun OutcomePill(outcome: CrossOutcome) {
    when (outcome) {
        CrossOutcome.Waiting -> Pill(outcome.label, color = Fresh.Page, textColor = Fresh.InkSoft)
        CrossOutcome.Success -> Pill("✓ ${outcome.label}", color = Fresh.Pastel, textColor = Fresh.GreenDeep)
        CrossOutcome.Failed -> Pill("✕ ${outcome.label}", color = Fresh.Error.copy(alpha = 0.12f), textColor = Fresh.Error)
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
    // The saved cross, while its animation plays; then the screen moves on to it.
    var born by rememberSaveable { mutableStateOf<Long?>(null) }

    Box(Modifier.fillMaxSize()) {
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
                onClick = { viewModel.save { born = it } },
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
    born?.let { id -> BreedAnimation(onDone = { onSaved(id) }) }
    }
}

/**
 * Both parents as one photo: the seed parent fills the left half and the pollen parent the
 * right, touching with no gap, the outer corners rounded. A heart sits on the seam. Each
 * half is tappable when [onMother]/[onFather] are given. Names sit under their half.
 */
@Composable
private fun ParentPair(
    mother: DexEntry?,
    father: DexEntry?,
    onMother: (() -> Unit)?,
    onFather: (() -> Unit)?,
    motherName: String? = null,
    fatherName: String? = null,
) {
    val corner = 28.dp
    Column {
        Box(Modifier.fillMaxWidth().aspectRatio(1.2f), contentAlignment = Alignment.Center) {
            Row(Modifier.matchParentSize()) {
                ParentHalf(mother, onMother, RoundedCornerShape(topStart = corner, bottomStart = corner), Modifier.weight(1f))
                ParentHalf(father, onFather, RoundedCornerShape(topEnd = corner, bottomEnd = corner), Modifier.weight(1f))
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.White),
            ) {
                Text("❤️", fontSize = 22.sp)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            ParentName(SEED, mother, motherName, Alignment.Start, Modifier.weight(1f))
            Spacer(Modifier.width(36.dp))
            ParentName(POLLEN, father, fatherName, Alignment.End, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ParentHalf(entry: DexEntry?, onClick: (() -> Unit)?, shape: RoundedCornerShape, modifier: Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(Fresh.Card)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
    ) {
        if (entry != null) {
            DexPhoto(entry.photo, Modifier.matchParentSize(), side = 480, emojiSize = 40.sp)
        } else if (onClick != null) {
            Text("+", style = MaterialTheme.typography.headlineLarge, color = Fresh.InkSoft)
        } else {
            // A parent that has left the collection: only its name is kept.
            Text("🪴", fontSize = 36.sp)
        }
    }
}

@Composable
private fun ParentName(role: String, entry: DexEntry?, fallbackName: String?, align: Alignment.Horizontal, modifier: Modifier) {
    Column(modifier, horizontalAlignment = align) {
        Text(role, style = Small, color = Fresh.InkSoft)
        Text(
            entry?.name ?: fallbackName ?: "Pick one",
            style = MaterialTheme.typography.titleMedium,
            color = if (entry != null || fallbackName != null) Fresh.Ink else Fresh.InkSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        entry?.let { Text(Dex.label(it.number), style = Small, color = Fresh.InkSoft) }
    }
}

/**
 * One cross: its code big, its name and date, whether it took, and both parents' photos.
 * A parent opens its page. Remove asks first.
 */
@Composable
fun BreedingDetailScreen(viewModel: BreedingDetailViewModel, onBack: () -> Unit, onOpenPlant: (Long) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmRemove by rememberSaveable { mutableStateOf(false) }
    // Removed (or never there): nothing left to show.
    LaunchedEffect(state) { if (state.loaded && state.detail == null) onBack() }

    Scaffold(containerColor = Fresh.Page) { insets ->
        Column(
            Modifier.padding(insets).padding(horizontal = 20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            val d = state.detail
            TopBar(title = "Cross", onBack = onBack) {
                if (d != null) {
                    Pill("Remove", color = Fresh.Card, textColor = Fresh.Error, modifier = Modifier.clip(CircleShape).clickable { confirmRemove = true })
                }
            }
            if (d == null) return@Column
            SoftCard(color = Fresh.Pastel, corner = 28.dp) {
                Text("ID", style = Label, color = Fresh.GreenDeep.copy(alpha = 0.75f))
                Text(d.cross.code, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Fresh.GreenDeep)
                Spacer(Modifier.height(4.dp))
                Text(d.cross.name, style = MaterialTheme.typography.titleLarge, color = Fresh.GreenDeep)
                Text("Crossed on ${Breeding.date(d.cross.date)}", style = Label, color = Fresh.GreenDeep.copy(alpha = 0.75f))
            }
            Spacer(Modifier.height(16.dp))
            Text("Did it take?", style = Label, color = Fresh.InkSoft, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CrossOutcome.entries.forEach { outcome ->
                    Choice(outcome.label, selected = d.cross.outcome == outcome) { viewModel.setOutcome(outcome) }
                }
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

    val shown = state.detail
    if (confirmRemove && shown != null) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            containerColor = Fresh.Page,
            title = { Text("Remove ${shown.cross.code}?") },
            text = { Text("${shown.cross.name} leaves your breeding list. Its code is not given to another cross.") },
            confirmButton = {
                TextButton(onClick = { confirmRemove = false; viewModel.remove() }) { Text("Remove", color = Fresh.Error) }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Cancel") } },
        )
    }
}
