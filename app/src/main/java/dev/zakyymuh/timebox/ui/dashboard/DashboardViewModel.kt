package dev.zakyymuh.timebox.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.timebox.core.DaySummaries
import dev.zakyymuh.timebox.core.DaySummary
import dev.zakyymuh.timebox.core.HabitCheer
import dev.zakyymuh.timebox.core.collection.Dex
import dev.zakyymuh.timebox.core.plants.FunFact
import dev.zakyymuh.timebox.core.plants.FunFacts
import dev.zakyymuh.timebox.core.plants.PlantCopy
import dev.zakyymuh.timebox.core.plants.PlantsOverview
import dev.zakyymuh.timebox.data.DexRepository
import dev.zakyymuh.timebox.core.motor.ServiceLog
import dev.zakyymuh.timebox.data.PlantRepository
import dev.zakyymuh.timebox.data.ServiceRepository
import dev.zakyymuh.timebox.data.TimeboxRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import kotlin.random.Random

/** [date] and [habit] are null until the database has answered, so nothing flashes. */
data class DashboardState(
    val date: LocalDate? = null,
    val habit: DaySummary? = null,
    val cheer: String = "",
    val plants: String = "",
    val collection: String = "",
    /** "Last: 12 Oct at AHASS · 27 days ago". */
    val motor: String = "",
    /** Up to three photos from the collection, newest number first, for the card. */
    val thumbnails: List<String> = emptyList(),
    /** "Did you know?" — null once closed. */
    val fact: FunFact? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val timebox: TimeboxRepository,
    private val plants: PlantRepository,
    private val dex: DexRepository,
    private val service: ServiceRepository,
    random: Random = Random.Default,
) : ViewModel() {

    private val todayDate = MutableStateFlow(timebox.todayDate())

    /** Drawn once per ViewModel, i.e. once each time the app is opened. */
    private val fact = MutableStateFlow<FunFact?>(FunFacts.random(random))

    val state: StateFlow<DashboardState> = combine(
        todayDate.flatMapLatest { timebox.dayFlow(it) },
        plants.plantsFlow(),
        plants.wateringsFlow(),
        combine(dex.entriesFlow(), service.recordsFlow(), ::Pair),
        combine(todayDate, fact, ::Pair),
    ) { day, plantList, waterings, (entries, services), (date, shownFact) ->
        val summary = DaySummaries.summarise(day)
        DashboardState(
            date = date,
            habit = summary,
            cheer = HabitCheer.of(summary),
            plants = PlantCopy.dashboard(PlantsOverview.of(plantList, waterings, date)),
            collection = Dex.summary(entries),
            motor = ServiceLog.summary(services, date),
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
        todayDate.value = timebox.todayDate()
    }
}
