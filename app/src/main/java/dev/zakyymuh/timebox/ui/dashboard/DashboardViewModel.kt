package dev.zakyymuh.timebox.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.timebox.core.collection.Dex
import dev.zakyymuh.timebox.core.plants.FunFact
import dev.zakyymuh.timebox.core.plants.FunFacts
import dev.zakyymuh.timebox.core.plants.PlantCopy
import dev.zakyymuh.timebox.core.plants.PlantRow
import dev.zakyymuh.timebox.core.plants.PlantsOverview
import dev.zakyymuh.timebox.data.DexRepository
import dev.zakyymuh.timebox.data.PlantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import kotlin.random.Random

/** [date] is null until the database has answered, so nothing flashes. */
data class DashboardState(
    val date: LocalDate? = null,
    val plants: String = "",
    /** The plant to water soonest — the most overdue one, else the next coming up. */
    val nextWatering: PlantRow? = null,
    val collection: String = "",
    /** Up to three photos from the collection, newest number first, for the card. */
    val thumbnails: List<String> = emptyList(),
    /** "Did you know?" — null once closed. */
    val fact: FunFact? = null,
)

class DashboardViewModel(
    private val plants: PlantRepository,
    private val dex: DexRepository,
    private val clock: Clock,
    random: Random = Random.Default,
) : ViewModel() {

    private val todayDate = MutableStateFlow(LocalDate.now(clock))

    /** Drawn once per ViewModel, i.e. once each time the app is opened. */
    private val fact = MutableStateFlow<FunFact?>(FunFacts.random(random))

    val state: StateFlow<DashboardState> = combine(
        plants.plantsFlow(),
        plants.wateringsFlow(),
        dex.entriesFlow(),
        todayDate,
        fact,
    ) { plantList, waterings, entries, date, shownFact ->
        val overview = PlantsOverview.of(plantList, waterings, date)
        DashboardState(
            date = date,
            plants = PlantCopy.dashboard(overview),
            nextWatering = overview.due.firstOrNull() ?: overview.upcoming.firstOrNull(),
            collection = Dex.summary(entries),
            thumbnails = entries.filter { it.owned && it.photo != null }.sortedByDescending { it.number }
                .take(3).mapNotNull { it.photo },
            fact = shownFact,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    /** Hides the fun fact until the app is next opened. */
    fun closeFact() {
        fact.value = null
    }

    /** Re-reads today, for when the app comes back after midnight. */
    fun refresh() {
        todayDate.value = LocalDate.now(clock)
    }
}
