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
 * [Palette] with [use], once, before anything is drawn: papacti paints itself dusty rose
 * ([Rose]), Daily keeps [Garden], Motor paints itself red. Each app is its own process, so one app's choice never
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

    /**
     * Dusty rose, papacti's: the rosette logo's #B8727C on its cream #F7F1EE. [Palette.green]
     * is the rose, [Palette.greenDeep] a deep rose that reads on cream and blush.
     */
    val Rose = Palette(
        page = Color(0xFFFFFFFF),
        card = Color(0xFFF7F1EE),
        ink = Color(0xFF1E1A1B),
        inkSoft = Color(0xFF6F6466),
        pastel = Color(0xFFF2DFE0),
        green = Color(0xFFB8727C),
        greenDeep = Color(0xFF7A3E48),
        error = Color(0xFFC0453A),
    )

    /** Rose at night: warm near-black page, light ink, a softer rose. */
    val RoseDark = Palette(
        page = Color(0xFF151213),
        card = Color(0xFF231E1F),
        ink = Color(0xFFF1E9E7),
        inkSoft = Color(0xFFA89A9C),
        pastel = Color(0xFF47292F),
        green = Color(0xFFC4848C),
        greenDeep = Color(0xFFEFC6CA),
        error = Color(0xFFE5705F),
    )

    /** Garden at night: near-black page, light ink, the same green. */
    val GardenDark = Palette(
        page = Color(0xFF121411),
        card = Color(0xFF1F221E),
        ink = Color(0xFFE9ECE7),
        inkSoft = Color(0xFF9CA298),
        pastel = Color(0xFF233F2D),
        green = Color(0xFF5DB37E),
        greenDeep = Color(0xFFB4E3C2),
        error = Color(0xFFE5705F),
    )

    /** True when the app is painted with its dark palette. */
    var Dark = false; private set

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
    fun use(palette: Palette, dark: Boolean = false) {
        Dark = dark
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
