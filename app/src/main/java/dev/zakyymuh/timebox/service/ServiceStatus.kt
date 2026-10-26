package dev.zakyymuh.timebox.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

/**
 * Whether Timebox is actually watching.
 *
 * The app's real failure mode is not being bypassed, it is being silent — the service
 * switched off in Settings, or killed by an OEM battery manager, while the app still
 * looks installed. So this is checked on every resume and said out loud.
 */
object ServiceStatus {

    fun isAccessibilityEnabled(context: Context): Boolean {
        val setting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )
        val own = ComponentName(context, AppWatcherService::class.java).flattenToString()
        return parseEnabledServices(setting, own)
    }

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun isWatching(context: Context): Boolean =
        isAccessibilityEnabled(context) && canDrawOverlays(context)

    /**
     * The setting is a colon-separated list of `package/class` entries. Substring
     * matching on it is wrong twice over: a package merely containing ours would count,
     * and the system may store the class in short form (`pkg/.Class`). So each entry is
     * parsed and compared as a component.
     */
    fun parseEnabledServices(setting: String?, ownComponent: String): Boolean {
        if (setting.isNullOrBlank()) return false
        val own = ComponentName.unflattenFromString(ownComponent) ?: return false
        return setting.split(':').any { entry ->
            ComponentName.unflattenFromString(entry.trim()) == own
        }
    }
}
