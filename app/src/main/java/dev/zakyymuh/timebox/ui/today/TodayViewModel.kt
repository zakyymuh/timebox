package dev.zakyymuh.timebox.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.timebox.core.DayList
import dev.zakyymuh.timebox.core.DaySummaries
import dev.zakyymuh.timebox.core.DaySummary
import dev.zakyymuh.timebox.core.TodoItem
import dev.zakyymuh.timebox.data.TimeboxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayUiState(
    val date: LocalDate = LocalDate.EPOCH,
    val note: String = "",
    val items: List<TodoItem> = emptyList(),
    val summary: DaySummary = DaySummary(0, 0),
    /** Earlier days with items, offered as one-tap starting points on an empty morning. */
    val templates: List<DayList> = emptyList(),
    val draft: String = "",
) {
    val isEmpty: Boolean get() = items.isEmpty()
}

class TodayViewModel(private val repo: TimeboxRepository) : ViewModel() {

    private val draft = MutableStateFlow("")
    private val templates = MutableStateFlow<List<DayList>>(emptyList())

    val state: StateFlow<TodayUiState> =
        combine(repo.todayFlow(), draft, templates) { day, currentDraft, recent ->
            TodayUiState(
                date = day.date,
                note = day.note.orEmpty(),
                items = day.items,
                summary = DaySummaries.summarise(day),
                templates = recent,
                draft = currentDraft,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    init {
        refreshTemplates()
    }

    private fun refreshTemplates() = viewModelScope.launch {
        templates.value = repo.recentDays(limit = 3)
    }

    fun onNoteChanged(text: String) {
        viewModelScope.launch { repo.saveNote(text) }
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

    fun useTemplate(date: LocalDate) {
        viewModelScope.launch {
            repo.useTemplate(date)
            refreshTemplates()
        }
    }
}
