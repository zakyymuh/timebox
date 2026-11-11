package dev.zakyymuh.motor.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Palette

/**
 * Motor's colours: a deep metallic red, like Mazda's Soul Red on a Mazda3 hatchback,
 * and chrome silver instead of papacti's grey-green. In this palette `green` is the
 * red and `greenDeep` the dark crimson. Errors are burnt orange, so "overdue" never
 * looks like an ordinary red accent.
 */
val SoulRed = Palette(
    page = Color(0xFFFFFFFF),
    card = Color(0xFFF1F2F4),
    ink = Color(0xFF18181B),
    inkSoft = Color(0xFF6B6F76),
    pastel = Color(0xFFF8DADD),
    green = Color(0xFFB3121D),
    greenDeep = Color(0xFF6E0A12),
    error = Color(0xFFD9480F),
)

/** Soul Red at night: near-black page, light ink, a brighter red so it still glows. */
val SoulRedDark = Palette(
    page = Color(0xFF111113),
    card = Color(0xFF1E1F23),
    ink = Color(0xFFECECEF),
    inkSoft = Color(0xFF9DA0A7),
    pastel = Color(0xFF3D1519),
    green = Color(0xFFE0313C),
    greenDeep = Color(0xFFF6B8BD),
    error = Color(0xFFF08A4B),
)

private val LightChrome = Brush.verticalGradient(
    0f to Color(0xFFF7F8FA),
    0.5f to Color(0xFFC8CCD2),
    1f to Color(0xFFEEF0F3),
)

private val DarkChrome = Brush.verticalGradient(
    0f to Color(0xFF4A4D53),
    0.5f to Color(0xFF26282C),
    1f to Color(0xFF3F4247),
)

/**
 * Polished chrome, light at the top and bottom with a darker band through the middle;
 * gunmetal in dark mode.
 */
val Chrome: Brush get() = if (Fresh.Dark) DarkChrome else LightChrome
