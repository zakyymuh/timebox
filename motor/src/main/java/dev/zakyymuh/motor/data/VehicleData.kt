package dev.zakyymuh.motor.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.zakyymuh.timebox.core.motor.Plate
import dev.zakyymuh.timebox.core.motor.Vehicle
import dev.zakyymuh.timebox.core.motor.VehicleForm
import dev.zakyymuh.timebox.core.motor.VehicleType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant

/** One of your vehicles (v2). */
@Entity(tableName = "vehicle")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: VehicleType,
    val name: String,
    val plate: String,
    val photo: String?,
    val createdAt: Instant,
) {
    fun toModel() = Vehicle(id, type, name, plate, photo)
}

@Dao
abstract class VehicleDao {
    @Query("SELECT * FROM vehicle ORDER BY createdAt, id")
    abstract fun vehiclesFlow(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicle WHERE id = :id")
    abstract fun vehicleFlow(id: Long): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicle WHERE id = :id")
    abstract suspend fun vehicle(id: Long): VehicleEntity?

    /** Returns the new row id on insert, -1 on update. */
    @Upsert
    abstract suspend fun upsert(vehicle: VehicleEntity): Long

    @Query("DELETE FROM vehicle WHERE id = :id")
    abstract suspend fun deleteVehicle(id: Long)

    @Query("DELETE FROM service WHERE vehicleId = :id")
    abstract suspend fun deleteServices(id: Long)

    @Query("DELETE FROM expiry WHERE vehicleId = :id")
    abstract suspend fun deleteExpiries(id: Long)

    /**
     * The foreign key cascades too, but saying it outright keeps a delete correct even
     * if foreign keys are ever switched off on a connection.
     */
    @Transaction
    open suspend fun delete(id: Long) {
        deleteServices(id)
        deleteExpiries(id)
        deleteVehicle(id)
    }
}

/** The only door to your vehicles. */
class VehicleRepository(
    private val dao: VehicleDao,
    private val clock: Clock,
) {
    fun vehiclesFlow(): Flow<List<Vehicle>> = dao.vehiclesFlow().map { rows -> rows.map(VehicleEntity::toModel) }

    fun vehicleFlow(id: Long): Flow<Vehicle?> = dao.vehicleFlow(id).map { it?.toModel() }

    suspend fun vehicle(id: Long): Vehicle? = dao.vehicle(id)?.toModel()

    /** Saves [form] as vehicle [id], or as a new one when [id] is 0. Returns the vehicle's id. */
    suspend fun save(id: Long, form: VehicleForm): Long {
        require(form.isValid()) { "invalid vehicle form" }
        val existing = if (id != 0L) dao.vehicle(id) else null
        val rowId = dao.upsert(
            VehicleEntity(
                id = id,
                type = form.type,
                name = form.name.trim(),
                plate = Plate.normalise(form.plate),
                photo = form.photo,
                createdAt = existing?.createdAt ?: clock.instant(),
            ),
        )
        return if (id != 0L) id else rowId
    }

    /** Deletes the vehicle, its service log and its dates. */
    suspend fun delete(id: Long) = dao.delete(id)
}
