package dev.zakyymuh.timebox.data

import dev.zakyymuh.timebox.core.plants.Plant
import dev.zakyymuh.timebox.core.plants.PlantForm
import dev.zakyymuh.timebox.core.plants.PlantsOverview
import dev.zakyymuh.timebox.core.plants.Watering
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate

/** The only door to plant storage. Every schedule decision lives in :core. */
class PlantRepository(
    private val dao: PlantDao,
    private val clock: Clock,
) {

    fun todayDate(): LocalDate = LocalDate.now(clock)

    fun plantsFlow(): Flow<List<Plant>> = dao.plantsFlow().map { it.map(PlantEntity::toModel) }

    fun wateringsFlow(): Flow<List<Watering>> = dao.wateringsFlow().map { it.map(WateringEntity::toModel) }

    /** For the 18:00 receiver, which has no flow to collect. */
    suspend fun overviewNow(): PlantsOverview = PlantsOverview.of(
        dao.plants().map(PlantEntity::toModel),
        dao.waterings().map(WateringEntity::toModel),
        todayDate(),
    )

    suspend fun plant(id: Long): Plant? = dao.plant(id)?.toModel()

    /** Saves [form] as plant [id], or as a new plant when [id] is 0. Returns the plant's id. */
    suspend fun save(id: Long, form: PlantForm): Long {
        val today = todayDate()
        require(form.isValid(today)) { "invalid plant form: ${form.errors(today)}" }
        val existing = if (id != 0L) dao.plant(id) else null
        val entity = PlantEntity(
            id = id,
            name = form.name.trim(),
            colour = form.colour.name,
            everyDays = if (form.repeats) form.everyDays else null,
            onceDate = if (form.repeats) null else form.onceDate,
            anchor = anchorFor(existing, form, today),
        )
        val rowId = dao.upsertPlant(entity)
        return if (id != 0L) id else rowId
    }

    /**
     * New plants: "last watered" for repeating ones, yesterday for one-time ones. Edits keep
     * the anchor, so renaming or recolouring never moves the schedule, except when the
     * plant switches between repeating and one-time or its one-time date changes: then
     * it starts fresh from today. Otherwise a watered plant switched to one-time would
     * count as already done, and an old one-time plant switched to repeating would be
     * weeks overdue.
     */
    private fun anchorFor(existing: PlantEntity?, form: PlantForm, today: LocalDate): LocalDate {
        // A new one-time plant anchors on yesterday, so watering it early today finishes it.
        if (existing == null) return if (form.repeats) form.lastWatered else today.minusDays(1)
        val wasRepeating = existing.everyDays != null
        val kindChanged = wasRepeating != form.repeats
        val onceMoved = !form.repeats && existing.onceDate != form.onceDate
        return if (kindChanged || onceMoved) today else existing.anchor
    }

    suspend fun delete(id: Long) = dao.deletePlant(id)

    private val watering = Mutex()

    /**
     * Records a watering today and returns its id, so the screen can offer Undo. A plant
     * is watered at most once a day: a second call the same day (a double tap, or the
     * notification button after the app) records nothing and returns null.
     */
    suspend fun water(plantId: Long): Long? = watering.withLock {
        val today = todayDate()
        if (dao.wateringOn(plantId, today) != null) return@withLock null
        dao.insertWatering(WateringEntity(plantId = plantId, date = today, wateredAt = clock.instant()))
    }

    suspend fun removeWatering(wateringId: Long) = dao.deleteWatering(wateringId)
}
