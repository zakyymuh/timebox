package dev.zakyymuh.timebox.ui

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * An old notification or shortcut can still ask papacti for a screen that moved to
 * another app. It must land on the dashboard, not crash on a route that no longer exists.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DestinationsTest {

    @Test
    fun `today is no longer a destination`() {
        assertNull(Destinations.from(Intent().putExtra(Destinations.EXTRA, "today")))
    }

    @Test
    fun `plants still is`() {
        assertEquals(Destinations.PLANTS, Destinations.from(Intent().putExtra(Destinations.EXTRA, Destinations.PLANTS)))
    }
}
