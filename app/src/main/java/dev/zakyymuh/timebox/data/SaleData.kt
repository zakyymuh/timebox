package dev.zakyymuh.timebox.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import dev.zakyymuh.timebox.core.collection.Sale
import dev.zakyymuh.timebox.core.collection.SaleKind
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/**
 * One sale (v15): the whole plant, or pups, cuttings or seedlings off it. [plantName] is a
 * copy, so the record still reads after the plant is deleted and [dexId] goes null.
 * [kind] is a [SaleKind] name; [price] is the total, in rupiah.
 */
@Entity(
    tableName = "sale",
    foreignKeys = [
        ForeignKey(entity = DexEntity::class, parentColumns = ["id"], childColumns = ["dexId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("dexId")],
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dexId: Long?,
    val plantName: String,
    val kind: String,
    val quantity: Int,
    val price: Long,
    val soldOn: LocalDate,
    val note: String,
    val createdAt: Instant,
) {
    fun toModel() = Sale(
        id = id,
        dexId = dexId,
        plantName = plantName,
        kind = SaleKind.entries.firstOrNull { it.name == kind } ?: SaleKind.WHOLE,
        quantity = quantity,
        price = price,
        soldOn = soldOn,
        note = note,
    )
}

@Dao
interface SaleDao {
    /** Newest first. */
    @Query("SELECT * FROM sale ORDER BY soldOn DESC, id DESC")
    fun salesFlow(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sale WHERE id = :id")
    suspend fun sale(id: Long): SaleEntity?

    /** The sale that took the whole of plant [dexId], if there is one. */
    @Query("SELECT * FROM sale WHERE dexId = :dexId AND kind = 'WHOLE' LIMIT 1")
    suspend fun wholeSaleFor(dexId: Long): SaleEntity?

    @Insert
    suspend fun insert(sale: SaleEntity): Long

    @Update
    suspend fun update(sale: SaleEntity)

    @Query("DELETE FROM sale WHERE id = :id")
    suspend fun delete(id: Long)
}
