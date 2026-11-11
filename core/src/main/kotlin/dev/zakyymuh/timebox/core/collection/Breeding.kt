package dev.zakyymuh.timebox.core.collection

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A cross of two collection plants. [code] is its own id, e.g. "1126001": month, year and
 * the count within that month. The parents' names are kept as they were, so the cross
 * still reads right after a parent leaves the collection ([motherId] then turns null).
 */
data class Cross(
    val id: Long,
    val code: String,
    val name: String,
    val date: LocalDate,
    val motherId: Long?,
    val fatherId: Long?,
    val motherName: String,
    val fatherName: String,
)

/** What the new-cross screen holds. */
data class BreedingForm(
    val motherId: Long? = null,
    val fatherId: Long? = null,
    val name: String = "",
) {
    val isValid: Boolean
        get() = motherId != null && fatherId != null && motherId != fatherId &&
            name.isNotBlank() && name.trim().length <= MAX_NAME

    companion object {
        const val MAX_NAME = 60
    }
}

object Breeding {
    private val day = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

    /** "MMYY" then a three-digit count, after the highest already given that month. */
    fun code(date: LocalDate, existing: List<String>): String {
        val prefix = "%02d%02d".format(date.monthValue, date.year % 100)
        val highest = existing.filter { it.length == 7 && it.startsWith(prefix) }
            .mapNotNull { it.drop(4).toIntOrNull() }
            .maxOrNull() ?: 0
        return prefix + "%03d".format(highest + 1)
    }

    fun name(mother: String, father: String): String = "${mother.trim()} × ${father.trim()}"

    fun date(date: LocalDate): String = date.format(day)

    /** The line on the dashboard card. */
    fun teaser(latest: Cross?): String =
        latest?.let { "Latest: ${it.name} · ${date(it.date)}" } ?: "Pick two plants from your collection to cross"
}
