package dev.zakyymuh.timebox.ui.plants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.timebox.core.plants.CalendarMarks
import dev.zakyymuh.timebox.core.plants.Mark
import dev.zakyymuh.timebox.core.plants.Plant
import dev.zakyymuh.timebox.core.plants.Watering
import dev.zakyymuh.timebox.data.PlantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarState(
    val month: YearMonth,
    val today: LocalDate,
    val marks: Map<LocalDate, List<Mark>>,
    val selected: LocalDate?,
    /** Waterings on [selected], each with its plant, so a mistake can be removed. */
    val selectedWaterings: List<Pair<Plant, Watering>>,
)

class CalendarViewModel(private val repo: PlantRepository) : ViewModel() {

    private val today = repo.todayDate()
    private val month = MutableStateFlow(YearMonth.from(today))
    private val selected = MutableStateFlow<LocalDate?>(null)

    val state: StateFlow<CalendarState> = combine(
        repo.plantsFlow(),
        repo.wateringsFlow(),
        month,
        selected,
    ) { plants, waterings, m, sel ->
        val byId = plants.associateBy { it.id }
        CalendarState(
            month = m,
            today = today,
            marks = CalendarMarks.forMonth(m, plants, waterings, today),
            selected = sel,
            selectedWaterings = waterings
                .filter { it.date == sel }
                .mapNotNull { w -> byId[w.plantId]?.let { it to w } },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CalendarState(YearMonth.from(today), today, emptyMap(), null, emptyList()),
    )

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
        selected.value = null
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
        selected.value = null
    }

    /** Only past days and today hold waterings, so only those can be opened. */
    fun select(date: LocalDate?) {
        selected.value = date?.takeIf { !it.isAfter(today) && it != selected.value }
    }

    fun removeWatering(id: Long) {
        viewModelScope.launch { repo.removeWatering(id) }
    }
}
