package dev.zakyymuh.timebox.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.zakyymuh.timebox.R

/**
 * Inter and JetBrains Mono, bundled rather than downloaded — the app has no internet
 * permission, so downloadable fonts are not an option.
 *
 * Both are variable fonts, so one file covers every weight. That needs API 26, which is
 * why minSdk is 26.
 */
private fun interAt(weight: FontWeight) = Font(
    R.font.inter_regular,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private fun monoAt(weight: FontWeight) = Font(
    R.font.jetbrains_mono_regular,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

internal val Inter = FontFamily(
    interAt(FontWeight.Light),
    interAt(FontWeight.Normal),
    interAt(FontWeight.Medium),
    interAt(FontWeight.SemiBold),
    interAt(FontWeight.Bold),
)

internal val Mono = FontFamily(
    monoAt(FontWeight.Normal),
    monoAt(FontWeight.Medium),
    monoAt(FontWeight.SemiBold),
)

/**
 * The design's scale: small type, tight tracking on headings, wide tracking on the
 * little uppercase labels.
 */
internal val DailyTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        letterSpacing = (-0.5).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        letterSpacing = (-0.1).sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
    ),
    // The 10px uppercase labels in the design — "ONE DAY AT A TIME", badges.
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 1.2.sp,
    ),
)

/** Dates and counters are mono in the design — they read as data, not prose. */
internal val MonoLabel = TextStyle(
    fontFamily = Mono,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    letterSpacing = 0.2.sp,
)

internal val MonoSmall = TextStyle(
    fontFamily = Mono,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    letterSpacing = 0.2.sp,
)
