package dev.zakyymuh.timebox.data

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/**
 * Dates are stored as ISO text so they sort and compare correctly in SQL; instants as
 * epoch millis. Both are absolute, so nothing shifts if the device timezone changes.
 */
class Converters {

    @TypeConverter
    fun dateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}
