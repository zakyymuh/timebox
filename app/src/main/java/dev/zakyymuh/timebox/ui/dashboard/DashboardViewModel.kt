package dev.zakyymuh.timebox.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.timebox.core.plants.FunFact
import dev.zakyymuh.timebox.core.plants.FunFacts
import dev.zakyymuh.timebox.core.plants.Plant
import dev.zakyymuh.timebox.core.plants.PlantEvents
import dev.zakyymuh.timebox.core.plants.StripDay
import dev.zakyymuh.timebox.core.plants.WeekStrip
import dev.zakyymuh.timebox.data.DexRepository
import dev.zakyymuh.timebox.data.PlantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import kotlin.random.Random

/** [date] is null until the database has answered, so nothing flashes. */
data class DashboardState(
    val date: LocalDate? = null,
    /** Three days back, today, three days ahead: what was watered and what is due. */
    val week: List<StripDay> = emptyList(),
    /** Every plant by name, for the Delete button's list. */
    val plants: List<Plant> = emptyList(),
    /** The newest owned photo, for the collection tile; null shows 🪴. */
    val cover: String? = null,
    /** A random line about a watering or the collection; null until anything has happened. */
    val event: String? = null,
    /** A plant fact from the encyclopedia; tapping it draws another. */
    val fact: FunFact? = null,
)

class DashboardViewModel(
    private val plants: PlantRepository,
    private val dex: DexRepository,
    private val clock: Clock,
    private val random: Random = Random.Default,
) : ViewModel() {

    private val todayDate = MutableStateFlow(LocalDate.now(clock))

    /** Drawn when the app opens; a tap draws again. */
    private val fact = MutableStateFlow(FunFacts.random(random))

    /** Which event line is showing. A new seed is a new random line. */
    private val eventSeed = MutableStateFlow(random.nextInt())

    val state: StateFlow<DashboardState> = combine(
        plants.plantsFlow(),
        plants.wateringsFlow(),
        dex.entriesFlow(),
        todayDate,
        combine(fact, eventSeed, ::Pair),
    ) { plantList, waterings, entries, date, (shownFact, seed) ->
        DashboardState(
            date = date,
            week = WeekStrip.of(plantList, waterings, date),
            plants = plantList.sortedBy { it.name.lowercase() },
            cover = entries.filter { it.owned && it.photo != null }.maxByOrNull { it.number }?.photo,
            event = PlantEvents.random(plantList, waterings, entries, date, Random(seed)),
            fact = shownFact,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    /** Another fact, never the one already showing. */
    fun shuffleFact() {
        val shown = fact.value
        fact.value = generateSequence { FunFacts.random(random) }.first { it != shown }
    }

    /** Deletes a plant and its watering history, after the screen has asked. */
    fun deletePlant(id: Long) {
        viewModelScope.launch { plants.delete(id) }
    }

    /** Another event line. */
    fun shuffleEvent() {
        eventSeed.value = random.nextInt()
    }

    /** Re-reads today, for when the app comes back after midnight. */
    fun refresh() {
        todayDate.value = LocalDate.now(clock)
    }
}
