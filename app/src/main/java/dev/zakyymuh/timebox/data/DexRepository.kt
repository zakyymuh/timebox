package dev.zakyymuh.timebox.data

import dev.zakyymuh.timebox.core.collection.Dex
import dev.zakyymuh.timebox.core.collection.DexEntry
import dev.zakyymuh.timebox.core.collection.DexForm
import dev.zakyymuh.timebox.core.collection.DexPhotos
import dev.zakyymuh.timebox.core.collection.DexShot
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

    /** A plant's photo diary, newest first. */
    fun photosFlow(id: Long): Flow<List<DexShot>> = dao.photosFlow(id).map { it.map(DexPhotoEntity::toModel) }

    /** Adds to the collection ([owned]) or the wishlist; a [photo] starts its diary. Returns the new id. */
    suspend fun add(form: DexForm, owned: Boolean, photo: String?, acquired: LocalDate? = null): Long {
        require(form.isValid) { "invalid dex form" }
        val id = numbering.withLock {
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
                    priceLow = form.priceLow,
                    priceHigh = form.priceHigh,
                ),
            )
        }
        if (photo != null) insertPhoto(id, photo, "")
        return id
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
                priceLow = form.priceLow,
                priceHigh = form.priceHigh,
            ),
        )
    }

    /** Adds a photo to [id]'s diary and makes it the cover. Older photos stay. */
    suspend fun addPhoto(id: Long, photo: String, note: String) {
        val existing = dao.entry(id) ?: return photos.delete(photo)
        insertPhoto(id, photo, note)
        dao.update(existing.copy(photo = photo))
    }

    /** Changes the note on one photo; the photo and its place stay. */
    suspend fun setPhotoNote(photoId: Long, note: String) = dao.setPhotoNote(photoId, DexPhotos.note(note))

    private suspend fun insertPhoto(id: Long, photo: String, note: String) {
        dao.insertPhoto(
            DexPhotoEntity(dexId = id, file = photo, note = DexPhotos.note(note), takenOn = todayDate(), createdAt = clock.instant()),
        )
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
        val files = dao.photos(id).map { it.file } + listOfNotNull(existing.photo)
        dao.delete(id)
        files.distinct().forEach(photos::delete)
    }
}
