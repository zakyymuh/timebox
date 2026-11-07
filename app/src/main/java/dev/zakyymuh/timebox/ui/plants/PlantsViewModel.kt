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

    private val _undo = MutableSharedFlow<Pair<Long, String>>(extraBufferCapacity = 1)

    /** Each watering just recorded (its id, the plant's name), so the screen can offer Undo. */
    val undo: SharedFlow<Pair<Long, String>> = _undo.asSharedFlow()

    fun water(plantId: Long) {
        viewModelScope.launch {
            val name = repo.plant(plantId)?.name ?: return@launch
            val wateringId = repo.water(plantId) ?: return@launch
            _undo.emit(wateringId to name)
        }
    }

    fun undo(wateringId: Long) {
        viewModelScope.launch { repo.removeWatering(wateringId) }
    }

    /** Re-reads today, for when the app comes back after midnight. */
    fun refresh() {
        today.value = repo.todayDate()
    }
}
