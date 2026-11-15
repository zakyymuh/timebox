package dev.zakyymuh.timebox.ui.collection

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import dev.zakyymuh.timebox.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Random

/**
 * A picture to share: the photo(s) on rounded corners inside a white frame, a title and a
 * subtitle under them set flush left, and the papacti rosette pressed over the photo's
 * lower-right corner like a gyotaku ink print, half on the photo and half on the frame.
 * Colours are fixed, so the card looks the same in light and dark mode.
 */
object ShareCard {
    private const val W = 1080
    private const val H = 1350
    private const val MARGIN = 56f
    private const val RADIUS = 44f

    private const val INK = 0xFF1F2A22.toInt()
    private const val INK_SOFT = 0xFF6B7A70.toInt()
    private const val ROSE = 0xFFB8727C.toInt() // dusty rose, the logo's
    private const val PASTEL = 0xFFF7F1EE.toInt() // cream, behind a missing photo

    /** The print's side; the leaf tips reach about 0.48 of it from the centre. */
    private const val PRINT = 342
    private const val PRINT_REACH = PRINT * 0.48f

    /** One plant: its photo [file] (null draws a pot), its name and its number. */
    suspend fun sharePlant(context: Context, file: String?, title: String, subtitle: String) {
        val photo = load(context, file)
        share(context, title) { canvas ->
            val side = W - 2 * MARGIN
            val box = RectF(MARGIN, MARGIN, MARGIN + side, MARGIN + side)
            drawPhoto(canvas, photo, box)
            drawWords(canvas, title, subtitle, top = box.bottom + 70f, right = printLeft(box) - 24f)
            drawPrint(canvas, box)
        }
    }

    /** A cross: mother and father side by side with a × between, its name and its code. */
    suspend fun shareCross(context: Context, mother: String?, father: String?, title: String, subtitle: String) {
        val m = load(context, mother)
        val f = load(context, father)
        share(context, title) { canvas ->
            val gap = 28f
            val side = (W - 2 * MARGIN - gap) / 2
            val top = MARGIN + 120f
            drawPhoto(canvas, m, RectF(MARGIN, top, MARGIN + side, top + side))
            drawPhoto(canvas, f, RectF(MARGIN + side + gap, top, W - MARGIN, top + side))
            // The × sits on a white disc over the seam.
            val cx = W / 2f
            val cy = top + side / 2
            canvas.drawCircle(cx, cy, 52f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
            canvas.drawText("×", cx, cy + 26f, text(76f, ROSE, bold = true))
            val box = RectF(MARGIN, top, W - MARGIN, top + side)
            drawWords(canvas, title, subtitle, top = box.bottom + 110f, right = W - MARGIN)
            drawPrint(canvas, box)
        }
    }

    private suspend fun load(context: Context, file: String?): Bitmap? =
        file?.let { ServiceLocator.photoStore(context).load(it, W) }

    /** Draws the card, writes it to cache/share and opens the share sheet. */
    private suspend fun share(context: Context, title: String, draw: (Canvas) -> Unit) {
        val file = withContext(Dispatchers.Default) {
            val card = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(card)
            canvas.drawColor(0xFFFFFFFF.toInt())
            draw(canvas)
            withContext(Dispatchers.IO) {
                val dir = File(context.cacheDir, "share").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() } // only the latest card is kept
                File(dir, "papacti-${System.currentTimeMillis()}.jpg").also { out ->
                    out.outputStream().use { card.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                }
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.photos", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/jpeg")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "Share $title"))
    }

    /** [photo] cropped to fill [box] with rounded corners; a pot on pastel when there is none. */
    private fun drawPhoto(canvas: Canvas, photo: Bitmap?, box: RectF) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        if (photo == null) {
            paint.color = PASTEL
            canvas.drawRoundRect(box, RADIUS, RADIUS, paint)
            canvas.drawText("🪴", box.centerX(), box.centerY() + box.width() * 0.09f, text(box.width() * 0.26f, INK))
            return
        }
        val scale = maxOf(box.width() / photo.width, box.height() / photo.height)
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(box.left + (box.width() - photo.width * scale) / 2, box.top + (box.height() - photo.height * scale) / 2)
        }
        paint.shader = BitmapShader(photo, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { setLocalMatrix(matrix) }
        canvas.drawRoundRect(box, RADIUS, RADIUS, paint)
    }

    /** Title and subtitle flush left, the title cut short before it reaches [right]. */
    private fun drawWords(canvas: Canvas, title: String, subtitle: String, top: Float, right: Float) {
        val big = text(58f, INK, bold = true).apply { textAlign = Paint.Align.LEFT }
        val small = text(36f, INK_SOFT).apply { textAlign = Paint.Align.LEFT }
        val room = right - MARGIN
        canvas.drawText(TextUtils.ellipsize(title, big, room, TextUtils.TruncateAt.END).toString(), MARGIN, top, big)
        canvas.drawText(TextUtils.ellipsize(subtitle, small, room, TextUtils.TruncateAt.END).toString(), MARGIN, top + 56f, small)
    }

    private fun printCentre(box: RectF) = box.right - 70f to box.bottom + 20f
    private fun printLeft(box: RectF) = printCentre(box).first - PRINT_REACH

    /** The gyotaku rosette over the photo's lower-right corner, multiplied into what is under it. */
    private fun drawPrint(canvas: Canvas, box: RectF) {
        val (cx, cy) = printCentre(box)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            alpha = (0.9f * 255).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) blendMode = BlendMode.MULTIPLY
        }
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(-6.9f)
        canvas.drawBitmap(print, -PRINT / 2f, -PRINT / 2f, paint)
        canvas.restore()
    }

    /** The rosette's rings: rotation, scale and the gap between leaves, as the launcher draws it. */
    private val RINGS: List<Triple<Float, Float, Float>> =
        (0 until 8).map { Triple(it * 45f, 2f, 3f) } +
            (0 until 8).map { Triple(22.5f + it * 45f, 1.5f, 4f) } +
            (0 until 6).map { Triple(it * 60f, 1f, 6f) } +
            (0 until 5).map { Triple(36f + it * 72f, 0.55f, 10.9f) }

    private val LEAF = Path().apply {
        moveTo(0f, 0f)
        cubicTo(38f, -18f, 46f, -62f, 0f, -100f)
        cubicTo(-46f, -62f, -38f, -18f, 0f, 0f)
        close()
    }

    /** Drawn once and kept; a fixed seed makes every card carry the same print. */
    private val print: Bitmap by lazy { gyotaku(PRINT, ROSE) }

    /**
     * The rosette as a gyotaku: inked leaves pressed onto paper. Dense rims and a paler middle,
     * midribs and veins left unprinted, blotches and grain where the ink didn't take, and a
     * slight bleed at the edges.
     */
    private fun gyotaku(size: Int, color: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val r = Random(7)
        fun rnd() = r.nextFloat()
        val clear = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val lift = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = clear }
        val vein = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = clear; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        }
        val gap = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = clear; style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; this.color = Color.BLACK
        }

        c.translate(size / 2f, size / 2f)
        c.scale(size / 420f, size / 420f)
        for ((rot, sc, sw) in RINGS) {
            c.save()
            c.rotate(rot + (rnd() - 0.5f) * 2.5f)
            c.scale(sc, sc)
            ink.alpha = ((0.92f + rnd() * 0.08f) * 255).toInt()
            c.drawPath(LEAF, ink)

            c.save()
            c.clipPath(LEAF)
            val a = ((0.25f + rnd() * 0.2f) * 255).toInt()
            lift.shader = LinearGradient(
                -30f, 0f, 30f, 0f,
                intArrayOf(0, Color.argb(a, 0, 0, 0), 0), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
            )
            c.drawRect(-50f, -100f, 50f, 0f, lift)
            vein.color = Color.argb(191, 0, 0, 0)
            vein.strokeWidth = 2.2f / sc
            c.drawPath(Path().apply { moveTo(0f, -6f); quadTo((rnd() - 0.5f) * 6f, -50f, 0f, -92f) }, vein)
            vein.color = Color.argb(115, 0, 0, 0)
            vein.strokeWidth = 1.1f / sc
            var y = -20f
            while (y > -85f) {
                for (d in intArrayOf(-1, 1)) {
                    val reach = d * (16f + rnd() * 8f) * (1 + y / 130f)
                    c.drawPath(Path().apply { moveTo(0f, y); quadTo(d * 10f, y - 6f, reach, y - 16f) }, vein)
                }
                y -= 13f + rnd() * 6f
            }
            c.restore()

            gap.strokeWidth = sw + 1.5f
            c.drawPath(LEAF, gap)
            c.restore()
        }
        c.drawCircle(0f, 0f, 9f, Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = clear })

        // Blotches where the paper didn't touch the ink.
        c.setMatrix(null)
        val blot = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = clear }
        repeat(70) {
            val x = rnd() * size
            val y = rnd() * size
            val rad = size * (0.02f + rnd() * 0.07f)
            blot.shader = RadialGradient(
                x, y, rad, Color.argb(((0.15f + rnd() * 0.35f) * 255).toInt(), 0, 0, 0), 0, Shader.TileMode.CLAMP,
            )
            c.drawCircle(x, y, rad, blot)
        }

        // Paper grain, then a light [1 2 1] blur of the ink so the edges bleed a little.
        val px = IntArray(size * size)
        bmp.getPixels(px, 0, size, 0, 0, size, size)
        val alpha = FloatArray(px.size) { i ->
            val a = (px[i] ushr 24).toFloat()
            if (a == 0f) 0f else { val n = rnd(); a * if (n < 0.06f) 0.15f else 0.78f + n * 0.22f }
        }
        val tmp = FloatArray(alpha.size)
        for (row in 0 until size) for (col in 0 until size) {
            val i = row * size + col
            tmp[i] = (alpha[i - if (col > 0) 1 else 0] + 2 * alpha[i] + alpha[i + if (col < size - 1) 1 else 0]) / 4
        }
        val rgb = color and 0xFFFFFF
        for (row in 0 until size) for (col in 0 until size) {
            val i = row * size + col
            val a = (tmp[i - if (row > 0) size else 0] + 2 * tmp[i] + tmp[i + if (row < size - 1) size else 0]) / 4
            px[i] = (a.toInt().coerceIn(0, 255) shl 24) or rgb
        }
        bmp.setPixels(px, 0, size, 0, 0, size, size)
        return bmp
    }

    private fun text(size: Float, color: Int, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        textAlign = Paint.Align.CENTER
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }
}
