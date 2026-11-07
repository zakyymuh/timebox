package dev.zakyymuh.timebox.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.timebox.core.DaySummaries
import dev.zakyymuh.timebox.core.plants.PlantCopy
import dev.zakyymuh.timebox.core.plants.PlantsOverview
import dev.zakyymuh.timebox.data.PlantRepository
import dev.zakyymuh.timebox.data.TimeboxRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** [date] is null until the database has answered, so nothing flashes a wrong day. */
data class DashboardState(val date: LocalDate?, val habit: String, val plants: String)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val timebox: TimeboxRepository,
    private val plants: PlantRepository,
) : ViewModel() {

    private val today = MutableStateFlow(timebox.todayDate())

    val state: StateFlow<DashboardState> = combine(
        today.flatMapLatest { timebox.dayFlow(it) },
        plants.plantsFlow(),
        plants.wateringsFlow(),
        today,
    ) { day, plantList, waterings, date ->
        DashboardState(
            date = date,
            habit = PlantCopy.habit(DaySummaries.summarise(day)),
            plants = PlantCopy.dashboard(PlantsOverview.of(plantList, waterings, date)),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState(null, "", ""))

    /** Re-reads today, for when the app comes back after midnight. */
    fun refresh() {
        today.value = timebox.todayDate()
    }
}
