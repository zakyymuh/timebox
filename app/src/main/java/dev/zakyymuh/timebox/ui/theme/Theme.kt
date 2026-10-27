package dev.zakyymuh.timebox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightPaper = lightColorScheme(
    background = Paper.Cream,
    onBackground = Stone.S900,
    surface = Paper.Cream,
    onSurface = Stone.S900,
    surfaceVariant = Paper.CreamSunken,
    onSurfaceVariant = Stone.S500,
    surfaceContainer = Paper.CreamRaised,
    outline = Paper.CreamBorder,
    outlineVariant = Paper.CreamBorder,
    primary = Stone.S900,
    onPrimary = Paper.Cream,
    secondary = Stone.S500,
    onSecondary = Paper.Cream,
    tertiary = Emerald,
)

private val DarkPaper = darkColorScheme(
    background = Paper.Ink,
    onBackground = Stone.S200,
    surface = Paper.Ink,
    onSurface = Stone.S200,
    surfaceVariant = Paper.InkSunken,
    onSurfaceVariant = Stone.S400,
    surfaceContainer = Paper.InkRaised,
    outline = Paper.InkBorder,
    outlineVariant = Paper.InkBorder,
    primary = Stone.S100,
    onPrimary = Paper.Ink,
    secondary = Stone.S400,
    onSecondary = Paper.Ink,
    tertiary = Emerald,
)

/**
 * Deliberately NOT using dynamic colour. The design is a specific paper palette, and
 * Material You would repaint it with the user's wallpaper.
 */
@Composable
fun DailyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkPaper else LightPaper,
        typography = DailyTypography,
        content = content,
    )
}
