package dev.zakyymuh.timebox.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import dev.zakyymuh.timebox.core.plants.HistoryNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/** Something the owner wrote down for the history: "Repotted the Monstera", on a day. */
@Entity(tableName = "note")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val date: LocalDate,
    val createdAt: Instant,
) {
    fun toModel() = HistoryNote(id = id, text = text, date = date)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM note ORDER BY date DESC, id DESC")
    fun notesFlow(): Flow<List<NoteEntity>>

    @Insert
    suspend fun insert(note: NoteEntity): Long
}

class NoteRepository(private val dao: NoteDao, private val clock: Clock) {

    fun notesFlow(): Flow<List<HistoryNote>> = dao.notesFlow().map { it.map(NoteEntity::toModel) }

    /** Blank text is ignored; returns the new note's id, or null. */
    suspend fun add(text: String, date: LocalDate): Long? {
        val clean = text.trim().take(MAX_TEXT)
        if (clean.isEmpty()) return null
        return dao.insert(NoteEntity(text = clean, date = date, createdAt = clock.instant()))
    }

    companion object {
        const val MAX_TEXT = 200
    }
}
