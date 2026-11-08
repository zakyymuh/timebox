package dev.zakyymuh.timebox.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import dev.zakyymuh.timebox.core.collection.DexEntry
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/**
 * A plant in the collection ([owned]) or on the wishlist. Separate from watering plants on
 * purpose: the owner keeps plants here they never schedule. [photo] is a file name under
 * filesDir/photos, never a path, so a restore onto another phone still finds it.
 */
@Entity(tableName = "dex", indices = [Index(value = ["number"], unique = true)])
data class DexEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: Int,
    val name: String,
    val species: String,
    val owned: Boolean,
    val photo: String?,
    val acquired: LocalDate?,
    val note: String,
    val createdAt: Instant,
    /** Price seen for a wish, in rupiah (v5). */
    val priceLow: Long? = null,
    val priceHigh: Long? = null,
) {
    fun toModel() = DexEntry(id, number, name, species, owned, photo, acquired, note, priceLow, priceHigh)
}

@Dao
interface DexDao {
    @Query("SELECT * FROM dex ORDER BY number")
    fun entriesFlow(): Flow<List<DexEntity>>

    @Query("SELECT * FROM dex ORDER BY number")
    suspend fun entries(): List<DexEntity>

    @Query("SELECT * FROM dex WHERE id = :id")
    suspend fun entry(id: Long): DexEntity?

    @Query("SELECT number FROM dex")
    suspend fun numbers(): List<Int>

    @Insert
    suspend fun insert(entry: DexEntity): Long

    @Update
    suspend fun update(entry: DexEntity)

    @Query("DELETE FROM dex WHERE id = :id")
    suspend fun delete(id: Long)
}
