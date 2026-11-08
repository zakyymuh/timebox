package dev.zakyymuh.timebox.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import dev.zakyymuh.timebox.core.soil.AirTest
import dev.zakyymuh.timebox.core.soil.AirTestForm
import dev.zakyymuh.timebox.core.soil.SampleCheck
import dev.zakyymuh.timebox.core.soil.SampleStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/** One air-filled porosity test (v7). [createdAt] is when it was saved: waterings after it count toward the next check. */
@Entity(tableName = "air_test")
data class AirTestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val mix: String,
    val potMl: Int,
    val drainedMl: Int,
    val pouredMl: Int?,
    val createdAt: Instant,
) {
    fun toModel() = AirTest(id, date, mix, potMl, drainedMl, pouredMl)
}

@Dao
interface AirTestDao {
    @Query("SELECT * FROM air_test ORDER BY createdAt DESC")
    fun testsFlow(): Flow<List<AirTestEntity>>

    @Query("SELECT MAX(createdAt) FROM air_test")
    fun lastTestAtFlow(): Flow<Instant?>

    @Query("SELECT MAX(createdAt) FROM air_test")
    suspend fun lastTestAt(): Instant?

    @Insert
    suspend fun insert(test: AirTestEntity): Long

    @Query("DELETE FROM air_test WHERE id = :id")
    suspend fun delete(id: Long)
}

/** Soil tests, and whether the sample is due for a re-check (every 2 waterings of any plant). */
@OptIn(ExperimentalCoroutinesApi::class)
class SoilRepository(
    private val dao: AirTestDao,
    private val plants: PlantDao,
    private val clock: Clock,
) {
    fun testsFlow(): Flow<List<AirTest>> = dao.testsFlow().map { it.map(AirTestEntity::toModel) }

    fun statusFlow(): Flow<SampleStatus> = dao.lastTestAtFlow().flatMapLatest { last ->
        if (last == null) {
            flowOf(SampleStatus.NotStarted)
        } else {
            plants.wateringsAfterFlow(last).map { SampleCheck.status(hasTest = true, wateringsSince = it) }
        }
    }

    /** For the 18:00 receiver. */
    suspend fun statusNow(): SampleStatus {
        val last = dao.lastTestAt() ?: return SampleStatus.NotStarted
        return SampleCheck.status(hasTest = true, wateringsSince = plants.wateringsAfter(last))
    }

    suspend fun save(form: AirTestForm): Long {
        require(form.isValid) { "invalid air test" }
        return dao.insert(
            AirTestEntity(
                date = LocalDate.now(clock),
                mix = form.mix.trim(),
                potMl = form.potMl!!,
                drainedMl = form.drainedMl!!,
                pouredMl = form.pouredMl,
                createdAt = clock.instant(),
            ),
        )
    }

    suspend fun delete(id: Long) = dao.delete(id)
}
