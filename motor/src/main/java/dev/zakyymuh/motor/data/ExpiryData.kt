package dev.zakyymuh.motor.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import dev.zakyymuh.timebox.core.motor.Expiries
import dev.zakyymuh.timebox.core.motor.Expiry
import dev.zakyymuh.timebox.core.motor.ExpiryForm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/**
 * One date something runs out (v3). One row per paper rather than a column per kind, so a
 * new kind of paper needs no migration. [vehicleId] is null for your own papers.
 */
@Entity(
    tableName = "expiry",
    foreignKeys = [
        ForeignKey(entity = VehicleEntity::class, parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("vehicleId")],
)
data class ExpiryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long?,
    val name: String,
    val date: LocalDate,
    val renewMonths: Int,
    val createdAt: Instant,
) {
    fun toModel() = Expiry(id, vehicleId, name, date, renewMonths)
}

@Dao
interface ExpiryDao {
    @Query("SELECT * FROM expiry")
    fun allFlow(): Flow<List<ExpiryEntity>>

    @Query("SELECT * FROM expiry")
    suspend fun all(): List<ExpiryEntity>

    @Query("SELECT * FROM expiry WHERE id = :id")
    suspend fun expiry(id: Long): ExpiryEntity?

    @Upsert
    suspend fun upsert(expiry: ExpiryEntity): Long

    @Query("UPDATE expiry SET date = :date WHERE id = :id")
    suspend fun setDate(id: Long, date: LocalDate)

    @Query("DELETE FROM expiry WHERE id = :id")
    suspend fun delete(id: Long)
}

/** The only door to the expiry dates. */
class ExpiryRepository(private val dao: ExpiryDao, private val clock: Clock) {

    fun todayDate(): LocalDate = LocalDate.now(clock)

    /** Soonest first. */
    fun expiriesFlow(): Flow<List<Expiry>> = dao.allFlow().map { rows -> Expiries.soonestFirst(rows.map(ExpiryEntity::toModel)) }

    suspend fun expiries(): List<Expiry> = Expiries.soonestFirst(dao.all().map(ExpiryEntity::toModel))

    suspend fun expiry(id: Long): Expiry? = dao.expiry(id)?.toModel()

    /** Saves [form] as expiry [id], or as a new one when [id] is 0. Returns its id. */
    suspend fun save(id: Long, form: ExpiryForm): Long {
        require(form.isValid()) { "invalid expiry form" }
        val existing = if (id != 0L) dao.expiry(id) else null
        val rowId = dao.upsert(
            ExpiryEntity(
                id = id,
                vehicleId = form.vehicleId,
                name = form.name.trim(),
                date = form.date,
                renewMonths = form.renewMonths,
                createdAt = existing?.createdAt ?: clock.instant(),
            ),
        )
        return if (id != 0L) id else rowId
    }

    /** Renewed: the date moves on by one period. Returns the new date. */
    suspend fun renew(id: Long): LocalDate? {
        val expiry = dao.expiry(id)?.toModel() ?: return null
        return Expiries.renewed(expiry).also { dao.setDate(id, it) }
    }

    /** Takes back a renewal tapped by mistake. */
    suspend fun setDate(id: Long, date: LocalDate) = dao.setDate(id, date)

    suspend fun delete(id: Long) = dao.delete(id)
}
