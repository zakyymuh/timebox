package dev.zakyymuh.timebox.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.zakyymuh.timebox.core.plants.Plant
import dev.zakyymuh.timebox.core.plants.PlantColour
import dev.zakyymuh.timebox.core.plants.Recurrence
import dev.zakyymuh.timebox.core.plants.Watering
import java.time.Instant
import java.time.LocalDate

/**
 * A plant. Exactly one of [everyDays] and [onceDate] is set. The next due date is never
 * stored: it is derived from the latest watering every time, so it cannot go stale.
 */
@Entity(tableName = "plant")
data class PlantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colour: String,
    val everyDays: Int?,
    val onceDate: LocalDate?,
    val anchor: LocalDate,
    /** "Soil still moist" — not due before this day. Cleared by watering. */
    val snoozedUntil: LocalDate? = null,
) {
    fun toModel() = Plant(
        id = id,
        name = name,
        colour = PlantColour.entries.firstOrNull { it.name == colour } ?: PlantColour.MOSS,
        recurrence = everyDays?.let { Recurrence.EveryDays(it) } ?: Recurrence.Once(onceDate ?: anchor),
        anchor = anchor,
        snoozedUntil = snoozedUntil,
    )
}

/** One tap of "I watered it". Deleted with its plant. */
@Entity(
    tableName = "watering",
    indices = [Index("plantId")],
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class WateringEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: Long,
    val date: LocalDate,
    val wateredAt: Instant,
) {
    fun toModel() = Watering(id = id, plantId = plantId, date = date)
}
