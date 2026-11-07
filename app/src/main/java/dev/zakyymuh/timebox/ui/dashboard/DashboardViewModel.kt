package dev.zakyymuh.timebox.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.timebox.core.DaySummaries
import dev.zakyymuh.timebox.core.TodayAgenda
import dev.zakyymuh.timebox.core.TodayEntry
import dev.zakyymuh.timebox.core.plants.PlantCopy
import dev.zakyymuh.timebox.core.plants.PlantsOverview
import dev.zakyymuh.timebox.data.PlantRepository
import dev.zakyymuh.timebox.data.TimeboxRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** [date] and [today] are null until the database has answered, so nothing flashes. */
data class DashboardState(
    val date: LocalDate?,
    val habit: String,
    val plants: String,
    val today: List<TodayEntry>?,
)

/** What the Undo snackbar can put back. */
sealed interface DashboardUndo {
    data class Watered(val wateringId: Long, val name: String) : DashboardUndo
    data class Ticked(val itemId: Long, val text: String) : DashboardUndo
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val timebox: TimeboxRepository,
    private val plants: PlantRepository,
) : ViewModel() {

    private val todayDate = MutableStateFlow(timebox.todayDate())

    val state: StateFlow<DashboardState> = combine(
        todayDate.flatMapLatest { timebox.dayFlow(it) },
        plants.plantsFlow(),
        plants.wateringsFlow(),
        todayDate,
    ) { day, plantList, waterings, date ->
        val overview = PlantsOverview.of(plantList, waterings, date)
        DashboardState(
            date = date,
            habit = PlantCopy.habit(DaySummaries.summarise(day)),
            plants = PlantCopy.dashboard(overview),
            today = TodayAgenda.of(day, overview),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState(null, "", "", null))

    private val _undo = MutableSharedFlow<DashboardUndo>(extraBufferCapacity = 1)

    /** Each one-tap action from Today, so the screen can offer Undo. */
    val undo: SharedFlow<DashboardUndo> = _undo.asSharedFlow()

    fun water(plantId: Long) {
        viewModelScope.launch {
            val name = plants.plant(plantId)?.name ?: return@launch
            _undo.emit(DashboardUndo.Watered(plants.water(plantId), name))
        }
    }

    fun tick(itemId: Long) {
        viewModelScope.launch {
            val item = timebox.todayNow().items.firstOrNull { it.id == itemId } ?: return@launch
            if (item.isDone) return@launch
            timebox.toggle(itemId)
            _undo.emit(DashboardUndo.Ticked(itemId, item.text))
        }
    }

    fun undo(event: DashboardUndo) {
        viewModelScope.launch {
            when (event) {
                is DashboardUndo.Watered -> plants.removeWatering(event.wateringId)
                // Toggle flips it back; only if it is still ticked, so a double Undo is harmless.
                is DashboardUndo.Ticked ->
                    if (timebox.todayNow().items.firstOrNull { it.id == event.itemId }?.isDone == true) {
                        timebox.toggle(event.itemId)
                    }
            }
        }
    }

    /** Re-reads today, for when the app comes back after midnight. */
    fun refresh() {
        todayDate.value = timebox.todayDate()
    }
}
