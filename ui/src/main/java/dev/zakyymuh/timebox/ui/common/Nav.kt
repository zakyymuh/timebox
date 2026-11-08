package dev.zakyymuh.timebox.ui.common

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController

/**
 * Runs [block] only while this screen is the one in front. Navigating or popping moves the
 * screen out of RESUMED at once, so a double tap cannot pop twice (leaving a blank screen
 * with no destination) or push the same screen twice.
 */
inline fun NavBackStackEntry.ifResumed(block: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) block()
}

fun NavHostController.go(route: String) = navigate(route) { launchSingleTop = true }
