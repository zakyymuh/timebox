package dev.zakyymuh.timebox.ui.plants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.timebox.core.plants.PlantColour
import dev.zakyymuh.timebox.core.plants.PlantForm
import dev.zakyymuh.timebox.core.plants.Recurrence
import dev.zakyymuh.timebox.data.PlantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Adds a plant when [id] is 0, otherwise edits plant [id]. */
class EditPlantViewModel(private val repo: PlantRepository, val id: Long) : ViewModel() {

    val isNew: Boolean get() = id == 0L

    private val today = MutableStateFlow(repo.todayDate())

    private val _form = MutableStateFlow(
        PlantForm(
            name = "",
            colour = PlantColour.MOSS,
            repeats = true,
            everyDays = 7,
            onceDate = today.value,
            lastWatered = today.value,
        ),
    )
    val form: StateFlow<PlantForm> = _form

    val canSave: StateFlow<Boolean> =
        combine(_form, today) { form, date -> form.isValid(date) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Set from the first Save until it finishes, so a double tap saves once. */
    private var saving = false

    /** Whether the stored plant has been read yet; an edit form is blank until then. */
    val loaded = MutableStateFlow(isNew)

    init {
        if (!isNew) {
            viewModelScope.launch {
                repo.plant(id)?.let { plant ->
                    val r = plant.recurrence
                    _form.value = PlantForm(
                        name = plant.name,
                        colour = plant.colour,
                        repeats = r is Recurrence.EveryDays,
                        everyDays = (r as? Recurrence.EveryDays)?.days ?: 7,
                        onceDate = (r as? Recurrence.Once)?.on ?: today.value,
                        lastWatered = plant.anchor,
                    )
                }
                loaded.value = true
            }
        }
    }

    /**
     * Re-reads today, for when the form comes back after midnight. Dates the user has not
     * touched follow today, so a new plant is not saved as starting yesterday.
     */
    fun refresh() {
        val old = today.value
        val now = repo.todayDate()
        if (now == old) return
        today.value = now
        val f = _form.value
        _form.value = f.copy(
            onceDate = if (f.onceDate == old) now else f.onceDate,
            lastWatered = if (isNew && f.lastWatered == old) now else f.lastWatered,
        )
    }

    fun update(form: PlantForm) {
        _form.value = form
    }

    fun save(onDone: () -> Unit) {
        if (saving || !_form.value.isValid(today.value)) return
        saving = true
        viewModelScope.launch {
            repo.save(id, _form.value)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (saving) return
        saving = true
        viewModelScope.launch {
            repo.delete(id)
            onDone()
        }
    }
}
