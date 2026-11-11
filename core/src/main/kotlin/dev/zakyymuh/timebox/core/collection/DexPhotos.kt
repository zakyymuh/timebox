package dev.zakyymuh.timebox.core.collection

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One photo in a plant's diary: the stored [file] name, a short [note], the day it was added. */
data class DexShot(
    val id: Long,
    val file: String,
    val note: String,
    val takenOn: LocalDate,
)

object DexPhotos {
    /** Short enough to sit in one bubble over the photo. */
    const val MAX_NOTE = 60

    private val day = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

    /** Trimmed, with runs of spaces and line breaks squeezed to one space, and cut to [MAX_NOTE]. */
    fun note(text: String): String = text.trim().replace(Regex("\\s+"), " ").take(MAX_NOTE).trimEnd()

    /** "11 Nov 2026 · 2 of 3"; just the day when there is only one photo. */
    fun caption(takenOn: LocalDate, index: Int, count: Int): String {
        val date = takenOn.format(day)
        return if (count > 1) "$date · ${index + 1} of $count" else date
    }
}
