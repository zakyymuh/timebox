package dev.zakyymuh.timebox.core.collection

import dev.zakyymuh.timebox.core.motor.Money
import java.time.LocalDate

/**
 * One plant in the dex: either in the collection ([owned]) or on the wishlist.
 * [number] is given once and never changes, so a wished plant keeps it when you get it.
 * [photo] is a file name inside the app's own storage.
 */
data class DexEntry(
    val id: Long,
    val number: Int,
    val name: String,
    val species: String,
    val owned: Boolean,
    val photo: String?,
    val acquired: LocalDate?,
    val note: String,
    /** The price seen for a wish, in rupiah: one price, or a range. */
    val priceLow: Long? = null,
    val priceHigh: Long? = null,
)

/** What the edit screen holds. */
data class DexForm(
    val name: String = "",
    val species: String = "",
    val note: String = "",
    val priceLow: Long? = null,
    val priceHigh: Long? = null,
) {
    val isValid: Boolean
        get() = name.isNotBlank() && name.trim().length <= MAX_NAME &&
            listOfNotNull(priceLow, priceHigh).all { it in 0..MAX_PRICE }

    companion object {
        const val MAX_NAME = 40
        const val MAX_SPECIES = 60
        const val MAX_NOTE = 300

        /** Rp 999.999.999 — anything above is a typo. */
        const val MAX_PRICE = 999_999_999L
    }
}

object Dex {
    fun label(number: Int): String = "#%03d".format(number)

    /** After the highest ever given, so a deleted number is never handed out again. */
    fun nextNumber(existing: List<Int>): Int = (existing.maxOrNull() ?: 0) + 1

    fun gotIt(entry: DexEntry, today: LocalDate): DexEntry = entry.copy(owned = true, acquired = today)

    /** "Rp 150.000", or "Rp 150.000 – 250.000" for a range; null when no price was noted. */
    fun price(low: Long?, high: Long?): String? {
        val values = listOfNotNull(low, high).distinct().sorted()
        if (values.isEmpty()) return null
        return "Rp " + values.joinToString(" – ") { Money.rupiah(it).removePrefix("Rp ") }
    }
}
