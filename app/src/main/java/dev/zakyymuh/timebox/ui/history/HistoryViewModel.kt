package dev.zakyymuh.timebox.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.timebox.core.plants.HistoryGroup
import dev.zakyymuh.timebox.core.plants.PlantHistory
import dev.zakyymuh.timebox.data.DexRepository
import dev.zakyymuh.timebox.data.PlantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

/** [loaded] is false until the database has answered, so "Nothing has happened yet" never flashes. */
data class HistoryState(
    val query: String = "",
    val groups: List<HistoryGroup> = emptyList(),
    /** Shown instead of the list: nothing yet, or nothing matches. */
    val empty: String? = null,
    val loaded: Boolean = false,
)

/** Everything that happened, newest first, grouped by date, narrowed by the search field. */
class HistoryViewModel(plants: PlantRepository, dex: DexRepository, private val clock: Clock) : ViewModel() {

    private val query = MutableStateFlow("")

    val state: StateFlow<HistoryState> = combine(
        plants.plantsFlow(),
        plants.wateringsFlow(),
        dex.entriesFlow(),
        query,
    ) { plantList, waterings, entries, q ->
        val found = PlantHistory.search(PlantHistory.events(plantList, waterings, entries), q)
        HistoryState(
            query = q,
            groups = PlantHistory.group(found, LocalDate.now(clock)),
            empty = PlantHistory.emptyMessage(found, q),
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryState())

    fun search(text: String) {
        query.value = text
    }
}
