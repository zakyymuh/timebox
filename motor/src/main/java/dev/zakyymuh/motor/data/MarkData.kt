package dev.zakyymuh.motor.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import dev.zakyymuh.timebox.core.lastdone.Event
import dev.zakyymuh.timebox.core.lastdone.LastDone
import dev.zakyymuh.timebox.core.lastdone.Mark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/**
 * One time a chore was done (v5): "Tambah angin, now". [vehicleId] is null for one that
 * belongs to no vehicle in particular, like getting a SIM for the car.
 */
@Entity(
    tableName = "mark",
    foreignKeys = [
        ForeignKey(entity = VehicleEntity::class, parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("vehicleId")],
)
data class MarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val at: Instant,
    val vehicleId: Long?,
)

@Dao
interface MarkDao {
    @Query("SELECT * FROM mark")
    fun marksFlow(): Flow<List<MarkEntity>>

    @Query("SELECT * FROM mark")
    suspend fun marks(): List<MarkEntity>

    @Insert
    suspend fun insert(mark: MarkEntity)

    @Query("DELETE FROM mark WHERE id = :id")
    suspend fun delete(id: Long)
}

/** What to log when "Did it" is tapped on an event already in the list. */
data class MarkTarget(val name: String, val vehicleId: Long?)

/** The events, and what each one logs again. */
data class MarkLog(val events: List<Event> = emptyList(), val targets: Map<String, MarkTarget> = emptyMap())

/**
 * "When did I last …?" for mobility chores. A vehicle's chore carries the vehicle's
 * name — "Cuci motor (Vario)" — so washing two vehicles counts as two separate things.
 */
class MarkRepository(
    private val dao: MarkDao,
    private val vehicles: VehicleRepository,
    private val clock: Clock,
) {
    fun todayDate(): LocalDate = LocalDate.now(clock)

    fun logFlow(): Flow<MarkLog> =
        combine(dao.marksFlow(), vehicles.vehiclesFlow()) { rows, all ->
            val names = all.associate { it.id to it.name }
            val labelled = rows.map { it to label(it, names) }
            MarkLog(
                events = LastDone.summarise(labelled.map { (row, label) -> Mark(label, row.at) }, clock.zone),
                // The most recent spelling wins, as it does for the event's name.
                targets = labelled.sortedBy { it.first.at }
                    .associate { (row, label) -> LastDone.key(label) to MarkTarget(row.name, row.vehicleId) },
            )
        }

    /** Logs [name] as done now, for [vehicleId] or for no vehicle. */
    suspend fun markDone(name: String, vehicleId: Long?) {
        if (name.isBlank()) return
        dao.insert(MarkEntity(name = name.trim(), at = clock.instant(), vehicleId = vehicleId))
    }

    /** Takes back the latest mark of the event keyed [key]. */
    suspend fun undoLastMark(key: String) {
        val names = vehicles.vehicles().associate { it.id to it.name }
        dao.marks()
            .filter { LastDone.key(label(it, names)) == key }
            .maxWithOrNull(compareBy<MarkEntity> { it.at }.thenBy { it.id })
            ?.let { dao.delete(it.id) }
    }

    private fun label(row: MarkEntity, vehicleNames: Map<Long, String>): String {
        val vehicle = row.vehicleId?.let(vehicleNames::get)
        return if (vehicle == null) row.name.trim() else "${row.name.trim()} ($vehicle)"
    }
}
