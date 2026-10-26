package dev.zakyymuh.timebox.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundFilterTest {

    private val f = ForegroundFilter("dev.zakyymuh.timebox")

    @Test
    fun `evaluates the first window event for an app`() {
        assertTrue(f.onWindow("com.instagram.android"))
    }

    @Test
    fun `collapses repeated events for the same app`() {
        f.onWindow("com.instagram.android")
        assertFalse(f.onWindow("com.instagram.android"))
        assertFalse(f.onWindow("com.instagram.android"))
    }

    @Test
    fun `evaluates the app again after another app was in between`() {
        f.onWindow("com.instagram.android")
        f.onWindow("com.google.android.apps.nexuslauncher")
        assertTrue(f.onWindow("com.instagram.android"))
    }

    @Test
    fun `never evaluates its own package`() {
        assertFalse(f.onWindow("dev.zakyymuh.timebox"))
    }

    @Test
    fun `its own window does not disturb what came before it`() {
        f.onWindow("com.instagram.android")
        f.onWindow("dev.zakyymuh.timebox")
        assertFalse(f.onWindow("com.instagram.android"))
    }

    @Test
    fun `ignores null and blank package names`() {
        assertFalse(f.onWindow(null))
        assertFalse(f.onWindow(""))
    }

    @Test
    fun `currentPackage reports the last real foreground app`() {
        f.onWindow("com.instagram.android")
        f.onWindow("dev.zakyymuh.timebox")
        assertEquals("com.instagram.android", f.currentPackage())
    }
}
