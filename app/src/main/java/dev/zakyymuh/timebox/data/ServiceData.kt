package dev.zakyymuh.timebox.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import dev.zakyymuh.timebox.core.motor.ServiceForm
import dev.zakyymuh.timebox.core.motor.ServiceRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/** One motorcycle service (v6). */
@Entity(tableName = "service")
data class ServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val place: String,
    val price: Long?,
    val work: String,
    val odometer: Int?,
    val createdAt: Instant,
) {
    fun toModel() = ServiceRecord(id, date, place, price, work, odometer)
}

@Dao
interface ServiceDao {
    @Query("SELECT * FROM service ORDER BY date DESC, id DESC")
    fun recordsFlow(): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM service WHERE id = :id")
    suspend fun record(id: Long): ServiceEntity?

    /** Returns the new row id on insert, -1 on update. */
    @Upsert
    suspend fun upsert(record: ServiceEntity): Long

    @Query("DELETE FROM service WHERE id = :id")
    suspend fun delete(id: Long)
}

/** The only door to the motorcycle service log. */
class ServiceRepository(private val dao: ServiceDao, private val clock: Clock) {

    fun todayDate(): LocalDate = LocalDate.now(clock)

    fun recordsFlow(): Flow<List<ServiceRecord>> = dao.recordsFlow().map { it.map(ServiceEntity::toModel) }

    suspend fun record(id: Long): ServiceRecord? = dao.record(id)?.toModel()

    /** Saves [form] as record [id], or as a new one when [id] is 0. Returns the record's id. */
    suspend fun save(id: Long, form: ServiceForm): Long {
        require(form.isValid(todayDate())) { "invalid service form" }
        val existing = if (id != 0L) dao.record(id) else null
        val rowId = dao.upsert(
            ServiceEntity(
                id = id,
                date = form.date,
                place = form.place.trim(),
                price = form.price,
                work = form.work.trim(),
                odometer = form.odometerKm,
                createdAt = existing?.createdAt ?: clock.instant(),
            ),
        )
        return if (id != 0L) id else rowId
    }

    suspend fun delete(id: Long) = dao.delete(id)
}
