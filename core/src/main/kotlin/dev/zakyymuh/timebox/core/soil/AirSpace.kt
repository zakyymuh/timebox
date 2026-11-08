package dev.zakyymuh.timebox.core.soil

import java.time.LocalDate

/**
 * One air-filled porosity test: a pot of [potMl] filled with mix, soaked, then drained;
 * [drainedMl] ran out. [pouredMl], if measured, is the water it took to soak the mix.
 */
data class AirTest(
    val id: Long,
    val date: LocalDate,
    val mix: String,
    val potMl: Int,
    val drainedMl: Int,
    val pouredMl: Int?,
) {
    val result: AirResult get() = AirSpace.result(potMl, drainedMl, pouredMl)
}

/** Air space, and when poured water was measured, total porosity and water holding — all % of pot volume. */
data class AirResult(val airPct: Int, val totalPct: Int?, val waterPct: Int?) {
    val verdict: String get() = AirSpace.verdict(airPct)
}

/** What the calculator holds; numbers are null while the field is empty. */
data class AirTestForm(
    val mix: String = "",
    val potMl: Int? = null,
    val drainedMl: Int? = null,
    val pouredMl: Int? = null,
) {
    val isValid: Boolean
        get() {
            val pot = potMl ?: return false
            val drained = drainedMl ?: return false
            return pot in 1..MAX_ML && drained in 0..pot && mix.trim().length <= MAX_MIX &&
                (pouredMl == null || pouredMl in drained..pot)
        }

    val result: AirResult? get() = if (isValid) AirSpace.result(potMl!!, drainedMl!!, pouredMl) else null

    companion object {
        const val MAX_ML = 100_000
        const val MAX_MIX = 40
    }
}

object AirSpace {
    fun result(potMl: Int, drainedMl: Int, pouredMl: Int?): AirResult {
        val air = pct(drainedMl, potMl)
        val total = pouredMl?.let { pct(it, potMl) }
        return AirResult(airPct = air, totalPct = total, waterPct = pouredMl?.let { pct(it - drainedMl, potMl) })
    }

    /** What the air space means for cacti and succulents. */
    fun verdict(airPct: Int): String = when {
        airPct < 10 -> "Dense: holds a lot of water and little air. Add grit (pumice, pasir malang) before using it for cacti."
        airPct < 20 -> "Moderate: fine for leafy plants and sansevieria, a bit wet for most cacti."
        airPct <= 35 -> "Airy: good for most cacti and succulents."
        else -> "Very airy: dries fast. Good for rot-prone plants like Astrophytum; check sooner in hot weather."
    }

    private fun pct(part: Int, whole: Int): Int = Math.round(part * 100.0 / whole).toInt()
}

sealed interface SampleStatus {
    /** No test saved yet, so nothing to re-check. */
    data object NotStarted : SampleStatus
    data class Waiting(val left: Int) : SampleStatus
    data class Due(val wateringsSince: Int) : SampleStatus
}

/** Re-test the sample every [EVERY] waterings (of any plant), to see the mix settle or break down. */
object SampleCheck {
    const val EVERY = 2

    fun status(hasTest: Boolean, wateringsSince: Int): SampleStatus = when {
        !hasTest -> SampleStatus.NotStarted
        wateringsSince >= EVERY -> SampleStatus.Due(wateringsSince)
        else -> SampleStatus.Waiting(EVERY - wateringsSince)
    }

    fun label(status: SampleStatus): String = when (status) {
        SampleStatus.NotStarted -> "Test a sample of your mix to start"
        is SampleStatus.Waiting ->
            if (status.left == 1) "Next check after 1 more watering" else "Next check after ${status.left} more waterings"
        is SampleStatus.Due -> "Time to check your soil sample · ${status.wateringsSince} waterings since the last check"
    }
}
