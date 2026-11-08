package dev.zakyymuh.timebox.core.motor

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** One motorcycle service: when, where, what it cost, and optionally what was done and the odometer. */
data class ServiceRecord(
    val id: Long,
    val date: LocalDate,
    val place: String,
    val price: Long?,
    val work: String,
    val odometerKm: Int?,
)

/** What the edit screen holds. */
data class ServiceForm(
    val date: LocalDate,
    val place: String = "",
    val price: Long? = null,
    val work: String = "",
    val odometerKm: Int? = null,
) {
    fun isValid(today: LocalDate): Boolean =
        place.isNotBlank() && place.trim().length <= MAX_PLACE && !date.isAfter(today) &&
            (price == null || price in 0..MAX_PRICE) && (odometerKm == null || odometerKm in 0..MAX_KM)

    companion object {
        const val MAX_PLACE = 60
        const val MAX_WORK = 300
        const val MAX_PRICE = 999_999_999L
        const val MAX_KM = 9_999_999
    }
}

object ServiceLog {
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    fun newestFirst(records: List<ServiceRecord>): List<ServiceRecord> =
        records.sortedWith(compareByDescending<ServiceRecord> { it.date }.thenByDescending { it.id })

    /** The dashboard line: "Last: 12 Oct at AHASS · 27 days ago". */
    fun summary(records: List<ServiceRecord>, today: LocalDate): String {
        val last = newestFirst(records).firstOrNull() ?: return "No service logged yet"
        val days = ChronoUnit.DAYS.between(last.date, today)
        return when (days) {
            0L -> "Last: today at ${last.place}"
            1L -> "Last: yesterday at ${last.place}"
            else -> "Last: ${last.date.format(dayMonth)} at ${last.place} · $days days ago"
        }
    }

    fun spentIn(records: List<ServiceRecord>, year: Int): Long =
        records.filter { it.date.year == year }.sumOf { it.price ?: 0L }

    /** Km ridden between the two latest services that both have an odometer reading. */
    fun kmSincePrevious(records: List<ServiceRecord>): Int? {
        val readings = newestFirst(records).mapNotNull { it.odometerKm }
        return if (readings.size < 2) null else readings[0] - readings[1]
    }
}

object Money {
    /** 1250000 → "Rp 1.250.000". */
    fun rupiah(value: Long): String = "Rp " + "%,d".format(Locale.ROOT, value).replace(',', '.')
}
