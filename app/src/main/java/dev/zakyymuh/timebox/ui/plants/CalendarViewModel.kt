package dev.zakyymuh.timebox.ui.plants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.timebox.core.plants.CalendarMarks
import dev.zakyymuh.timebox.core.plants.Mark
import dev.zakyymuh.timebox.core.plants.MarkKind
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
    /** Plants the schedule expects to water on [selected] (today or later). */
    val selectedPlanned: List<Plant> = emptyList(),
)

class CalendarViewModel(private val repo: PlantRepository) : ViewModel() {

    private val today = MutableStateFlow(repo.todayDate())
    private val month = MutableStateFlow(YearMonth.from(today.value))
    private val selected = MutableStateFlow<LocalDate?>(null)

    val state: StateFlow<CalendarState> = combine(
        repo.plantsFlow(),
        repo.wateringsFlow(),
        month,
        selected,
        today,
    ) { plants, waterings, m, sel, today ->
        val byId = plants.associateBy { it.id }
        val marks = CalendarMarks.forMonth(m, plants, waterings, today)
        CalendarState(
            month = m,
            today = today,
            marks = marks,
            selected = sel,
            selectedWaterings = waterings
                .filter { it.date == sel }
                .mapNotNull { w -> byId[w.plantId]?.let { it to w } },
            selectedPlanned = marks[sel].orEmpty()
                .filter { it.kind == MarkKind.PROJECTED }
                .mapNotNull { byId[it.plantId] },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CalendarState(YearMonth.from(today.value), today.value, emptyMap(), null, emptyList()),
    )

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
        selected.value = null
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
        selected.value = null
    }

    /** Any day can be opened: past days show what was watered, later days what is planned. Tapping it again closes it. */
    fun select(date: LocalDate?) {
        selected.value = date?.takeIf { it != selected.value }
    }

    /** Re-reads today, for when the calendar comes back after midnight. */
    fun refresh() {
        today.value = repo.todayDate()
    }

    fun removeWatering(id: Long) {
        viewModelScope.launch { repo.removeWatering(id) }
    }
}
