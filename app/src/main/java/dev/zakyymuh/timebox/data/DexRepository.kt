package dev.zakyymuh.timebox.data

import dev.zakyymuh.timebox.core.collection.Dex
import dev.zakyymuh.timebox.core.collection.DexEntry
import dev.zakyymuh.timebox.core.collection.DexForm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate

/** Removes a stored photo by file name. The real one is [PhotoStore]. */
fun interface PhotoFiles {
    fun delete(name: String)
}

/** The only door to the collection and wishlist. */
class DexRepository(
    private val dao: DexDao,
    private val clock: Clock,
    private val photos: PhotoFiles,
) {
    /** Number handout is read-then-insert; one at a time so two adds never share a number. */
    private val numbering = Mutex()

    fun todayDate(): LocalDate = LocalDate.now(clock)

    fun entriesFlow(): Flow<List<DexEntry>> = dao.entriesFlow().map { it.map(DexEntity::toModel) }

    suspend fun entry(id: Long): DexEntry? = dao.entry(id)?.toModel()

    /** Adds to the collection ([owned]) or the wishlist. Returns the new id. */
    suspend fun add(form: DexForm, owned: Boolean, photo: String?, acquired: LocalDate? = null): Long {
        require(form.isValid) { "invalid dex form" }
        return numbering.withLock {
            dao.insert(
                DexEntity(
                    number = Dex.nextNumber(dao.numbers()),
                    name = form.name.trim(),
                    species = form.species.trim(),
                    owned = owned,
                    photo = photo,
                    acquired = if (owned) acquired ?: todayDate() else null,
                    note = form.note.trim(),
                    createdAt = clock.instant(),
                ),
            )
        }
    }

    /** Changes the words, and for a collected plant the date it was got. Number and photo stay. */
    suspend fun update(id: Long, form: DexForm, acquired: LocalDate? = null) {
        require(form.isValid) { "invalid dex form" }
        val existing = dao.entry(id) ?: return
        dao.update(
            existing.copy(
                name = form.name.trim(),
                species = form.species.trim(),
                note = form.note.trim(),
                acquired = if (existing.owned) acquired ?: existing.acquired else null,
            ),
        )
    }

    /** Puts a new photo on [id]; the old file is deleted so photos never pile up. */
    suspend fun setPhoto(id: Long, photo: String) {
        val existing = dao.entry(id) ?: return photos.delete(photo)
        dao.update(existing.copy(photo = photo))
        existing.photo?.takeIf { it != photo }?.let(photos::delete)
    }

    /** A wished plant comes home: into the collection, same number, dated today. */
    suspend fun gotIt(id: Long) {
        val existing = dao.entry(id) ?: return
        if (existing.owned) return
        val got = Dex.gotIt(existing.toModel(), todayDate())
        dao.update(existing.copy(owned = true, acquired = got.acquired))
    }

    suspend fun delete(id: Long) {
        val existing = dao.entry(id) ?: return
        dao.delete(id)
        existing.photo?.let(photos::delete)
    }
}
