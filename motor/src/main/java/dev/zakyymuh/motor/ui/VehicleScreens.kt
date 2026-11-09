package dev.zakyymuh.motor.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.motor.data.PhotoStore
import dev.zakyymuh.motor.data.ServiceRepository
import dev.zakyymuh.motor.data.VehicleRepository
import dev.zakyymuh.timebox.core.motor.ServiceLog
import dev.zakyymuh.timebox.core.motor.ServiceRecord
import dev.zakyymuh.timebox.core.motor.Vehicle
import dev.zakyymuh.timebox.core.motor.VehicleForm
import dev.zakyymuh.timebox.core.motor.VehicleType
import dev.zakyymuh.timebox.ui.common.SoftCard
import dev.zakyymuh.timebox.ui.common.softFieldColors
import dev.zakyymuh.timebox.ui.common.TopBar
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import dev.zakyymuh.timebox.ui.theme.Small
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** A vehicle and its services, as the home list shows it. */
data class VehicleCard(val vehicle: Vehicle, val services: List<ServiceRecord>)

class VehicleListViewModel(vehicles: VehicleRepository, services: ServiceRepository) : ViewModel() {
    val today: LocalDate = services.todayDate()

    val cards: StateFlow<List<VehicleCard>?> =
        combine(vehicles.vehiclesFlow(), services.byVehicleFlow()) { all, byVehicle ->
            all.map { VehicleCard(it, byVehicle[it.id].orEmpty()) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** The emoji standing in for a vehicle without a photo. */
fun VehicleType.glyph(): String = when (this) {
    VehicleType.MOTORCYCLE -> "🏍"
    VehicleType.CAR -> "🚗"
    VehicleType.OTHER -> "🚲"
}

/** A heading with a small action on the right, like "Vehicles  + Add". */
@Composable
fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = Fresh.Ink, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                style = Label,
                color = Fresh.GreenDeep,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Fresh.Pastel)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

/** Your vehicles, each with its last service. Empty, it explains itself. */
@Composable
fun VehicleSection(
    cards: List<VehicleCard>,
    today: LocalDate,
    photos: PhotoStore,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    SectionHeader("Vehicles", "+ Add", onAdd)
    if (cards.isEmpty()) {
        SoftCard(corner = 20.dp, onClick = onAdd) {
            Text("Add your motorcycle, car, or anything else you ride.", style = MaterialTheme.typography.bodyLarge, color = Fresh.Ink)
            Spacer(Modifier.height(4.dp))
            Text("Then log its services, taxes and papers.", style = Small, color = Fresh.InkSoft)
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cards.forEach { card ->
            SoftCard(corner = 20.dp, onClick = { onOpen(card.vehicle.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VehiclePhoto(photos, card.vehicle.photo, card.vehicle.type, 56.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(card.vehicle.name, style = MaterialTheme.typography.titleMedium, color = Fresh.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (card.vehicle.plate.isNotEmpty()) {
                            Text(card.vehicle.plate, style = Label, color = Fresh.InkSoft)
                        }
                        Text(ServiceLog.summary(card.services, today), style = Small, color = Fresh.InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("›", style = MaterialTheme.typography.titleLarge, color = Fresh.InkSoft)
                }
            }
        }
    }
}

/** The vehicle's photo, cropped square, or its emoji on a pastel tile. */
@Composable
fun VehiclePhoto(photos: PhotoStore, name: String?, type: VehicleType, size: Dp) {
    val bitmap by produceState<ImageBitmap?>(null, name) {
        value = name?.let { photos.load(it)?.asImageBitmap() }
    }
    val shape = RoundedCornerShape(size / 4)
    val image = bitmap
    if (image != null) {
        Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(shape))
    } else {
        Box(Modifier.size(size).clip(shape).background(Fresh.Pastel), contentAlignment = Alignment.Center) {
            Text(type.glyph(), fontSize = (size.value * 0.45f).sp)
        }
    }
}

class VehicleEditViewModel(
    private val repo: VehicleRepository,
    private val photos: PhotoStore,
    private val id: Long,
) : ViewModel() {
    val isNew = id == 0L
    val photoStore: PhotoStore get() = photos

    private val _form = MutableStateFlow(VehicleForm())
    val form: StateFlow<VehicleForm> = _form.asStateFlow()

    private val _loaded = MutableStateFlow(isNew)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /** The photo saved with the vehicle; anything else in the form is not kept yet. */
    private var savedPhoto: String? = null
    private var busy = false
    private var finished = false

    init {
        if (!isNew) {
            viewModelScope.launch {
                repo.vehicle(id)?.let { v ->
                    savedPhoto = v.photo
                    _form.value = VehicleForm(v.type, v.name, v.plate, v.photo)
                }
                _loaded.value = true
            }
        }
    }

    fun update(form: VehicleForm) {
        _form.value = form.copy(photo = _form.value.photo)
    }

    fun pickPhoto(uri: Uri) {
        viewModelScope.launch {
            val name = photos.importPhoto(uri) ?: return@launch
            dropUnsavedPhoto()
            _form.value = _form.value.copy(photo = name)
        }
    }

    fun removePhoto() {
        dropUnsavedPhoto()
        _form.value = _form.value.copy(photo = null)
    }

    private fun dropUnsavedPhoto() {
        val current = _form.value.photo
        if (current != savedPhoto) photos.delete(current)
    }

    fun save(onDone: () -> Unit) {
        if (busy || !_form.value.isValid()) return
        busy = true
        viewModelScope.launch {
            repo.save(id, _form.value)
            if (savedPhoto != _form.value.photo) photos.delete(savedPhoto)
            finished = true
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            repo.delete(id)
            dropUnsavedPhoto()
            photos.delete(savedPhoto)
            finished = true
            onDone()
        }
    }

    /** Leaving without saving: a photo picked on this visit is nobody's, so delete it. */
    override fun onCleared() {
        if (!finished) dropUnsavedPhoto()
    }
}

/** Add or edit a vehicle: what it is, its name, plate and photo. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VehicleEditScreen(viewModel: VehicleEditViewModel, onDone: () -> Unit, onDeleted: () -> Unit) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.pickPhoto(uri)
    }

    Scaffold(containerColor = Fresh.Page) { insets ->
        Column(
            Modifier.padding(insets).padding(horizontal = 20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            TopBar(title = if (viewModel.isNew) "Add a vehicle" else "Edit vehicle", onBack = onDone)
            if (!loaded) return@Column

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clickable { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    VehiclePhoto(viewModel.photoStore, form.photo, form.type, 96.dp)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    TextButton(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Text(if (form.photo == null) "Add a photo" else "Change photo", color = Fresh.GreenDeep)
                    }
                    if (form.photo != null) {
                        TextButton(onClick = viewModel::removePhoto) { Text("Remove photo", color = Fresh.InkSoft) }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VehicleType.entries.forEach { type ->
                    Chip("${type.glyph()}  ${type.label}", selected = form.type == type) { viewModel.update(form.copy(type = type)) }
                }
            }
            Spacer(Modifier.height(16.dp))
            TextField(
                value = form.name,
                onValueChange = { viewModel.update(form.copy(name = it.take(VehicleForm.MAX_NAME))) },
                placeholder = { Text("Name or nickname — Vario, Avanza…", color = Fresh.InkSoft) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = softFieldColors(),
                textStyle = MaterialTheme.typography.titleMedium,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            TextField(
                value = form.plate,
                onValueChange = { viewModel.update(form.copy(plate = it.uppercase().take(VehicleForm.MAX_PLATE + 4))) },
                placeholder = { Text("Plate number, e.g. B 1234 XYZ (optional)", color = Fresh.InkSoft) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = softFieldColors(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(28.dp))
            SaveButton(enabled = form.isValid()) { viewModel.save(onDone) }
            if (!viewModel.isNew) {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Delete this vehicle", color = Fresh.Error)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${form.name.trim().ifEmpty { "this vehicle" }}?") },
            text = { Text("Its service log and dates go with it. This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete(onDeleted) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

/** A round toggle; pastel green when selected. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        style = Label,
        color = if (selected) Fresh.GreenDeep else Fresh.Ink,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Fresh.Pastel else Fresh.Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

/** The full-width black Save button every form ends with. */
@Composable
fun SaveButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Fresh.Ink,
            contentColor = Color.White,
            disabledContainerColor = Fresh.Card,
            disabledContentColor = Fresh.InkSoft,
        ),
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Text("Save", style = MaterialTheme.typography.labelLarge)
    }
}
