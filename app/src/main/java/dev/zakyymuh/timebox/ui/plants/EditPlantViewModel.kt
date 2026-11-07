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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Adds a plant when [id] is 0, otherwise edits plant [id]. */
class EditPlantViewModel(private val repo: PlantRepository, val id: Long) : ViewModel() {

    val isNew: Boolean get() = id == 0L

    private val today = repo.todayDate()

    private val _form = MutableStateFlow(
        PlantForm(
            name = "",
            colour = PlantColour.MOSS,
            repeats = true,
            everyDays = 7,
            onceDate = today,
            lastWatered = today,
        ),
    )
    val form: StateFlow<PlantForm> = _form

    val canSave: StateFlow<Boolean> =
        _form.map { it.isValid(today) }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

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
                        onceDate = (r as? Recurrence.Once)?.on ?: today,
                        lastWatered = plant.anchor,
                    )
                }
                loaded.value = true
            }
        }
    }

    fun update(form: PlantForm) {
        _form.value = form
    }

    fun save(onDone: () -> Unit) {
        if (!canSave.value) return
        viewModelScope.launch {
            repo.save(id, _form.value)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.delete(id)
            onDone()
        }
    }
}
