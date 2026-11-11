package dev.zakyymuh.timebox.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Built when first drawn, so it picks up the palette the app chose with [Fresh.use]. */
private fun freshScheme(): ColorScheme = (if (Fresh.Dark) darkColorScheme() else lightColorScheme()).copy(
    background = Fresh.Page,
    onBackground = Fresh.Ink,
    surface = Fresh.Page,
    onSurface = Fresh.Ink,
    surfaceVariant = Fresh.Card,
    onSurfaceVariant = Fresh.InkSoft,
    surfaceContainer = Fresh.Card,
    surfaceContainerHigh = Fresh.Card,
    surfaceContainerHighest = Fresh.Card,
    outline = Fresh.InkSoft,
    outlineVariant = Fresh.Card,
    primary = Fresh.Ink,
    onPrimary = Fresh.Page,
    secondary = Fresh.Green,
    onSecondary = Fresh.Page,
    secondaryContainer = Fresh.Pastel,
    onSecondaryContainer = Fresh.GreenDeep,
    tertiary = Fresh.Green,
    onTertiary = Fresh.Page,
    error = Fresh.Error,
    // Snackbars use the inverse colours: ink bar, white text, pastel "Undo".
    inverseSurface = Fresh.Ink,
    inverseOnSurface = Fresh.Page,
    inversePrimary = Fresh.Pastel,
    primaryContainer = Fresh.Pastel,
    onPrimaryContainer = Fresh.GreenDeep,
)

/**
 * The app's [Fresh] palette, light or dark as picked in Settings ([Appearance]). No dynamic
 * colour: Material You would repaint it with the wallpaper.
 */
@Composable
fun DailyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = remember { freshScheme() },
        typography = DailyTypography,
        content = content,
    )
}
