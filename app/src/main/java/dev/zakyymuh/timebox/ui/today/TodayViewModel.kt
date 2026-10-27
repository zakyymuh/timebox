package dev.zakyymuh.timebox.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.timebox.core.DayList
import dev.zakyymuh.timebox.core.DaySummaries
import dev.zakyymuh.timebox.core.DaySummary
import dev.zakyymuh.timebox.core.TodoItem
import dev.zakyymuh.timebox.data.TimeboxRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayUiState(
    /** Null until the database has answered, so nothing renders a placeholder date. */
    val date: LocalDate? = null,
    val note: String = "",
    val items: List<TodoItem> = emptyList(),
    val summary: DaySummary = DaySummary(0, 0),
    /** Earlier days with items, offered as one-tap starting points on an empty morning. */
    val templates: List<DayList> = emptyList(),
    val draft: String = "",
) {
    val isEmpty: Boolean get() = items.isEmpty()
}

class TodayViewModel(
    private val repo: TimeboxRepository,
    /** Fires the morning reminder now, so it can be tested without waiting for 06:30. */
    private val sendTestReminder: () -> Unit = {},
) : ViewModel() {

    private val draft = MutableStateFlow("")
    private val templates = MutableStateFlow<List<DayList>>(emptyList())

    /**
     * What the user has typed into the note, or null if they have not touched it.
     *
     * The field cannot read its value straight back out of the database: the write is
     * async, so every keystroke would race the re-emission and drop characters or jump
     * the cursor. Local state wins while editing, and the save is debounced.
     */
    private val noteEdits = MutableStateFlow<String?>(null)

    val state: StateFlow<TodayUiState> =
        combine(repo.todayFlow(), draft, templates, noteEdits) { day, currentDraft, recent, edited ->
            TodayUiState(
                date = day.date,
                note = edited ?: day.note.orEmpty(),
                items = day.items,
                summary = DaySummaries.summarise(day),
                templates = recent,
                draft = currentDraft,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    init {
        refreshTemplates()
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
        viewModelScope.launch { repo.addItem(text) }
    }

    fun toggle(itemId: Long) {
        viewModelScope.launch { repo.toggle(itemId) }
    }

    fun rename(itemId: Long, text: String) {
        viewModelScope.launch { repo.rename(itemId, text) }
    }

    fun remove(itemId: Long) {
        viewModelScope.launch { repo.remove(itemId) }
    }

    fun move(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch { repo.move(fromIndex, toIndex) }
    }

    fun testReminder() = sendTestReminder()

    fun useTemplate(date: LocalDate) {
        viewModelScope.launch {
            repo.useTemplate(date)
            refreshTemplates()
        }
    }
}
