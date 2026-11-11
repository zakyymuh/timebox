package dev.zakyymuh.timebox.ui.plants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.timebox.core.plants.PlantsOverview
import dev.zakyymuh.timebox.data.PlantRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class PlantsViewModel(private val repo: PlantRepository) : ViewModel() {

    private val today = MutableStateFlow(repo.todayDate())

    /** Null until the database has answered, so the empty state does not flash. */
    val overview: StateFlow<PlantsOverview?> =
        combine(repo.plantsFlow(), repo.wateringsFlow(), today) { plants, waterings, date ->
            PlantsOverview.of(plants, waterings, date)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val todayDate: StateFlow<LocalDate> = today

    private val _undo = MutableSharedFlow<PlantUndo>(extraBufferCapacity = 1)

    /** Each one-tap action, so the screen can offer Undo. */
    val undo: SharedFlow<PlantUndo> = _undo.asSharedFlow()

    fun water(plantId: Long) {
        viewModelScope.launch {
            val name = repo.plant(plantId)?.name ?: return@launch
            val wateringId = repo.water(plantId) ?: return@launch
            _undo.emit(PlantUndo.Watered(wateringId, name))
        }
    }

    /** "Soil still moist": due again in [days] days, and the schedule runs on from there. */
    fun snooze(plantId: Long, days: Int = 1) {
        viewModelScope.launch {
            val name = repo.plant(plantId)?.name ?: return@launch
            val previous = repo.snooze(plantId, days)
            _undo.emit(PlantUndo.Checked(plantId, name, previous))
        }
    }

    /** "Use N" from the interval tip. */
    fun useInterval(plantId: Long, days: Int) {
        viewModelScope.launch { repo.setInterval(plantId, days) }
    }

    fun undo(event: PlantUndo) {
        viewModelScope.launch {
            when (event) {
                is PlantUndo.Watered -> repo.removeWatering(event.wateringId)
                is PlantUndo.Checked -> repo.restoreSnooze(event.plantId, event.previous)
            }
        }
    }

    /** Re-reads today, for when the app comes back after midnight. */
    fun refresh() {
        today.value = repo.todayDate()
    }
}

/** What the plant page's Undo snackbar can put back. */
sealed interface PlantUndo {
    val name: String

    data class Watered(val wateringId: Long, override val name: String) : PlantUndo

    data class Checked(val plantId: Long, override val name: String, val previous: LocalDate?) : PlantUndo
}
