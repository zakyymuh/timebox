package dev.zakyymuh.timebox.data

import dev.zakyymuh.timebox.core.plants.Plant
import dev.zakyymuh.timebox.core.plants.PlantForm
import dev.zakyymuh.timebox.core.plants.PlantsOverview
import dev.zakyymuh.timebox.core.plants.Watering
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
            // "Last watered" is only asked for new repeating plants; an edit keeps the
            // original anchor, so editing never moves the schedule by itself.
            anchor = existing?.anchor ?: if (form.repeats) form.lastWatered else today,
        )
        val rowId = dao.upsertPlant(entity)
        return if (id != 0L) id else rowId
    }

    suspend fun delete(id: Long) = dao.deletePlant(id)

    /** Records a watering today. Returns its id, so the screen can offer Undo. */
    suspend fun water(plantId: Long): Long =
        dao.insertWatering(WateringEntity(plantId = plantId, date = todayDate(), wateredAt = clock.instant()))

    suspend fun removeWatering(wateringId: Long) = dao.deleteWatering(wateringId)
}
