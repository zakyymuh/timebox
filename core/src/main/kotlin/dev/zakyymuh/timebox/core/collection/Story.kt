package dev.zakyymuh.timebox.core.collection

/**
 * Every share image is an Instagram Story: 1080×1920 (9:16). The photo fills the width
 * inside the margin and ends at [PHOTO_BOTTOM], leaving about [WORDS] px under it for the words.
 */
object Story {
    const val WIDTH = 1080
    const val HEIGHT = 1920

    /** Room under the photo for the title, price, subtitle and the print. */
    const val WORDS = 330

    const val PHOTO_BOTTOM = (HEIGHT - WORDS).toFloat()
}
