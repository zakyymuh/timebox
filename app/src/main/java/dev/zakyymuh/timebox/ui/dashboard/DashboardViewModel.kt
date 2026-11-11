package dev.zakyymuh.timebox.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.timebox.core.plants.FunFact
import dev.zakyymuh.timebox.core.plants.FunFacts
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
import java.time.Clock
import java.time.LocalDate
import kotlin.random.Random

/** [date] is null until the database has answered, so nothing flashes. */
data class DashboardState(
    val date: LocalDate? = null,
    /** Three days back, today, three days ahead: what was watered and what is due. */
    val week: List<StripDay> = emptyList(),
    /** "3 plants" on the collection tile. */
    val owned: String = "",
    /** A random owned photo, the collection tile's background; null keeps it lilac. */
    val cover: String? = null,
    /** A random line about a watering or the collection, new each time the app opens; null until anything has happened. */
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

    /** Which event line shows: drawn once, so it is new each time the app opens. */
    private val eventSeed = random.nextInt()

    /** Which photo is the collection tile's background: drawn once, so it changes each open. */
    private val coverSeed = random.nextInt()

    val state: StateFlow<DashboardState> = combine(
        plants.plantsFlow(),
        plants.wateringsFlow(),
        dex.entriesFlow(),
        todayDate,
        fact,
    ) { plantList, waterings, entries, date, shownFact ->
        DashboardState(
            date = date,
            week = WeekStrip.of(plantList, waterings, date),
            owned = entries.count { it.owned }.let { if (it == 1) "1 plant" else "$it plants" },
            cover = entries.filter { it.owned && it.photo != null }.sortedBy { it.number }
                .let { photos -> photos.getOrNull(Math.floorMod(coverSeed, photos.size.coerceAtLeast(1)))?.photo },
            event = PlantEvents.random(plantList, waterings, entries, date, Random(eventSeed)),
            fact = shownFact,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    /** Another fact, never the one already showing. */
    fun shuffleFact() {
        val shown = fact.value
        fact.value = generateSequence { FunFacts.random(random) }.first { it != shown }
    }

    /** Re-reads today, for when the app comes back after midnight. */
    fun refresh() {
        todayDate.value = LocalDate.now(clock)
    }
}
