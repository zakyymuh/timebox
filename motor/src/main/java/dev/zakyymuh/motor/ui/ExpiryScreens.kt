package dev.zakyymuh.motor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.motor.data.ExpiryRepository
import dev.zakyymuh.motor.data.VehicleRepository
import dev.zakyymuh.timebox.core.motor.Expiries
import dev.zakyymuh.timebox.core.motor.Expiry
import dev.zakyymuh.timebox.core.motor.ExpiryForm
import dev.zakyymuh.timebox.core.motor.Vehicle
import dev.zakyymuh.timebox.ui.common.DateField
import dev.zakyymuh.timebox.ui.common.SoftCard
import dev.zakyymuh.timebox.ui.common.TopBar
import dev.zakyymuh.timebox.ui.common.softFieldColors
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
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDate = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/** "SIM C", or "STNK tax Avanza" when it is a vehicle's. */
fun Expiry.title(vehicles: Map<Long, Vehicle>): String =
    vehicleId?.let { vehicles[it] }?.let { "$name ${it.name}" } ?: name

/** One row on home: the expiry and what to call it. */
data class ExpiryRow(val expiry: Expiry, val title: String)

/** A renewal just tapped, kept so it can be taken back. */
data class Renewal(val id: Long, val title: String, val from: LocalDate, val to: LocalDate)

class ExpiryListViewModel(private val repo: ExpiryRepository, vehicles: VehicleRepository) : ViewModel() {
    val today: LocalDate = repo.todayDate()

    val rows: StateFlow<List<ExpiryRow>?> =
        combine(repo.expiriesFlow(), vehicles.vehiclesFlow()) { expiries, all ->
            val byId = all.associateBy { it.id }
            expiries.map { ExpiryRow(it, it.title(byId)) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _renewal = MutableStateFlow<Renewal?>(null)
    val renewal: StateFlow<Renewal?> = _renewal.asStateFlow()

    fun renew(row: ExpiryRow) {
        viewModelScope.launch {
            val to = repo.renew(row.expiry.id) ?: return@launch
            _renewal.value = Renewal(row.expiry.id, row.title, row.expiry.date, to)
        }
    }

    fun undoRenewal() {
        val last = _renewal.value ?: return
        _renewal.value = null
        viewModelScope.launch { repo.setDate(last.id, last.from) }
    }

    fun dismissRenewal() {
        _renewal.value = null
    }
}

/**
 * Every date something runs out, soonest first. The 30 days before turn it pastel green
 * and offer "Renewed"; once it has passed, the line turns red.
 */
@Composable
fun ExpirySection(
    rows: List<ExpiryRow>,
    today: LocalDate,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onRenew: (ExpiryRow) -> Unit,
) {
    SectionHeader("Dates", "+ Add", onAdd)
    if (rows.isEmpty()) {
        SoftCard(corner = 20.dp, onClick = onAdd) {
            Text("When do your SIM and STNK run out?", style = MaterialTheme.typography.bodyLarge, color = Fresh.Ink)
            Spacer(Modifier.height(4.dp))
            Text("Add them here: SIM A or C, STNK tax, insurance, KIR.", style = Small, color = Fresh.InkSoft)
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            val date = row.expiry.date
            val overdue = Expiries.isOverdue(date, today)
            val soon = Expiries.isSoon(date, today)
            SoftCard(corner = 20.dp, color = if (soon) Fresh.Pastel else Fresh.Card, onClick = { onEdit(row.expiry.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(row.title, style = MaterialTheme.typography.titleMedium, color = Fresh.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            Expiries.line(date, today),
                            style = Small,
                            color = when {
                                overdue -> Fresh.Error
                                soon -> Fresh.GreenDeep
                                else -> Fresh.InkSoft
                            },
                        )
                    }
                    if (overdue || soon) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Renewed",
                            style = Label,
                            color = Fresh.Page,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (overdue) Fresh.Error else Fresh.GreenDeep)
                                .clickable { onRenew(row) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

/** "SIM C renewed to 12 Mar 2031" for the snackbar. */
fun Renewal.message(): String = "$title renewed to ${to.format(shortDate)}"

class ExpiryEditViewModel(
    private val repo: ExpiryRepository,
    vehicles: VehicleRepository,
    private val id: Long,
) : ViewModel() {
    val isNew = id == 0L
    val today: LocalDate = repo.todayDate()

    val vehicles: StateFlow<List<Vehicle>> = vehicles.vehiclesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _form = MutableStateFlow(ExpiryForm(date = today.plusYears(1)))
    val form: StateFlow<ExpiryForm> = _form.asStateFlow()

    private val _loaded = MutableStateFlow(isNew)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private var busy = false

    init {
        if (!isNew) {
            viewModelScope.launch {
                repo.expiry(id)?.let { e -> _form.value = ExpiryForm(e.vehicleId, e.name, e.date, e.renewMonths) }
                _loaded.value = true
            }
        }
    }

    fun update(form: ExpiryForm) {
        _form.value = form
    }

    fun save(onDone: () -> Unit) {
        if (busy || !_form.value.isValid()) return
        busy = true
        viewModelScope.launch {
            repo.save(id, _form.value)
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

private val renewChoices = listOf(6, 12, 24, 36, 60, 120)

/** Add or edit a date: whose it is, what it is, when it runs out, and how far a renewal moves it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpiryEditScreen(viewModel: ExpiryEditViewModel, onDone: () -> Unit) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(containerColor = Fresh.Page) { insets ->
        Column(
            Modifier.padding(insets).padding(horizontal = 20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            TopBar(title = if (viewModel.isNew) "Add a date" else "Edit date", onBack = onDone)
            if (!loaded) return@Column

            Text("Whose", style = Label, color = Fresh.InkSoft, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Mine", selected = form.vehicleId == null) { viewModel.update(form.copy(vehicleId = null)) }
                vehicles.forEach { v ->
                    Chip("${v.type.glyph()}  ${v.name}", selected = form.vehicleId == v.id) { viewModel.update(form.copy(vehicleId = v.id)) }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("What", style = Label, color = Fresh.InkSoft, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (if (form.vehicleId == null) Expiries.personal else Expiries.vehicle).forEach { kind ->
                    Chip(kind.name, selected = form.name.trim() == kind.name) {
                        viewModel.update(form.copy(name = kind.name, renewMonths = kind.renewMonths))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            TextField(
                value = form.name,
                onValueChange = { viewModel.update(form.copy(name = it.take(ExpiryForm.MAX_NAME))) },
                placeholder = { Text("Or type one — SIM C, STNK tax, insurance…", color = Fresh.InkSoft) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = softFieldColors(),
                textStyle = MaterialTheme.typography.titleMedium,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(18.dp))
            DateField("Runs out on", form.date) { viewModel.update(form.copy(date = it)) }
            Text(
                Expiries.line(form.date, viewModel.today).replaceFirstChar { it.uppercase() },
                style = Small,
                color = if (Expiries.isOverdue(form.date, viewModel.today)) Fresh.Error else Fresh.InkSoft,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )

            Spacer(Modifier.height(18.dp))
            Text("Each renewal lasts", style = Label, color = Fresh.InkSoft, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (renewChoices + form.renewMonths).distinct().sorted().forEach { months ->
                    Chip(Expiries.period(months), selected = form.renewMonths == months) { viewModel.update(form.copy(renewMonths = months)) }
                }
            }

            if (!viewModel.isNew) {
                val next = form.date.plusMonths(form.renewMonths.toLong())
                TextButton(onClick = { viewModel.update(form.copy(date = next)) }, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Renewed? Move it to ${next.format(shortDate)}", color = Fresh.GreenDeep)
                }
            }

            Spacer(Modifier.height(28.dp))
            SaveButton(enabled = form.isValid()) { viewModel.save(onDone) }
            if (!viewModel.isNew) {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Delete this date", color = Fresh.Error)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${form.name.trim().ifEmpty { "this date" }}?") },
            text = { Text("This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete(onDone) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
