package dev.zakyymuh.timebox.core.collection

import dev.zakyymuh.timebox.core.motor.Money
import java.time.LocalDate
import java.time.YearMonth

/** What went out: the whole plant, or a piece of it while the mother stays. */
enum class SaleKind(val label: String) {
    WHOLE("Whole plant"),
    PUP("Pup"),
    CUTTING("Cutting"),
    SEEDLING("Seedling"),
}

/**
 * One sale. [dexId] is the plant it came from, null once that plant is deleted; [plantName]
 * is a copy so the record still reads. [price] is the total for this sale, in rupiah.
 */
data class Sale(
    val id: Long,
    val dexId: Long?,
    val plantName: String,
    val kind: SaleKind,
    val quantity: Int,
    val price: Long,
    val soldOn: LocalDate,
    val note: String,
)

/** What the record-a-sale sheet holds. */
data class SaleForm(
    val kind: SaleKind = SaleKind.WHOLE,
    val quantity: Int = 1,
    val price: Long? = null,
    val soldOn: LocalDate,
    val note: String = "",
) {
    val isValid: Boolean
        get() = quantity in 1..MAX_QUANTITY && price != null && price in 0..DexForm.MAX_PRICE && note.length <= DexForm.MAX_NOTE

    companion object {
        const val MAX_QUANTITY = 99
    }
}

/** [items] counts pieces (3 pups is 3); the month ones are the calendar month of today. */
data class SalesTotals(val items: Int, val earned: Long, val monthItems: Int, val monthEarned: Long)

object Sales {
    fun totals(sales: List<Sale>, today: LocalDate): SalesTotals {
        val month = YearMonth.from(today)
        val thisMonth = sales.filter { YearMonth.from(it.soldOn) == month }
        return SalesTotals(
            items = sales.sumOf { it.quantity },
            earned = sales.sumOf { it.price },
            monthItems = thisMonth.sumOf { it.quantity },
            monthEarned = thisMonth.sumOf { it.price },
        )
    }

    /** "2 pups from Astro · Rp 50.000", or "Astro · Rp 150.000" for the whole plant. */
    fun line(sale: Sale): String {
        val price = Money.rupiah(sale.price)
        if (sale.kind == SaleKind.WHOLE) return "${sale.plantName} · $price"
        val noun = sale.kind.label.lowercase() + if (sale.quantity == 1) "" else "s"
        return "${sale.quantity} $noun from ${sale.plantName} · $price"
    }

    /** The dashboard tile's title: this month's takings, or "Nothing sold yet" before the first sale. */
    fun dashboardLine(sales: List<Sale>, today: LocalDate): String =
        if (sales.isEmpty()) "Nothing sold yet" else "${Money.rupiah(totals(sales, today).monthEarned)} this month"

    fun suggestedPrice(asking: Long?, quantity: Int): Long? = asking?.times(quantity)

    /** A whole plant is always one. */
    fun normalise(form: SaleForm): SaleForm = if (form.kind == SaleKind.WHOLE) form.copy(quantity = 1) else form
}
