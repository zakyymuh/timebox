package dev.zakyymuh.timebox.data

import androidx.room.withTransaction
import dev.zakyymuh.timebox.core.collection.DexEntry
import dev.zakyymuh.timebox.core.collection.Sale
import dev.zakyymuh.timebox.core.collection.SaleForm
import dev.zakyymuh.timebox.core.collection.SaleKind
import dev.zakyymuh.timebox.core.collection.Sales
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate

/**
 * Selling off the collection. A sale of the whole plant marks it sold ([DexEntity.soldOn],
 * the sale's date) and takes it off sale; the plant row, its photos and its family tree all
 * stay. Every write re-derives that from whether a whole-plant sale exists, so editing or
 * deleting one puts the plant back.
 */
class SaleRepository(private val db: TimeboxDatabase, private val clock: Clock) {
    private val sales = db.saleDao()
    private val dex = db.dexDao()

    fun salesFlow(): Flow<List<Sale>> = sales.salesFlow().map { it.map(SaleEntity::toModel) }

    fun forSaleFlow(): Flow<List<DexEntry>> = dex.forSaleFlow().map { it.map(DexEntity::toModel) }

    fun todayDate(): LocalDate = LocalDate.now(clock)

    /** Puts an owned, unsold plant up for sale (or takes it off); anything else is left alone. */
    suspend fun setForSale(dexId: Long, forSale: Boolean, askingPrice: Long?) {
        val plant = dex.entry(dexId)?.takeIf { it.owned && it.soldOn == null } ?: return
        dex.update(plant.copy(forSale = forSale, askingPrice = askingPrice))
    }

    /** The new sale's id, or null when refused: an invalid form, a wish, or a plant already sold. */
    suspend fun record(dexId: Long, form: SaleForm): Long? {
        val sale = Sales.normalise(form).takeIf { it.isValid } ?: return null
        return db.withTransaction {
            val plant = dex.entry(dexId)?.takeIf { it.owned && it.soldOn == null } ?: return@withTransaction null
            val id = sales.insert(
                SaleEntity(
                    dexId = plant.id,
                    plantName = plant.name,
                    kind = sale.kind.name,
                    quantity = sale.quantity,
                    price = sale.price!!,
                    soldOn = sale.soldOn,
                    note = sale.note.trim(),
                    createdAt = clock.instant(),
                ),
            )
            settle(plant.id)
            id
        }
    }

    /** Changes a sale. Turning another plant's only piece-sale into a second whole sale is refused. */
    suspend fun edit(saleId: Long, form: SaleForm) {
        val sale = Sales.normalise(form).takeIf { it.isValid } ?: return
        db.withTransaction {
            val existing = sales.sale(saleId) ?: return@withTransaction
            val plantId = existing.dexId
            if (sale.kind == SaleKind.WHOLE && existing.kind != SaleKind.WHOLE.name && plantId != null) {
                if (sales.wholeSaleFor(plantId) != null) return@withTransaction
            }
            sales.update(
                existing.copy(
                    kind = sale.kind.name,
                    quantity = sale.quantity,
                    price = sale.price!!,
                    soldOn = sale.soldOn,
                    note = sale.note.trim(),
                ),
            )
            plantId?.let { settle(it) }
        }
    }

    suspend fun delete(saleId: Long) {
        db.withTransaction {
            val existing = sales.sale(saleId) ?: return@withTransaction
            sales.delete(saleId)
            existing.dexId?.let { settle(it) }
        }
    }

    /** Sold on the whole-plant sale's date and off sale, or back in the collection without one. */
    private suspend fun settle(dexId: Long) {
        val plant = dex.entry(dexId) ?: return
        val whole = sales.wholeSaleFor(dexId)
        val settled = if (whole != null) plant.copy(soldOn = whole.soldOn, forSale = false) else plant.copy(soldOn = null)
        if (settled != plant) dex.update(settled)
    }
}
