package dev.zakyymuh.motor.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.zakyymuh.timebox.core.motor.ExpiryReminders
import dev.zakyymuh.timebox.core.motor.Reminder
import dev.zakyymuh.timebox.core.motor.SentReminder
import java.time.Clock
import java.time.LocalDate

/** A reminder that has gone out (v4), so it never goes out twice. */
@Entity(
    tableName = "reminder_sent",
    primaryKeys = ["expiryId", "date", "tag"],
    foreignKeys = [
        ForeignKey(entity = ExpiryEntity::class, parentColumns = ["id"], childColumns = ["expiryId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class ReminderSentEntity(
    val expiryId: Long,
    val date: LocalDate,
    /** "30d" or "7d". */
    val tag: String,
)

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminder_sent")
    suspend fun sent(): List<ReminderSentEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markSent(row: ReminderSentEntity)
}

/** Which expiry reminders are due today, and remembering the ones posted. */
class ReminderRepository(
    private val reminders: ReminderDao,
    private val expiries: ExpiryRepository,
    private val vehicles: VehicleRepository,
    private val clock: Clock,
) {
    suspend fun dueToday(): List<Reminder> {
        val all = expiries.expiries()
        val byId = vehicles.vehicles().associateBy { it.id }
        val titles = all.associate { e -> e.id to e.title(e.vehicleId?.let(byId::get)?.name) }
        val sent = reminders.sent().map { SentReminder(it.expiryId, it.date, it.tag) }.toSet()
        return ExpiryReminders.due(all, titles, LocalDate.now(clock), sent)
    }

    suspend fun markSent(reminder: Reminder) =
        reminders.markSent(ReminderSentEntity(reminder.expiryId, reminder.date, reminder.stage.tag))
}
