package dev.zakyymuh.timebox.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The eight colours an app is painted with, by role. The names come from the original
 * green palette: [green] is "the accent", [greenDeep] "the deep accent", [pastel] "this
 * needs you now (or is selected)", whatever colour they actually are.
 */
data class Palette(
    val page: Color,
    val card: Color,
    val ink: Color,
    val inkSoft: Color,
    val pastel: Color,
    val green: Color,
    val greenDeep: Color,
    val error: Color,
)

/**
 * The colours every screen reads. White and pastel green unless the app picks another
 * [Palette] with [use], once, before anything is drawn: papacti and Daily keep [Garden],
 * Motor paints itself red. Each app is its own process, so one app's choice never
 * reaches another.
 */
object Fresh {
    /** White and pastel green: papacti and Daily. */
    val Garden = Palette(
        page = Color(0xFFFFFFFF),
        card = Color(0xFFF5F7F3),
        ink = Color(0xFF1B1D1A),
        inkSoft = Color(0xFF6B7068),
        pastel = Color(0xFFCDEBD5),
        green = Color(0xFF5DB37E),
        greenDeep = Color(0xFF1F4D33),
        error = Color(0xFFC0453A),
    )

    var Page = Garden.page; private set
    var Card = Garden.card; private set
    var Ink = Garden.ink; private set
    var InkSoft = Garden.inkSoft; private set
    var Pastel = Garden.pastel; private set
    var Green = Garden.green; private set
    var GreenDeep = Garden.greenDeep; private set
    var Error = Garden.error; private set

    /**
     * Repaints the app with [palette]. Call it in onCreate before setContent: nothing
     * observes these values, so a change after the first frame would only show in part.
     */
    fun use(palette: Palette) {
        Page = palette.page
        Card = palette.card
        Ink = palette.ink
        InkSoft = palette.inkSoft
        Pastel = palette.pastel
        Green = palette.green
        GreenDeep = palette.greenDeep
        Error = palette.error
    }
}
