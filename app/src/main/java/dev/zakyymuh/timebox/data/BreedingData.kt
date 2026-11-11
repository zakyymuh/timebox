package dev.zakyymuh.timebox.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import dev.zakyymuh.timebox.core.collection.Breeding
import dev.zakyymuh.timebox.core.collection.BreedingForm
import dev.zakyymuh.timebox.core.collection.Cross
import dev.zakyymuh.timebox.core.collection.CrossOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/**
 * A cross of two collection plants (v11). The parents' names are copied in, and their ids
 * turn null if a parent is removed, so a cross outlives its parents. A removed cross is only
 * hidden ([removedAt], v12), so its code stays taken and is never given to another.
 */
@Entity(
    tableName = "breeding",
    foreignKeys = [
        ForeignKey(entity = DexEntity::class, parentColumns = ["id"], childColumns = ["motherId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = DexEntity::class, parentColumns = ["id"], childColumns = ["fatherId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index(value = ["code"], unique = true), Index("motherId"), Index("fatherId")],
)
data class BreedingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val date: LocalDate,
    val motherId: Long?,
    val fatherId: Long?,
    val motherName: String,
    val fatherName: String,
    val createdAt: Instant,
    /** A [CrossOutcome] name (v12). */
    @ColumnInfo(defaultValue = "Waiting") val outcome: String = CrossOutcome.Waiting.name,
    val removedAt: Instant? = null,
) {
    fun toModel() = Cross(id, code, name, date, motherId, fatherId, motherName, fatherName, CrossOutcome.from(outcome))
}

@Dao
interface BreedingDao {
    @Query("SELECT * FROM breeding WHERE removedAt IS NULL ORDER BY date DESC, id DESC")
    fun crossesFlow(): Flow<List<BreedingEntity>>

    @Query("SELECT * FROM breeding WHERE id = :id AND removedAt IS NULL")
    suspend fun cross(id: Long): BreedingEntity?

    /** Every code ever given, removed crosses included. */
    @Query("SELECT code FROM breeding")
    suspend fun codes(): List<String>

    @Query("UPDATE breeding SET outcome = :outcome WHERE id = :id")
    suspend fun setOutcome(id: Long, outcome: String)

    @Query("UPDATE breeding SET removedAt = :at WHERE id = :id")
    suspend fun remove(id: Long, at: Instant)

    @Insert
    suspend fun insert(cross: BreedingEntity): Long
}

class BreedingRepository(
    private val dao: BreedingDao,
    private val dex: DexDao,
    private val clock: Clock,
) {
    /** Code handout is read-then-insert; one at a time so two saves never share a code. */
    private val numbering = Mutex()

    fun todayDate(): LocalDate = LocalDate.now(clock)

    fun crossesFlow(): Flow<List<Cross>> = dao.crossesFlow().map { it.map(BreedingEntity::toModel) }

    suspend fun cross(id: Long): Cross? = dao.cross(id)?.toModel()

    suspend fun setOutcome(id: Long, outcome: CrossOutcome) = dao.setOutcome(id, outcome.name)

    /** Hides the cross from every list; its code stays taken. */
    suspend fun remove(id: Long) = dao.remove(id, clock.instant())

    /** Saves a cross on [date]; null if the form is invalid or a parent is gone. Returns the new id. */
    suspend fun add(form: BreedingForm, date: LocalDate): Long? {
        if (!form.isValid) return null
        val mother = dex.entry(form.motherId ?: return null) ?: return null
        val father = dex.entry(form.fatherId ?: return null) ?: return null
        return numbering.withLock {
            dao.insert(
                BreedingEntity(
                    code = Breeding.code(date, dao.codes()),
                    name = form.name.trim(),
                    date = date,
                    motherId = mother.id,
                    fatherId = father.id,
                    motherName = mother.name,
                    fatherName = father.name,
                    createdAt = clock.instant(),
                ),
            )
        }
    }
}
