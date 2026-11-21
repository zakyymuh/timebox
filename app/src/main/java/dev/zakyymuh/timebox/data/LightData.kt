package dev.zakyymuh.timebox.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import dev.zakyymuh.timebox.core.light.LightLevel
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

@Dao
interface LightReadingDao {
    @Insert
    suspend fun insert(reading: LightReadingEntity): Long

    @Query("SELECT * FROM light_readings ORDER BY takenAt DESC, id DESC")
    suspend fun all(): List<LightReadingEntity>
}

class LightRepository(private val dao: LightReadingDao, private val clock: Clock) {

    /** Saves [lux] as it is now; a negative or broken reading counts as 0. Returns the new id. */
    suspend fun save(lux: Float): Long {
        val clean = if (lux.isNaN()) 0f else lux.coerceAtLeast(0f)
        return dao.insert(LightReadingEntity(lux = clean, band = LightLevel.band(clean).name, takenAt = clock.instant()))
    }
}
