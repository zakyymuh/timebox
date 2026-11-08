package dev.zakyymuh.timebox.core.collection

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
)

/** What the edit screen holds. */
data class DexForm(
    val name: String = "",
    val species: String = "",
    val note: String = "",
) {
    val isValid: Boolean get() = name.isNotBlank() && name.trim().length <= MAX_NAME

    companion object {
        const val MAX_NAME = 40
        const val MAX_SPECIES = 60
        const val MAX_NOTE = 300
    }
}

object Dex {
    fun label(number: Int): String = "#%03d".format(number)

    /** After the highest ever given, so a deleted number is never handed out again. */
    fun nextNumber(existing: List<Int>): Int = (existing.maxOrNull() ?: 0) + 1

    fun gotIt(entry: DexEntry, today: LocalDate): DexEntry = entry.copy(owned = true, acquired = today)

    fun summary(entries: List<DexEntry>): String {
        val owned = entries.count { it.owned }
        val wished = entries.size - owned
        if (entries.isEmpty()) return "Start your collection"
        val plants = if (owned == 1) "1 plant" else "$owned plants"
        return if (wished == 0) plants else "$plants · $wished on wishlist"
    }
}
