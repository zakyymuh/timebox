package dev.zakyymuh.timebox.ui.theme

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb

/** Light or dark, picked in Settings. Each app keeps its own choice. */
enum class Appearance(val label: String) {
    System("Follow phone"),
    Light("Light"),
    Dark("Dark");

    companion object {
        private const val PREFS = "appearance"
        private const val KEY = "mode"

        fun load(context: Context): Appearance {
            val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            return entries.firstOrNull { it.name == name } ?: System
        }

        /** Saves [mode] and repaints [activity] with it. */
        fun choose(activity: Activity, mode: Appearance) {
            if (mode == load(activity)) return
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, mode.name).apply()
            activity.recreate()
        }

        fun isDark(context: Context, mode: Appearance = load(context)): Boolean = when (mode) {
            Light -> false
            Dark -> true
            System -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        }

        /**
         * Paints [activity] with [light] or [dark] and matches the system bars. Call it first
         * thing in onCreate, before setContent: nothing observes [Fresh], so a later change
         * only shows after [Activity.recreate].
         */
        fun start(activity: ComponentActivity, light: Palette, dark: Palette) {
            val night = isDark(activity)
            Fresh.use(if (night) dark else light, night)
            val bar = Fresh.Page.toArgb()
            val style = if (night) SystemBarStyle.dark(bar) else SystemBarStyle.light(bar, bar)
            activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        }
    }
}
