package dev.zakyymuh.timebox.ui.collection

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.timebox.core.collection.DexEntry
import dev.zakyymuh.timebox.core.collection.DexForm
import dev.zakyymuh.timebox.core.collection.DexShot
import dev.zakyymuh.timebox.data.DexRepository
import dev.zakyymuh.timebox.data.PhotoStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** The grid. Null until loaded, so the empty message never flashes. */
class CollectionViewModel(repo: DexRepository) : ViewModel() {
    val entries: StateFlow<List<DexEntry>?> = repo.entriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** [entry] is null before loading ([loaded] false) and after a delete ([loaded] true). */
data class DexDetailState(val loaded: Boolean = false, val entry: DexEntry? = null)

/** One plant's page. After a delete the entry is gone, which sends the screen back. */
class DexDetailViewModel(
    private val repo: DexRepository,
    private val photos: PhotoStore,
    private val id: Long,
) : ViewModel() {
    val state: StateFlow<DexDetailState> = repo.entriesFlow()
        .map { list -> DexDetailState(loaded = true, entry = list.firstOrNull { it.id == id }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DexDetailState())

    /** The photo diary, newest first. Null until loaded. */
    val shots: StateFlow<List<DexShot>?> = repo.photosFlow(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun gotIt() {
        viewModelScope.launch { repo.gotIt(id) }
    }

    /** Imports the picked photo into the diary with [note]; [onResult] gets false if it could not be read. */
    fun addPhoto(uri: Uri, note: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val name = photos.import(uri)
            if (name != null) repo.addPhoto(id, name, note)
            onResult(name != null)
        }
    }
}

/** Adding to the collection or wishlist ([id] 0), or editing an entry. */
class DexEditViewModel(
    private val repo: DexRepository,
    private val photos: PhotoStore,
    private val id: Long,
    ownedIfNew: Boolean,
) : ViewModel() {
    val isNew = id == 0L

    private val _form = MutableStateFlow(DexForm())
    val form: StateFlow<DexForm> = _form.asStateFlow()

    private val _owned = MutableStateFlow(ownedIfNew)
    val owned: StateFlow<Boolean> = _owned.asStateFlow()

    private val _photo = MutableStateFlow<String?>(null)
    val photo: StateFlow<String?> = _photo.asStateFlow()

    private val _acquired = MutableStateFlow(repo.todayDate())
    val acquired: StateFlow<LocalDate> = _acquired.asStateFlow()

    private val _loaded = MutableStateFlow(isNew)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /** True once a new entry is saved, so its photo is not cleaned up as an orphan. */
    private var saved = false
    private var busy = false

    init {
        if (!isNew) {
            viewModelScope.launch {
                repo.entry(id)?.let { e ->
                    _form.value = DexForm(e.name, e.species, e.note, e.priceLow, e.priceHigh)
                    _owned.value = e.owned
                    _photo.value = e.photo
                    e.acquired?.let { _acquired.value = it }
                }
                _loaded.value = true
            }
        }
    }

    fun update(form: DexForm) {
        _form.value = form
    }

    fun setAcquired(date: LocalDate) {
        _acquired.value = date
    }

    /** A new entry holds its photo until Save; an existing one adds it to the diary straight away. */
    fun changePhoto(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val name = photos.import(uri)
            if (name != null) {
                if (isNew) {
                    _photo.value?.let(photos::delete)
                } else {
                    repo.addPhoto(id, name, "")
                }
                _photo.value = name
            }
            onResult(name != null)
        }
    }

    fun save(onDone: () -> Unit) {
        if (busy || !_form.value.isValid) return
        busy = true
        viewModelScope.launch {
            if (isNew) {
                repo.add(_form.value, _owned.value, _photo.value, _acquired.value)
                saved = true
            } else {
                repo.update(id, _form.value, _acquired.value)
            }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            repo.delete(id)
            onDone()
        }
    }

    override fun onCleared() {
        // A photo taken for a new entry that was never saved belongs to nothing.
        if (isNew && !saved) _photo.value?.let(photos::delete)
    }
}
