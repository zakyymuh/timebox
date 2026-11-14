package dev.zakyymuh.timebox.ui.collection

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.timebox.core.collection.Dex
import dev.zakyymuh.timebox.core.collection.DexEntry
import dev.zakyymuh.timebox.core.collection.DexForm
import dev.zakyymuh.timebox.core.collection.DexShot
import dev.zakyymuh.timebox.core.collection.DexSort
import dev.zakyymuh.timebox.core.plants.Encyclopedia
import dev.zakyymuh.timebox.ui.common.AddButton
import dev.zakyymuh.timebox.ui.common.AddButtonClearance
import dev.zakyymuh.timebox.ui.common.Pill
import dev.zakyymuh.timebox.ui.common.RoundButton
import dev.zakyymuh.timebox.ui.common.SoftCard
import dev.zakyymuh.timebox.ui.common.TopBar
import dev.zakyymuh.timebox.ui.plants.Choice
import dev.zakyymuh.timebox.ui.common.DateField
import dev.zakyymuh.timebox.ui.common.softFieldColors
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import dev.zakyymuh.timebox.ui.theme.Small
import java.time.format.DateTimeFormatter

private val sinceFormat = DateTimeFormatter.ofPattern("d MMM yyyy")

/** The dex: two tabs, a grid of numbered cards. Wishes are grey until you get them. */
@Composable
fun CollectionScreen(
    viewModel: CollectionViewModel,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onAdd: (owned: Boolean) -> Unit,
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    // Page 0 is the collection, page 1 the wishlist; swipe or tap a tab to move.
    val pager = rememberPagerState { 2 }
    val wishlist = pager.currentPage == 1
    val scope = rememberCoroutineScope()
    var sort by rememberSaveable { mutableStateOf(DexSort.Date) }
    val all = entries

    Scaffold(
        containerColor = Fresh.Page,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = { AddButton(if (wishlist) "Add a wish" else "Add a plant") { onAdd(!wishlist) } },
    ) { insets ->
        Column(Modifier.padding(insets).fillMaxSize()) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                TopBar(title = "My plants", onBack = onBack) {
                    if (all != null) {
                        // Price is only kept for wishes, so the collection sorts by date or name.
                        val sorts = if (wishlist) DexSort.entries else listOf(DexSort.Date, DexSort.Name)
                        SortMenu(sort = if (sort in sorts) sort else DexSort.Date, sorts = sorts) { sort = it }
                    }
                }
            }
            if (all == null) return@Column
            val owned = all.filter { it.owned }
            val wished = all.filterNot { it.owned }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 20.dp)) {
                Choice("Collection · ${owned.size}", selected = !wishlist) { scope.launch { pager.animateScrollToPage(0) } }
                Choice("Wishlist · ${wished.size}", selected = wishlist) { scope.launch { pager.animateScrollToPage(1) } }
            }
            Spacer(Modifier.height(16.dp))

            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.Top) { page ->
                val isWish = page == 1
                val pageSort = if (!isWish && sort == DexSort.Price) DexSort.Date else sort
                DexGrid(pageSort.apply(if (isWish) wished else owned), isWish, onOpen)
            }
        }
    }
}

/** One page of the collection screen: the grid, or a hint when it is empty. */
@Composable
private fun DexGrid(shown: List<DexEntry>, wishlist: Boolean, onOpen: (Long) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        if (shown.isEmpty()) {
            Text(
                if (wishlist) "Plants you dream of go here." else "Every plant you own, with its photo and number.",
                style = MaterialTheme.typography.headlineSmall,
                color = Fresh.InkSoft.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (wishlist) "Tap Add a wish below to wish for one." else "Tap Add a plant below to add your first.",
                style = MaterialTheme.typography.bodyMedium,
                color = Fresh.InkSoft.copy(alpha = 0.6f),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = AddButtonClearance),
            ) {
                items(shown, key = { it.id }) { DexCard(it) { onOpen(it.id) } }
            }
        }
    }
}

/** "↕ Newest": opens the choice of order. */
@Composable
private fun SortMenu(sort: DexSort, sorts: List<DexSort>, onPick: (DexSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Pill("↕  ${sort.label}", color = Fresh.Card, modifier = Modifier.clip(CircleShape).clickable { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Fresh.Page) {
            sorts.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option.label,
                            color = Fresh.Ink,
                            fontWeight = if (option == sort) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        open = false
                        onPick(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun DexCard(entry: DexEntry, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Fresh.Card)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        DexPhoto(
            entry.photo,
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(18.dp)),
            wished = !entry.owned,
        )
        Column(Modifier.padding(horizontal = 6.dp, vertical = 8.dp)) {
            Text(Dex.label(entry.number), style = Small, color = Fresh.InkSoft)
            Text(
                entry.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (entry.owned) Fresh.Ink else Fresh.InkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.species.isNotBlank()) {
                Text(entry.species, style = Small, color = Fresh.InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!entry.owned) {
                Dex.price(entry.priceLow, entry.priceHigh)?.let {
                    Text(it, style = Small, color = Fresh.GreenDeep, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** One plant: its photo diary (swipe; 📷 adds one with a note), number, when you got it, notes, and its encyclopedia page. */
@Composable
fun DexDetailScreen(
    viewModel: DexDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onOpenPlant: (Long) -> Unit,
    onBreed: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shots by viewModel.shots.collectAsStateWithLifecycle()
    val origin by viewModel.origin.collectAsStateWithLifecycle()
    val children by viewModel.children.collectAsStateWithLifecycle()
    var repotting by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // The picked photos wait here while their note is asked for.
    var picked by remember { mutableStateOf<List<Uri>?>(null) }
    var editing by remember { mutableStateOf<DexShot?>(null) }
    var deleting by remember { mutableStateOf<DexShot?>(null) }
    val pick = rememberPhotosPicker { uris -> picked = uris }
    picked?.let { uris ->
        PhotoNoteDialog(
            title = if (uris.size > 1) "Add a note · ${uris.size} photos" else "Add a note",
            hint = if (uris.size > 1) "It goes on every one; tap a photo later to change its own." else null,
            confirm = if (uris.size > 1) "Save ${uris.size} photos" else "Save photo",
            onSave = { note ->
                picked = null
                viewModel.addPhotos(uris, note) { failed ->
                    if (failed > 0) {
                        val text = if (failed == uris.size) "Couldn't read that photo" else "Couldn't read $failed of the photos"
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { picked = null },
        )
    }
    editing?.let { shot ->
        PhotoNoteDialog(
            initial = shot.note,
            title = if (shot.note.isBlank()) "Add a note" else "Change the note",
            confirm = "Save",
            onSave = { note ->
                editing = null
                viewModel.setNote(shot.id, note)
            },
            onDismiss = { editing = null },
            onShare = state.entry?.let { e ->
                {
                    val subtitle = listOfNotNull(Dex.label(e.number), e.species.takeIf { it.isNotBlank() }).joinToString(" · ")
                    scope.launch { ShareCard.sharePlant(context, shot.file, e.name, subtitle) }
                }
            },
            onDelete = {
                editing = null
                deleting = shot
            },
            onMakeDefault = if (shot.file != state.entry?.photo) {
                {
                    editing = null
                    viewModel.setCover(shot.id)
                    Toast.makeText(context, "Set as the default picture", Toast.LENGTH_SHORT).show()
                }
            } else {
                null
            },
        )
    }
    deleting?.let { shot ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = Fresh.Page,
            title = { Text("Delete this photo?") },
            text = { Text("It's removed from your phone too. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    viewModel.deletePhoto(shot.id)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }

    val parent = state.entry
    if (repotting && parent != null) {
        var removeOld by rememberSaveable { mutableStateOf(false) }
        CountDialog(
            title = "Repot ${parent.name}",
            text = "How many plants came out? Each joins your collection as ${parent.name}'s child.",
            confirm = { "Add $it" },
            onConfirm = { count ->
                repotting = false
                viewModel.repot(count, removeOld) { added ->
                    Toast.makeText(context, "$added plants added from ${parent.name} 🌱", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { repotting = false },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { removeOld = !removeOld },
            ) {
                Checkbox(
                    checked = removeOld,
                    onCheckedChange = { removeOld = it },
                    colors = CheckboxDefaults.colors(checkedColor = Fresh.GreenDeep),
                )
                Column {
                    Text("The old pot is empty now", style = MaterialTheme.typography.bodyMedium, color = Fresh.Ink)
                    Text("Removes ${parent.name} and its photos; the new plants keep its name.", style = Small, color = Fresh.InkSoft)
                }
            }
        }
    }

    // Deleted from the edit screen: nothing left to show here.
    LaunchedEffect(state) { if (state.loaded && state.entry == null) onBack() }

    Scaffold(containerColor = Fresh.Page) { insets ->
        Column(
            Modifier.padding(insets).padding(horizontal = 20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            val e = state.entry
            TopBar(title = e?.let { Dex.label(it.number) } ?: "", onBack = onBack) {
                if (e != null) {
                    Pill("Edit", color = Fresh.Card, modifier = Modifier.clickable(onClick = onEdit))
                }
            }
            if (e == null) return@Column

            PhotoDiary(shots ?: emptyList(), wished = !e.owned, onAdd = pick, onEditNote = { editing = it }, cover = e.photo)
            origin?.let { OriginCard(it, onOpenPlant, Modifier.fillMaxWidth().padding(top = 12.dp)) }

            Text(e.name, style = MaterialTheme.typography.headlineLarge, color = Fresh.Ink, modifier = Modifier.padding(top = 18.dp))
            if (e.species.isNotBlank()) {
                Text(e.species, style = MaterialTheme.typography.bodyLarge, color = Fresh.InkSoft)
            }
            Spacer(Modifier.height(12.dp))
            Pill(
                text = if (e.owned) {
                    e.acquired?.let { "In your collection since ${it.format(sinceFormat)}" } ?: "In your collection"
                } else {
                    "On your wishlist"
                },
                color = if (e.owned) Fresh.Pastel else Fresh.Card,
                textColor = if (e.owned) Fresh.GreenDeep else Fresh.InkSoft,
            )
            if (!e.owned) {
                Dex.price(e.priceLow, e.priceHigh)?.let { price ->
                    SoftCard(color = Fresh.Pastel, corner = 20.dp, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Price seen", style = Label, color = Fresh.GreenDeep.copy(alpha = 0.75f))
                        Text(price, style = MaterialTheme.typography.titleLarge, color = Fresh.GreenDeep)
                    }
                }
            }

            if (e.note.isNotBlank()) {
                SoftCard(corner = 20.dp, modifier = Modifier.padding(top = 16.dp)) {
                    Text(e.note, style = MaterialTheme.typography.bodyMedium, color = Fresh.Ink)
                }
            }

            val info = Encyclopedia.matching(e.name) ?: e.species.takeIf { it.isNotBlank() }?.let(Encyclopedia::matching)
            if (info != null) {
                SoftCard(color = Fresh.Pastel, corner = 20.dp, onClick = { onOpenEntry(info.id) }, modifier = Modifier.padding(top = 12.dp)) {
                    Text("📖  Read about ${info.name} →", style = MaterialTheme.typography.titleMedium, color = Fresh.GreenDeep)
                    Text("Sun, water and care tips", style = Label, color = Fresh.GreenDeep.copy(alpha = 0.75f))
                }
            }

            PlantList("Children", children, onOpenPlant, Modifier.padding(top = 20.dp))

            if (e.owned) {
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { repotting = true },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Fresh.Ink, contentColor = Fresh.Page),
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) {
                        Text("🪴  Repot", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = onBreed,
                        shape = CircleShape,
                        border = BorderStroke(1.5.dp, Fresh.Ink),
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) {
                        Text("❤️  Breed", style = MaterialTheme.typography.labelLarge, color = Fresh.Ink)
                    }
                }
            }
            if (!e.owned) {
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        viewModel.gotIt()
                        Toast.makeText(context, "Welcome home, ${e.name}! 🎉", Toast.LENGTH_SHORT).show()
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Fresh.Ink, contentColor = Fresh.Page),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text("Got it! 🎉", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Add or edit: photo, name, kind, the day you got it, and notes. A delete goes [onDeleted]: back to the list. */
@Composable
fun DexEditScreen(viewModel: DexEditViewModel, onDone: () -> Unit, onDeleted: () -> Unit) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val owned by viewModel.owned.collectAsStateWithLifecycle()
    val photo by viewModel.photo.collectAsStateWithLifecycle()
    val acquired by viewModel.acquired.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val pick = rememberPhotoPicker { uri ->
        viewModel.changePhoto(uri) { ok ->
            if (!ok) Toast.makeText(context, "Couldn't read that photo", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(containerColor = Fresh.Page) { insets ->
        Column(
            Modifier.padding(insets).padding(horizontal = 20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            TopBar(
                title = when {
                    !viewModel.isNew -> "Edit"
                    owned -> "New plant"
                    else -> "New wish"
                },
                onBack = onDone,
            )
            if (!loaded) return@Column

            Box(
                Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(24.dp)).clickable(onClick = pick),
                contentAlignment = Alignment.Center,
            ) {
                DexPhoto(photo, Modifier.matchParentSize(), wished = !owned, side = 1080, emojiSize = 56.sp)
                if (photo == null) {
                    Pill(
                        "📷  Add a photo",
                        color = Fresh.Page,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            TextField(
                value = form.name,
                onValueChange = { viewModel.update(form.copy(name = it.take(DexForm.MAX_NAME))) },
                placeholder = { Text("Name", color = Fresh.InkSoft) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = softFieldColors(),
                textStyle = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            TextField(
                value = form.species,
                onValueChange = { viewModel.update(form.copy(species = it.take(DexForm.MAX_SPECIES))) },
                placeholder = { Text("Kind, e.g. Astrophytum asterias (optional)", color = Fresh.InkSoft) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = softFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (owned) {
                Spacer(Modifier.height(10.dp))
                DateField("Got it on", acquired, viewModel::setAcquired)
            } else {
                Spacer(Modifier.height(10.dp))
                PriceFields(form) { viewModel.update(it) }
            }
            Spacer(Modifier.height(10.dp))
            TextField(
                value = form.note,
                onValueChange = { viewModel.update(form.copy(note = it.take(DexForm.MAX_NOTE))) },
                placeholder = {
                    Text(
                        if (owned) "Notes: where it's from, price, how it's doing…" else "Notes: where to find it, price…",
                        color = Fresh.InkSoft,
                    )
                },
                minLines = 3,
                shape = RoundedCornerShape(20.dp),
                colors = softFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { viewModel.save(onDone) },
                enabled = form.isValid,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Fresh.Ink,
                    contentColor = Fresh.Page,
                    disabledContainerColor = Fresh.Card,
                    disabledContentColor = Fresh.InkSoft,
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text("Save", style = MaterialTheme.typography.labelLarge)
            }
            if (!viewModel.isNew) {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.padding(top = 8.dp)) {
                    Text(if (owned) "Remove from collection" else "Remove from wishlist", color = Fresh.Error)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove ${form.name.trim()}?") },
            text = { Text("Its photos and notes go too. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete(onDeleted) }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

/**
 * The price you last saw a wish for: one price, or a range (fill both). Digits only; shown
 * back formatted underneath so a missing zero is easy to spot.
 */
@Composable
private fun PriceFields(form: DexForm, onChange: (DexForm) -> Unit) {
    Text("Price seen (optional)", style = Label, color = Fresh.InkSoft, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        PriceField(form.priceLow, "Rp, e.g. 150000", Modifier.weight(1f)) { onChange(form.copy(priceLow = it)) }
        Text("–", style = MaterialTheme.typography.titleMedium, color = Fresh.InkSoft)
        PriceField(form.priceHigh, "up to (optional)", Modifier.weight(1f)) { onChange(form.copy(priceHigh = it)) }
    }
    Dex.price(form.priceLow, form.priceHigh)?.let {
        Text(it, style = Small, color = Fresh.GreenDeep, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
    }
}

@Composable
private fun PriceField(value: Long?, hint: String, modifier: Modifier, onChange: (Long?) -> Unit) {
    TextField(
        value = value?.toString() ?: "",
        onValueChange = { text -> onChange(text.filter(Char::isDigit).take(9).toLongOrNull()) },
        placeholder = { Text(hint, color = Fresh.InkSoft, style = Small) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(20.dp),
        colors = softFieldColors(),
        modifier = modifier,
    )
}
