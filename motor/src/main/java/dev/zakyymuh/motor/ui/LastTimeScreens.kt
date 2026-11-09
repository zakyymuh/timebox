package dev.zakyymuh.motor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.motor.data.MarkLog
import dev.zakyymuh.motor.data.MarkRepository
import dev.zakyymuh.motor.data.VehicleRepository
import dev.zakyymuh.timebox.core.lastdone.Event
import dev.zakyymuh.timebox.core.lastdone.LastDone
import dev.zakyymuh.timebox.core.motor.Vehicle
import dev.zakyymuh.timebox.ui.lastdone.LastDoneUi
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Irregular mobility chores — cuci motor, tambah angin, SIM for the car — and how long ago. */
class LastTimeViewModel(private val repo: MarkRepository, vehicles: VehicleRepository) : ViewModel() {

    private val factSeed = MutableStateFlow(Random.nextLong())

    private val log: StateFlow<MarkLog> = repo.logFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MarkLog())

    val lastDone: StateFlow<LastDoneUi> =
        combine(log, factSeed) { current, seed ->
            val today = repo.todayDate()
            LastDoneUi(current.events, LastDone.funFact(current.events, today, Random(seed)), today)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LastDoneUi())

    val vehicles: StateFlow<List<Vehicle>> = vehicles.vehiclesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun shuffleFact() {
        factSeed.value = Random.nextLong()
    }

    /** A typed name, for the vehicle picked under the field (or none). */
    fun markDone(name: String, vehicleId: Long?) {
        viewModelScope.launch { repo.markDone(name, vehicleId) }
    }

    /** "Did it" on a row: the same chore, for the same vehicle, again. */
    fun again(event: Event) {
        val target = log.value.targets[event.key] ?: return
        viewModelScope.launch { repo.markDone(target.name, target.vehicleId) }
    }

    fun undoLastMark(key: String) {
        viewModelScope.launch { repo.undoLastMark(key) }
    }
}

/** Under the text field: which vehicle a typed chore is for. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.ForVehicle(vehicles: List<Vehicle>, selected: Long?, onSelect: (Long?) -> Unit) {
    if (vehicles.isEmpty()) return
    Text("For", style = Label, color = Fresh.InkSoft, modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("No vehicle", selected = selected == null) { onSelect(null) }
        vehicles.forEach { v ->
            Chip("${v.type.glyph()}  ${v.name}", selected = selected == v.id) { onSelect(v.id) }
        }
    }
}
