package dev.zakyymuh.timebox.data

import dev.zakyymuh.timebox.core.collection.Cross
import dev.zakyymuh.timebox.core.collection.Dex
import dev.zakyymuh.timebox.core.collection.DexEntry
import dev.zakyymuh.timebox.core.collection.DexForm
import dev.zakyymuh.timebox.core.collection.DexOrigin
import dev.zakyymuh.timebox.core.collection.DexPhotos
import dev.zakyymuh.timebox.core.collection.DexShot
import dev.zakyymuh.timebox.core.collection.Lineage
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

    /**
     * Removes one photo from its plant's diary, and its file from the phone unless something
     * still uses it. When it was the cover, the newest photo left takes over, or none.
     */
    suspend fun deletePhoto(photoId: Long) {
        val shot = dao.photo(photoId) ?: return
        dao.deletePhoto(photoId)
        val left = dao.photos(shot.dexId)
        val entry = dao.entry(shot.dexId)
        if (entry != null && entry.photo == shot.file) dao.update(entry.copy(photo = left.firstOrNull()?.file))
        if (left.none { it.file == shot.file }) photos.delete(shot.file)
    }

    /** Adds [files] in the order picked, each with [note]; the last one becomes the cover. */
    suspend fun addPhotos(id: Long, files: List<String>, note: String) {
        val existing = dao.entry(id) ?: return files.forEach(photos::delete)
        if (files.isEmpty()) return
        files.forEach { insertPhoto(id, it, note) }
        dao.update(existing.copy(photo = files.last()))
    }

    private suspend fun insertPhoto(id: Long, photo: String, note: String) {
        dao.insertPhoto(
            DexPhotoEntity(dexId = id, file = photo, note = DexPhotos.note(note), takenOn = todayDate(), createdAt = clock.instant()),
        )
    }

    /** Where [id] came from; null for a plant that was simply got. */
    fun originFlow(id: Long): Flow<DexOrigin?> = dao.originFlow(id).map { it?.toModel() }

    /** Plants repotted out of [id] or grown from a cross it was a parent in. */
    fun childrenFlow(id: Long): Flow<List<DexEntry>> = dao.childrenFlow(id).map { it.map(DexEntity::toModel) }

    /** Plants grown from cross [crossId]. */
    fun seedlingsFlow(crossId: Long): Flow<List<DexEntry>> = dao.seedlingsFlow(crossId).map { it.map(DexEntity::toModel) }

    /**
     * Repots [parentId] into [count] new collection plants, named after it and of its kind,
     * each remembering it as their mother. The parent stays unless [removeParent] (an
     * emptied bulk pot). Returns the new ids; none if the parent is gone or only wished.
     */
    suspend fun repot(parentId: Long, count: Int, removeParent: Boolean = false): List<Long> {
        val parent = dao.entry(parentId)?.takeIf { it.owned } ?: return emptyList()
        val origin = DexOriginEntity(dexId = 0, motherId = parent.id, fatherId = null, motherName = parent.name, fatherName = null, crossId = null)
        val ids = addChildren(parent.name, parent.species, count, origin)
        if (removeParent) delete(parent.id)
        return ids
    }

    /** Adds [count] seedlings of [cross] to the collection, each remembering both parents. */
    suspend fun sow(cross: Cross, count: Int): List<Long> {
        val origin = DexOriginEntity(
            dexId = 0,
            motherId = cross.motherId?.takeIf { dao.entry(it) != null },
            fatherId = cross.fatherId?.takeIf { dao.entry(it) != null },
            motherName = cross.motherName,
            fatherName = cross.fatherName,
            crossId = cross.id,
        )
        return addChildren(cross.name, "", count, origin)
    }

    private suspend fun addChildren(base: String, species: String, count: Int, origin: DexOriginEntity): List<Long> =
        numbering.withLock {
            val names = Lineage.names(base, count, dao.names())
            val first = Dex.nextNumber(dao.numbers())
            val children = names.mapIndexed { i, name ->
                DexEntity(
                    number = first + i,
                    name = name,
                    species = species,
                    owned = true,
                    photo = null,
                    acquired = todayDate(),
                    note = "",
                    createdAt = clock.instant(),
                )
            }
            dao.insertChildren(children, origin)
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
