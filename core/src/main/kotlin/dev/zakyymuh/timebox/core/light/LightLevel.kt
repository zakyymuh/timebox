package dev.zakyymuh.timebox.core.light

import kotlin.math.roundToInt

/** How much light a spot gets, from a lux reading, and what lives happily there. */
enum class LightBand(val title: String, val verdict: String, val fromLux: Float) {
    Dark("Too dark", "Too dark for most plants to grow. Fine for a short stay, not a home.", 0f),
    Low("Low light", "Shade lovers only: snake plant, ZZ, pothos. Too dim for cacti.", 500f),
    Medium("Medium light", "Good for most leafy houseplants. Cacti will stretch and lean here.", 2_500f),
    Bright("Bright light", "Bright indirect light. Many succulents are happy; cacti like a bit more.", 10_000f),
    Sun("Direct sun", "Full sun: cactus heaven. Ease leafy plants in slowly or they scorch.", 20_000f),
}

object LightLevel {
    /** How much of each new reading counts, so the number settles instead of flickering. */
    const val SMOOTHING = 0.25f

    fun band(lux: Float): LightBand =
        LightBand.entries.lastOrNull { lux >= it.fromLux } ?: LightBand.Dark

    /** Whole lux with thousands separated, rounded to the nearest 100 from 1,000 up. */
    fun format(lux: Float): String {
        val whole = lux.coerceAtLeast(0f).roundToInt()
        val shown = if (whole >= 1_000) (whole + 50) / 100 * 100 else whole
        return "%,d".format(java.util.Locale.US, shown)
    }

    fun smooth(previous: Float?, reading: Float): Float =
        if (previous == null) reading else previous + (reading - previous) * SMOOTHING
}
