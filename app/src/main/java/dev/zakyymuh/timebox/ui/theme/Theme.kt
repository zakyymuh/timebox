package dev.zakyymuh.timebox.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val FreshScheme = lightColorScheme(
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
 * Always light — white and pastel green whatever the phone's dark-mode setting (owner's
 * call). No dynamic colour either: Material You would repaint it with the wallpaper.
 */
@Composable
fun DailyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FreshScheme,
        typography = DailyTypography,
        content = content,
    )
}
