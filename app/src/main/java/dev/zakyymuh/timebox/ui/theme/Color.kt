package dev.zakyymuh.timebox.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * A paper palette, taken from the design.
 *
 * Light is warm — creams rather than white — and dark is cool, near-black rather than
 * grey. They are not inversions of each other on purpose: paper in daylight is warm,
 * and a screen at night should not be.
 */
internal object Paper {
    // Light: warm creams.
    val Cream = Color(0xFFFAF9F5)
    val CreamRaised = Color(0xFFFDFCF7)
    val CreamSunken = Color(0xFFF0EEE1)
    val CreamBorder = Color(0xFFECEBE6)

    // Dark: cool near-blacks.
    val Ink = Color(0xFF0E0E10)
    val InkRaised = Color(0xFF161519)
    val InkSunken = Color(0xFF1F1E24)
    val InkBorder = Color(0xFF27262D)
}

/** Warm greys. stone-400 and stone-200 carry most of the text in the design. */
internal object Stone {
    val S100 = Color(0xFFF5F5F4)
    val S200 = Color(0xFFE7E5E4)
    val S300 = Color(0xFFD6D3D1)
    val S400 = Color(0xFFA8A29E)
    val S500 = Color(0xFF78716C)
    val S600 = Color(0xFF57534E)
    val S700 = Color(0xFF44403C)
    val S800 = Color(0xFF292524)
    val S900 = Color(0xFF1C1917)
    val S950 = Color(0xFF0C0A09)
}

/** The one accent, used sparingly — a tick, an "offline" badge, and little else. */
internal val Emerald = Color(0xFF34D399)
