package dev.zakyymuh.timebox.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.timebox.core.DayAccess
import dev.zakyymuh.timebox.core.DayList
import dev.zakyymuh.timebox.core.DaySummaries
import dev.zakyymuh.timebox.core.DaySummary
import dev.zakyymuh.timebox.core.TodoItem
import dev.zakyymuh.timebox.data.TimeboxRepository
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
    val draft: String = "",
    val hasEarlier: Boolean = false,
    val hasLater: Boolean = false,
) {
    val isEmpty: Boolean get() = items.isEmpty()
}

class TodayViewModel(
    private val repo: TimeboxRepository,
    /** Fires the morning reminder now, so it can be tested without waiting for 06:30. */
    private val sendTestReminder: () -> Unit = {},
) : ViewModel() {

    /** The day being looked at. Starts on today and returns there on demand. */
    private val viewing = MutableStateFlow(repo.todayDate())

    private val draft = MutableStateFlow("")
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

    private data class Neighbours(val earlier: LocalDate? = null, val later: LocalDate? = null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val day = viewing.flatMapLatest { repo.dayFlow(it) }

    val state: StateFlow<DayUiState> =
        combine(day, draft, templates, noteEdits, neighbours) { d, currentDraft, recent, edited, near ->
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
                hasLater = near.later != null,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayUiState())

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
        neighbours.value = Neighbours(
            earlier = repo.previousDayWithItems(at),
            later = repo.nextDayWithItems(at),
        )
    }

    // ---- navigating the record -------------------------------------------------

    fun goEarlier() = viewModelScope.launch {
        repo.previousDayWithItems(viewing.value)?.let { moveTo(it) }
    }

    fun goLater() = viewModelScope.launch {
        repo.nextDayWithItems(viewing.value)?.let { moveTo(it) }
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
        draft.value = text
    }

    /** Commits the draft as a new item and clears the field, ready for the next one. */
    fun addDraft() {
        val text = draft.value
        if (text.isBlank()) return
        draft.value = ""
        viewModelScope.launch {
            repo.addItem(text)
            refreshNeighbours()
        }
    }

    fun toggle(itemId: Long) {
        viewModelScope.launch { repo.toggle(itemId) }
    }

    fun remove(itemId: Long) {
        viewModelScope.launch { repo.remove(itemId) }
    }

    fun move(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch { repo.move(fromIndex, toIndex) }
    }

    fun useTemplate(date: LocalDate) {
        viewModelScope.launch {
            repo.useTemplate(date)
            jumpToToday()
            refreshTemplates()
        }
    }

    fun testReminder() = sendTestReminder()
}
