package dev.zakyymuh.timebox.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import dev.zakyymuh.timebox.core.light.LightBand
import dev.zakyymuh.timebox.core.light.LightLevel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant

/** One Light check reading the owner saved: how bright, what kind of spot, and when. */
@Entity(tableName = "light_readings")
data class LightReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lux: Float,
    /** A [dev.zakyymuh.timebox.core.light.LightBand] name, e.g. "Bright". */
    val band: String,
    val takenAt: Instant,
)

/** A saved reading as the list shows it: how bright, and what kind of spot. */
data class SavedLight(val id: Long, val lux: Float, val band: LightBand)

@Dao
interface LightReadingDao {
    @Insert
    suspend fun insert(reading: LightReadingEntity): Long

    @Query("SELECT * FROM light_readings ORDER BY takenAt DESC, id DESC")
    suspend fun all(): List<LightReadingEntity>

    @Query("SELECT * FROM light_readings ORDER BY takenAt DESC, id DESC")
    fun allFlow(): Flow<List<LightReadingEntity>>
}

class LightRepository(private val dao: LightReadingDao, private val clock: Clock) {

    /** Every saved reading, newest first; an unknown band name reads as [LightBand.Dark]. */
    fun readingsFlow(): Flow<List<SavedLight>> = dao.allFlow().map { rows ->
        rows.map { row ->
            SavedLight(row.id, row.lux, LightBand.entries.firstOrNull { it.name == row.band } ?: LightBand.Dark)
        }
    }

    /**
     * Saves [lux] as it is now; a negative or broken reading counts as 0. Returns the new id.
     * Finishes even if the caller is cancelled, so leaving the screen right after a tap keeps it.
     */
    suspend fun save(lux: Float): Long = withContext(NonCancellable) {
        val clean = if (lux.isNaN()) 0f else lux.coerceAtLeast(0f)
        dao.insert(LightReadingEntity(lux = clean, band = LightLevel.band(clean).name, takenAt = clock.instant()))
    }
}
