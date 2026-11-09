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
            // Editing never drops a "still moist" check.
            snoozedUntil = existing?.snoozedUntil,
        )
        val rowId = dao.upsertPlant(entity)
        if (existing != null) return id
        // "Last watered" is a real watering the owner told us about, so it shows in the
        // week strip and the history. The schedule is unchanged: it counts from that day
        // either way.
        if (form.repeats) {
            val noon = form.lastWatered.atTime(12, 0).atZone(clock.zone).toInstant()
            dao.insertWatering(WateringEntity(plantId = rowId, date = form.lastWatered, wateredAt = noon))
        }
        return rowId
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
        val id = dao.insertWatering(WateringEntity(plantId = plantId, date = today, wateredAt = clock.instant()))
        // A real watering ends any "still moist" check.
        dao.setSnooze(plantId, null)
        id
    }

    /**
     * "Soil still moist": not due before tomorrow. Not a watering — the schedule still
     * counts from the last real one. Returns the previous snooze, for Undo.
     */
    suspend fun snooze(plantId: Long): LocalDate? {
        val previous = dao.plant(plantId)?.snoozedUntil
        dao.setSnooze(plantId, todayDate().plusDays(1))
        return previous
    }

    suspend fun restoreSnooze(plantId: Long, previous: LocalDate?) = dao.setSnooze(plantId, previous)

    /** "Use N" from the interval tip. Repeating plants only; the anchor is unchanged. */
    suspend fun setInterval(plantId: Long, days: Int) {
        require(days in PlantForm.INTERVAL) { "interval out of range: $days" }
        dao.setEveryDays(plantId, days)
    }

    suspend fun removeWatering(wateringId: Long) = dao.deleteWatering(wateringId)
}
