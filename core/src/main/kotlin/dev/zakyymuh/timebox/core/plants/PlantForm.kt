package dev.zakyymuh.timebox.core.plants

import java.time.LocalDate

enum class FormError { NAME_BLANK, NAME_TOO_LONG, INTERVAL_OUT_OF_RANGE, LAST_WATERED_IN_FUTURE }

/**
 * What the add/edit screen holds. Both recurrence fields are kept while the user flips
 * between "repeats" and "one time", so switching back does not lose what they typed.
 */
data class PlantForm(
    val name: String,
    val colour: PlantColour,
    val repeats: Boolean,
    val everyDays: Int,
    val onceDate: LocalDate,
    val lastWatered: LocalDate,
) {
    fun errors(today: LocalDate): Set<FormError> = buildSet {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) add(FormError.NAME_BLANK)
        if (trimmed.length > MAX_NAME) add(FormError.NAME_TOO_LONG)
        if (repeats) {
            if (everyDays !in INTERVAL) add(FormError.INTERVAL_OUT_OF_RANGE)
            if (lastWatered.isAfter(today)) add(FormError.LAST_WATERED_IN_FUTURE)
        }
    }

    fun isValid(today: LocalDate): Boolean = errors(today).isEmpty()

    companion object {
        const val MAX_NAME = 40
        val INTERVAL = 1..90
    }
}
