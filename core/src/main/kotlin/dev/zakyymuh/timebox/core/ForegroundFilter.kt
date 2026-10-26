package dev.zakyymuh.timebox.core

/**
 * Decides which window changes are worth evaluating.
 *
 * Two things make this necessary. One app launch fires several
 * `typeWindowStateChanged` events, so evaluating every one would raise the wall
 * several times over. And the wall is itself a window, so without an own-package
 * rule the gate gates itself and relaunches forever.
 *
 * Not thread-safe: the accessibility service delivers events on one thread.
 */
class ForegroundFilter(private val ownPackage: String) {

    private var lastPackage: String? = null

    /**
     * Records [packageName] as the foreground app and returns whether it should be
     * evaluated now. Timebox's own windows are ignored without disturbing the record
     * of what was in front before them.
     */
    fun onWindow(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        if (packageName == ownPackage) return false

        val changed = packageName != lastPackage
        lastPackage = packageName
        return changed
    }

    /** The last real foreground app, ignoring Timebox's own windows. */
    fun currentPackage(): String? = lastPackage

    fun reset() {
        lastPackage = null
    }
}
