package dev.zakyymuh.motor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.timebox.core.motor.Money
import dev.zakyymuh.timebox.core.motor.ServiceForm
import dev.zakyymuh.timebox.core.motor.ServiceLog
import dev.zakyymuh.timebox.core.motor.ServiceRecord
import dev.zakyymuh.motor.data.ServiceRepository
import dev.zakyymuh.motor.data.VehicleRepository
import dev.zakyymuh.timebox.core.motor.Vehicle
import dev.zakyymuh.timebox.ui.common.RoundButton
import dev.zakyymuh.timebox.ui.common.SoftCard
import dev.zakyymuh.timebox.ui.common.TopBar
import dev.zakyymuh.timebox.ui.common.DateField
import dev.zakyymuh.timebox.ui.common.softFieldColors
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import dev.zakyymuh.timebox.ui.theme.Small
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM yyyy")

class ServiceListViewModel(
    private val repo: ServiceRepository,
    vehicles: VehicleRepository,
    vehicleId: Long,
) : ViewModel() {
    val records: StateFlow<List<ServiceRecord>?> = repo.recordsFlow(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val vehicle: StateFlow<Vehicle?> = vehicles.vehicleFlow(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun year(): Int = repo.todayDate().year
}

class ServiceEditViewModel(
    private val repo: ServiceRepository,
    private val id: Long,
    private val vehicleId: Long,
) : ViewModel() {
    val isNew = id == 0L
    val today = repo.todayDate()

    private val _form = MutableStateFlow(ServiceForm(date = today))
    val form: StateFlow<ServiceForm> = _form.asStateFlow()

    private val _loaded = MutableStateFlow(isNew)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private var busy = false

    init {
        if (!isNew) {
            viewModelScope.launch {
                repo.record(id)?.let { r -> _form.value = ServiceForm(r.date, r.place, r.price, r.work, r.odometerKm) }
                _loaded.value = true
            }
        }
    }

    fun update(form: ServiceForm) {
        _form.value = form
    }

    fun save(onDone: () -> Unit) {
        if (busy || !_form.value.isValid(today)) return
        busy = true
        viewModelScope.launch {
            repo.save(id, vehicleId, _form.value)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            repo.delete(id)
            onDone()
        }
    }
}

/** One vehicle's services, newest first, with what this year has cost so far. */
@Composable
fun ServiceListScreen(
    viewModel: ServiceListViewModel,
    onBack: () -> Unit,
    onEditVehicle: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val vehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    Scaffold(containerColor = Fresh.Page) { insets ->
        Column(Modifier.padding(insets).padding(horizontal = 20.dp).fillMaxSize()) {
            TopBar(title = vehicle?.name ?: "", onBack = onBack) {
                RoundButton("✎", onClick = onEditVehicle, description = "Edit vehicle")
                Spacer(Modifier.width(8.dp))
                RoundButton("+", onClick = onAdd, container = Fresh.Ink, content = Color.White, description = "Log a service")
            }
            vehicle?.plate?.takeIf { it.isNotEmpty() }?.let {
                Text(it, style = Label, color = Fresh.InkSoft, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
            }
            val all = records ?: return@Column
            if (all.isEmpty()) {
                Text(
                    "Keep every service in one place: when, where and what it cost.",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Fresh.InkSoft.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(8.dp))
                Text("Tap + to log your last one.", style = MaterialTheme.typography.bodyMedium, color = Fresh.InkSoft.copy(alpha = 0.6f))
                return@Column
            }
            val sorted = ServiceLog.newestFirst(all)
            val year = viewModel.year()
            SoftCard(color = Fresh.Pastel, corner = 24.dp) {
                Text("Spent in $year", style = Label, color = Fresh.GreenDeep.copy(alpha = 0.75f))
                Text(Money.rupiah(ServiceLog.spentIn(all, year)), style = MaterialTheme.typography.headlineMedium, color = Fresh.GreenDeep)
                val services = all.count { it.date.year == year }
                val km = ServiceLog.kmSincePrevious(all)
                Text(
                    buildString {
                        append(if (services == 1) "1 service" else "$services services")
                        if (km != null) append(" · ${"%,d".format(km).replace(',', '.')} km between the last two")
                    },
                    style = Small,
                    color = Fresh.GreenDeep,
                )
            }
            Spacer(Modifier.height(14.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(sorted, key = { it.id }) { record -> ServiceRow(record) { onEdit(record.id) } }
            }
        }
    }
}

@Composable
private fun ServiceRow(record: ServiceRecord, onClick: () -> Unit) {
    SoftCard(corner = 20.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(record.place, style = MaterialTheme.typography.titleMedium, color = Fresh.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    record.date.format(dateFormat) + (record.odometerKm?.let { " · ${"%,d".format(it).replace(',', '.')} km" } ?: ""),
                    style = Label,
                    color = Fresh.InkSoft,
                )
                if (record.work.isNotBlank()) {
                    Text(record.work, style = MaterialTheme.typography.bodyMedium, color = Fresh.Ink, modifier = Modifier.padding(top = 6.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            record.price?.let {
                Text(
                    Money.rupiah(it),
                    style = MaterialTheme.typography.labelLarge,
                    color = Fresh.GreenDeep,
                    modifier = Modifier
                        .background(Fresh.Pastel, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** Log or edit a service: when, where, price, and optionally what was done and the km. */
@Composable
fun ServiceEditScreen(viewModel: ServiceEditViewModel, onDone: () -> Unit) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(containerColor = Fresh.Page) { insets ->
        Column(
            Modifier.padding(insets).padding(horizontal = 20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            TopBar(title = if (viewModel.isNew) "Log a service" else "Edit service", onBack = onDone)
            if (!loaded) return@Column

            DateField("When", form.date) { viewModel.update(form.copy(date = it)) }
            if (form.date.isAfter(viewModel.today)) {
                Text("That's in the future", style = Small, color = Fresh.Error, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            }
            Spacer(Modifier.height(10.dp))
            Field(form.place, "Where — workshop or bengkel") { viewModel.update(form.copy(place = it.take(ServiceForm.MAX_PLACE))) }
            Spacer(Modifier.height(10.dp))
            NumberField(form.price?.toString(), "Price (Rp)") { viewModel.update(form.copy(price = it?.toLong())) }
            form.price?.let { Text(Money.rupiah(it), style = Small, color = Fresh.GreenDeep, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) }
            Spacer(Modifier.height(10.dp))
            NumberField(form.odometerKm?.toString(), "Odometer, km (optional)") { viewModel.update(form.copy(odometerKm = it?.toInt())) }
            Spacer(Modifier.height(10.dp))
            TextField(
                value = form.work,
                onValueChange = { viewModel.update(form.copy(work = it.take(ServiceForm.MAX_WORK))) },
                placeholder = { Text("What was done: oil, brake pads, CVT… (optional)", color = Fresh.InkSoft) },
                minLines = 3,
                shape = RoundedCornerShape(20.dp),
                colors = softFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { viewModel.save(onDone) },
                enabled = form.isValid(viewModel.today),
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
            if (!viewModel.isNew) {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Delete this service", color = Fresh.Error)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this service?") },
            text = { Text("This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete(onDone) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Field(value: String, hint: String, onChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(hint, color = Fresh.InkSoft) },
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        colors = softFieldColors(),
        textStyle = MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Digits only; an empty field is null. */
@Composable
private fun NumberField(value: String?, hint: String, onChange: (String?) -> Unit) {
    TextField(
        value = value ?: "",
        onValueChange = { text -> onChange(text.filter(Char::isDigit).take(9).ifEmpty { null }) },
        placeholder = { Text(hint, color = Fresh.InkSoft) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(20.dp),
        colors = softFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}
