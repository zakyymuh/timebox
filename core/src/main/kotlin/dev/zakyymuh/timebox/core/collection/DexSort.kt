package dev.zakyymuh.timebox.core.collection

/** How the collection grid is ordered. Ties, and plants with nothing to sort by, go newest added first. */
enum class DexSort(val label: String) {
    /** The day it was got, latest first; an undated wish counts by when it was added. */
    Date("Newest") {
        override fun apply(list: List<DexEntry>) =
            list.sortedWith(compareByDescending<DexEntry> { it.acquired }.thenByDescending { it.number })
    },
    Name("A–Z") {
        override fun apply(list: List<DexEntry>) =
            list.sortedWith(compareBy<DexEntry, String>(String.CASE_INSENSITIVE_ORDER) { it.name.trim() }.thenByDescending { it.number })
    },

    /** By the lowest price seen; plants with no price at the end. */
    Price("Cheapest") {
        override fun apply(list: List<DexEntry>) =
            list.sortedWith(compareBy<DexEntry, Long?>(nullsLast()) { it.priceLow ?: it.priceHigh }.thenByDescending { it.number })
    },
    ;

    abstract fun apply(list: List<DexEntry>): List<DexEntry>
}
