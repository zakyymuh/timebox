package dev.zakyymuh.timebox.ui.breeding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.timebox.core.collection.Breeding
import dev.zakyymuh.timebox.core.collection.BreedingForm
import dev.zakyymuh.timebox.core.collection.Cross
import dev.zakyymuh.timebox.core.collection.DexEntry
import dev.zakyymuh.timebox.data.BreedingRepository
import dev.zakyymuh.timebox.data.DexRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Every cross, newest first. Null until loaded. Also feeds the dashboard card. */
class BreedingListViewModel(repo: BreedingRepository) : ViewModel() {
    val crosses: StateFlow<List<Cross>?> = repo.crossesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** A new cross: two parents from the collection, a name made from theirs, and a date. */
class BreedingNewViewModel(private val repo: BreedingRepository, dex: DexRepository) : ViewModel() {
    /** Plants in the collection; wishes cannot be parents. */
    val parents: StateFlow<List<DexEntry>?> = dex.entriesFlow()
        .map { list -> list.filter { it.owned } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _form = MutableStateFlow(BreedingForm())
    val form: StateFlow<BreedingForm> = _form.asStateFlow()

    private val _date = MutableStateFlow(repo.todayDate())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    private val _mother = MutableStateFlow<DexEntry?>(null)
    val mother: StateFlow<DexEntry?> = _mother.asStateFlow()

    private val _father = MutableStateFlow<DexEntry?>(null)
    val father: StateFlow<DexEntry?> = _father.asStateFlow()

    /** Once the owner types a name, picking another parent no longer replaces it. */
    private var renamed = false
    private var busy = false

    fun pickMother(entry: DexEntry) {
        _mother.value = entry
        refresh()
    }

    fun pickFather(entry: DexEntry) {
        _father.value = entry
        refresh()
    }

    fun rename(name: String) {
        renamed = true
        _form.value = _form.value.copy(name = name.take(BreedingForm.MAX_NAME))
    }

    fun setDate(date: LocalDate) {
        _date.value = date
    }

    fun save(onDone: (Long) -> Unit) {
        if (busy || !_form.value.isValid) return
        busy = true
        viewModelScope.launch {
            val id = repo.add(_form.value, _date.value)
            busy = false
            if (id != null) onDone(id)
        }
    }

    private fun refresh() {
        val m = _mother.value
        val f = _father.value
        val name = if (!renamed && m != null && f != null) Breeding.name(m.name, f.name) else _form.value.name
        _form.value = BreedingForm(motherId = m?.id, fatherId = f?.id, name = name)
    }
}

/** One cross, with its parents as they are now in the collection (null once removed). */
data class CrossDetail(val cross: Cross, val mother: DexEntry?, val father: DexEntry?)

/** [detail] is null before loading ([loaded] false), or for a cross that does not exist. */
data class CrossState(val loaded: Boolean = false, val detail: CrossDetail? = null)

class BreedingDetailViewModel(repo: BreedingRepository, dex: DexRepository, id: Long) : ViewModel() {
    val state: StateFlow<CrossState> = combine(repo.crossesFlow(), dex.entriesFlow()) { crosses, entries ->
        val cross = crosses.firstOrNull { it.id == id }
        CrossState(
            loaded = true,
            detail = cross?.let { c ->
                CrossDetail(c, entries.firstOrNull { it.id == c.motherId }, entries.firstOrNull { it.id == c.fatherId })
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CrossState())
}
