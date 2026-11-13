package dev.zakyymuh.timebox.core.collection

/**
 * Where a collection plant came from: repotted out of one plant ([fatherId] and
 * [fatherName] null), or grown from a cross of two. The names are kept as they were, so the
 * line still reads after a parent leaves the collection (its id then turns null).
 */
data class DexOrigin(
    val motherId: Long?,
    val fatherId: Long?,
    val motherName: String,
    val fatherName: String?,
    val crossId: Long?,
) {
    val label: String
        get() = fatherName?.let { Breeding.name(motherName, it) } ?: motherName
}

object Lineage {
    /** The most plants one repot or one sowing adds at once. */
    const val MAX_COUNT = 50

    fun count(n: Int): Int = n.coerceIn(1, MAX_COUNT)

    /**
     * "[base] #1" onward, counting on after the highest "[base] #n" already [taken], so a
     * second repot never repeats a name. [base] is cut so each name fits [DexForm.MAX_NAME].
     */
    fun names(base: String, count: Int, taken: List<String>): List<String> {
        val start = (taken.mapNotNull { numberIn(it, base.trim()) }.maxOrNull() ?: 0) + 1
        val numbers = start until start + count(count)
        val room = DexForm.MAX_NAME - " #${numbers.last}".length
        val cut = base.trim().take(room).trim()
        return numbers.map { "$cut #$it" }
    }

    private fun numberIn(name: String, base: String): Int? {
        val prefix = "$base #"
        return if (name.startsWith(prefix)) name.removePrefix(prefix).toIntOrNull() else null
    }
}
