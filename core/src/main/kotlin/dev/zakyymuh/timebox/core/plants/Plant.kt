package dev.zakyymuh.timebox.core.plants

import java.time.LocalDate

/** The six colours a plant can wear. Stored by name, so reordering this is safe; renaming is not. */
enum class PlantColour { MOSS, OCHRE, DUSK, CLAY, PLUM, SLATE }

sealed interface Recurrence {
    /** Every [days] days, counted from the last time it was actually watered. */
    data class EveryDays(val days: Int) : Recurrence

    /** A single watering on [on]; finished by any watering, early or late. */
    data class Once(val on: LocalDate) : Recurrence
}

data class Plant(
    val id: Long,
    val name: String,
    val colour: PlantColour,
    val recurrence: Recurrence,
    /**
     * Repeating: "last watered" given at creation. One-time: only waterings dated after
     * this finish it (the day before creation, or the day it was switched to one-time).
     * Not history: never drawn as a solid dot.
     */
    val anchor: LocalDate,
)

/** One tap of "I watered it". Only the date matters to the schedule. */
data class Watering(val id: Long, val plantId: Long, val date: LocalDate)

sealed interface PlantStatus {
    /** Due today when [daysLate] is 0. */
    data class Due(val daysLate: Int) : PlantStatus

    /** Due in [inDays] days, at least 1. */
    data class Upcoming(val inDays: Int) : PlantStatus

    /** A one-time plant that has been watered. */
    data object Done : PlantStatus
}
