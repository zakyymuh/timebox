package dev.zakyymuh.habits.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.timebox.core.DayAccess
import dev.zakyymuh.timebox.core.DayList
import dev.zakyymuh.timebox.core.DaySummaries
import dev.zakyymuh.timebox.core.DaySummary
import dev.zakyymuh.timebox.core.TodoItem
import dev.zakyymuh.timebox.core.lastdone.LastDone
import dev.zakyymuh.timebox.ui.lastdone.LastDoneUi
import dev.zakyymuh.habits.data.SometimesEntity
import dev.zakyymuh.habits.data.TimeboxRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.random.Random

/** What is being typed into the add sheet: a name, an optional icon and description. */
data class Draft(
    val text: String = "",
    val icon: String = CardIcons.default,
    val description: String = "",
    /** Tagged "Sometimes": goes to the pool, offered once the day is cleared. */
    val sometimes: Boolean = false,
)

data class DayUiState(
    /** Null until the database answers, so nothing renders a placeholder date. */
    val date: LocalDate? = null,
    val isToday: Boolean = true,
    /** Only today can be changed. Earlier days are a record, not a backlog. */
    val editable: Boolean = true,
    val note: String = "",
    val items: List<TodoItem> = emptyList(),
    val summary: DaySummary = DaySummary(0, 0),
    /** Earlier days with items, offered as one-tap starting points on an empty morning. */
    val templates: List<DayList> = emptyList(),
    val draft: Draft = Draft(),
    val hasEarlier: Boolean = false,
    /** The nearest earlier day with anything on it, and how it went, for the 📅 card. */
    val earlier: LocalDate? = null,
    val earlierSummary: DaySummary? = null,
    val hasLater: Boolean = false,
    /**
     * A "sometimes" task to offer: only on today, once every card on it is done, and
     * never one already on today's list.
     */
    val offer: SometimesEntity? = null,
    /** Whether "Another one" has anything else to show. */
    val offerHasMore: Boolean = false,
) {
    val isEmpty: Boolean get() = items.isEmpty()
}

class TodayViewModel(
    private val repo: TimeboxRepository,
) : ViewModel() {

    /** The day being looked at. Starts on today and returns there on demand. */
    private val viewing = MutableStateFlow(repo.todayDate())

    private val draft = MutableStateFlow(Draft())
    private val templates = MutableStateFlow<List<DayList>>(emptyList())
    private val neighbours = MutableStateFlow(Neighbours())

    /**
     * What the user has typed into the note, or null if they have not touched it.
     *
     * The field cannot read its value straight back out of the database: the write is
     * async, so every keystroke would race the re-emission and drop characters or jump
     * the cursor. Local state wins while editing, and the save is debounced.
     */
    private val noteEdits = MutableStateFlow<String?>(null)

    private data class Neighbours(
        val earlier: LocalDate? = null,
        val later: LocalDate? = null,
        val earlierSummary: DaySummary? = null,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val day = viewing.flatMapLatest { repo.dayFlow(it) }

    /** Which pooled task is offered; "Not now" moves it on, and a new seed reshuffles. */
    private val offerSeed = MutableStateFlow(Random.nextInt())

    /**
     * What has been waved off today: tasks skipped with "Another one", and whether "Not now"
     * has quieted the popup until tomorrow. Kept in memory only; a new day starts clean.
     */
    private val declined = MutableStateFlow(Declined())

    private data class Declined(
        val date: LocalDate? = null,
        val skipped: Set<Long> = emptySet(),
        val quiet: Boolean = false,
    )

    private data class Offer(val task: SometimesEntity, val hasMore: Boolean)

    private val offer = combine(day, repo.sometimesFlow(), declined, offerSeed) { d, pool, no, seed ->
        val cleared = d.items.isNotEmpty() && d.items.all { it.isDone }
        if (!cleared || !DayAccess.isToday(d.date, repo.todayDate())) return@combine null
        val waved = no.date == d.date
        if (waved && no.quiet) return@combine null
        val onToday = d.items.map { it.text.lowercase() }.toSet()
        val skipped = if (waved) no.skipped else emptySet()
        val left = pool.filter { it.id !in skipped && it.text.lowercase() !in onToday }
        if (left.isEmpty()) null else Offer(left[Random(seed).nextInt(left.size)], left.size > 1)
    }

    private val dayWithOffer = combine(day, offer) { d, o -> d to o }

    val state: StateFlow<DayUiState> =
        combine(dayWithOffer, draft, templates, noteEdits, neighbours) { (d, offered), currentDraft, recent, edited, near ->
            val today = repo.todayDate()
            val editable = DayAccess.isEditable(d.date, today)
            DayUiState(
                date = d.date,
                isToday = DayAccess.isToday(d.date, today),
                editable = editable,
                // An earlier day shows what was written then, never a live edit buffer.
                note = if (editable) edited ?: d.note.orEmpty() else d.note.orEmpty(),
                items = d.items,
                summary = DaySummaries.summarise(d),
                templates = recent,
                draft = currentDraft,
                hasEarlier = near.earlier != null,
                earlier = near.earlier,
                earlierSummary = near.earlierSummary,
                hasLater = near.later != null,
                offer = offered?.task,
                offerHasMore = offered?.hasMore == true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayUiState())

    /** Which fact is showing. A new seed is a new random fact; tapping the card shuffles. */
    private val factSeed = MutableStateFlow(Random.nextInt())

    val lastDone: StateFlow<LastDoneUi> =
        combine(repo.eventsFlow(), factSeed) { events, seed ->
            val today = repo.todayDate()
            LastDoneUi(events, LastDone.funFact(events, today, Random(seed)), today)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LastDoneUi())

    init {
        refreshTemplates()
        refreshNeighbours()
        saveNoteEditsWhenTypingStops()
    }

    /**
     * One write when typing pauses, rather than one per keystroke — which also means one
     * widget redraw instead of dozens.
     */
    @OptIn(FlowPreview::class)
    private fun saveNoteEditsWhenTypingStops() = viewModelScope.launch {
        noteEdits.filterNotNull()
            .debounce(600)
            .collect { repo.saveNote(it) }
    }

    private fun refreshTemplates() = viewModelScope.launch {
        templates.value = repo.recentDays(limit = 3)
    }

    private fun refreshNeighbours() = viewModelScope.launch {
        val at = viewing.value
        val earlier = repo.previousDayWithItems(at)
        neighbours.value = Neighbours(
            earlier = earlier,
            later = DayAccess.nextStep(at, repo.nextDayWithItems(at), repo.todayDate()),
            earlierSummary = earlier?.let { DaySummaries.summarise(repo.dayNow(it)) },
        )
    }

    // ---- navigating the record -------------------------------------------------

    fun goEarlier() = viewModelScope.launch {
        repo.previousDayWithItems(viewing.value)?.let { moveTo(it) }
    }

    fun goLater() = viewModelScope.launch {
        val at = viewing.value
        DayAccess.nextStep(at, repo.nextDayWithItems(at), repo.todayDate())?.let { moveTo(it) }
    }

    fun jumpToToday() = moveTo(repo.todayDate())

    private fun moveTo(date: LocalDate) {
        viewing.value = date
        // Leaving a day discards any half-typed note edit rather than carrying it over.
        noteEdits.value = null
        refreshNeighbours()
    }

    // ---- editing today ---------------------------------------------------------

    fun onNoteChanged(text: String) {
        noteEdits.value = text
    }

    fun onDraftChanged(text: String) {
        draft.value = draft.value.copy(text = text)
    }

    fun onDraftIcon(icon: String) {
        draft.value = draft.value.copy(icon = icon)
    }

    fun onDraftDescription(text: String) {
        draft.value = draft.value.copy(description = text)
    }

    fun onDraftSometimes(on: Boolean) {
        draft.value = draft.value.copy(sometimes = on)
    }

    /**
     * Commits the draft as a new card and clears the name and description, ready for the
     * next one. The icon stays picked, since a run of similar tasks often shares one.
     */
    fun addDraft() {
        val d = draft.value
        if (d.text.isBlank()) return
        draft.value = d.copy(text = "", description = "")
        viewModelScope.launch {
            if (d.sometimes) {
                repo.addSometimes(d.text, d.icon, d.description)
            } else {
                repo.addItem(d.text, d.icon, d.description)
                refreshNeighbours()
            }
        }
    }

    // ---- sometimes -------------------------------------------------------------

    /** "Do it now": the offered task joins today's list as a fresh card. */
    fun takeOffer(id: Long) {
        viewModelScope.launch { repo.takeSometimes(id) }
    }

    /** "Not now": no more offers until tomorrow. */
    fun declineOffer() {
        declined.value = todaysDeclined().copy(quiet = true)
    }

    /** "Another one": skips this task for today and shows a different one. */
    fun anotherOffer(id: Long) {
        val now = todaysDeclined()
        declined.value = now.copy(skipped = now.skipped + id)
        offerSeed.value = Random.nextInt()
    }

    private fun todaysDeclined(): Declined {
        val today = repo.todayDate()
        return declined.value.takeIf { it.date == today } ?: Declined(today)
    }

    /** Takes a task out of the pool for good. */
    fun removeSometimes(id: Long) {
        viewModelScope.launch { repo.removeSometimes(id) }
    }

    /** "Working on it": the card turns dark and holds the screen until [finish]. */
    fun startWorking(itemId: Long) {
        viewModelScope.launch { repo.startWorking(itemId) }
    }

    /** "Cancel": undoes a mistaken "Working on it". */
    fun stopWorking(itemId: Long) {
        viewModelScope.launch { repo.stopWorking(itemId) }
    }

    fun finish(itemId: Long) {
        viewModelScope.launch { repo.setDone(itemId, true) }
    }

    fun editItem(itemId: Long, text: String, icon: String?, description: String?) {
        viewModelScope.launch { repo.editItem(itemId, text, icon, description) }
    }

    fun remove(itemId: Long) {
        viewModelScope.launch {
            repo.remove(itemId)
            refreshNeighbours()
        }
    }

    fun useTemplate(date: LocalDate) {
        viewModelScope.launch {
            repo.useTemplate(date)
            jumpToToday()
            refreshTemplates()
        }
    }

    // ---- last time I did … -----------------------------------------------------

    fun shuffleFact() {
        factSeed.value = Random.nextInt()
    }

    fun markDone(name: String) {
        viewModelScope.launch { repo.markDone(name) }
    }

    fun undoLastMark(key: String) {
        viewModelScope.launch { repo.undoLastMark(key) }
    }
}
