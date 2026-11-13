package dev.zakyymuh.timebox.ui.collection

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dev.zakyymuh.timebox.R
import dev.zakyymuh.timebox.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * A picture to share: the photo(s) on rounded corners inside a white frame, a title and a
 * subtitle under them, and the papacti logo at the bottom centre. Colours are fixed, so the
 * card looks the same in light and dark mode.
 */
object ShareCard {
    private const val W = 1080
    private const val H = 1350
    private const val MARGIN = 56f
    private const val RADIUS = 44f

    private const val INK = 0xFF1F2A22.toInt()
    private const val INK_SOFT = 0xFF6B7A70.toInt()
    private const val GREEN = 0xFF1F4D33.toInt()
    private const val PASTEL = 0xFFCDEBD5.toInt()

    /** One plant: its photo [file] (null draws a pot), its name and its number. */
    suspend fun sharePlant(context: Context, file: String?, title: String, subtitle: String) {
        val photo = load(context, file)
        share(context, title) { canvas ->
            val side = W - 2 * MARGIN
            drawPhoto(canvas, photo, RectF(MARGIN, MARGIN, MARGIN + side, MARGIN + side))
            drawWords(canvas, title, subtitle, top = MARGIN + side + 70f)
        }
    }

    /** A cross: mother and father side by side with a × between, its name and its code. */
    suspend fun shareCross(context: Context, mother: String?, father: String?, title: String, subtitle: String) {
        val m = load(context, mother)
        val f = load(context, father)
        share(context, title) { canvas ->
            val gap = 28f
            val side = (W - 2 * MARGIN - gap) / 2
            // Photos, × and words, centred in the space above the logo.
            val top = 300f
            drawPhoto(canvas, m, RectF(MARGIN, top, MARGIN + side, top + side))
            drawPhoto(canvas, f, RectF(MARGIN + side + gap, top, W - MARGIN, top + side))
            // The × sits on a white disc over the seam.
            val cx = W / 2f
            val cy = top + side / 2
            canvas.drawCircle(cx, cy, 52f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
            canvas.drawText("×", cx, cy + 26f, text(76f, GREEN, bold = true))
            drawWords(canvas, title, subtitle, top = top + side + 110f)
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
            drawLogo(context, canvas)
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

    private fun drawWords(canvas: Canvas, title: String, subtitle: String, top: Float) {
        val big = text(58f, INK, bold = true)
        val fitted = TextUtils.ellipsize(title, big, W - 2 * MARGIN, TextUtils.TruncateAt.END).toString()
        canvas.drawText(fitted, W / 2f, top, big)
        canvas.drawText(subtitle, W / 2f, top + 56f, text(36f, INK_SOFT))
    }

    /** The launcher's bishop's cap on its pastel disc, with "papacti" beside it. */
    private fun drawLogo(context: Context, canvas: Canvas) {
        val r = 36f
        val word = text(44f, GREEN, bold = true).apply { textAlign = Paint.Align.LEFT }
        val gap = 18f
        val width = 2 * r + gap + word.measureText("papacti")
        val left = (W - width) / 2
        val cy = H - MARGIN - r
        canvas.drawCircle(left + r, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PASTEL })
        ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)?.let { cap ->
            // The art sits in the middle of a 108-unit canvas; 1.5× the disc makes it fill it.
            val half = (r * 1.5f).toInt()
            cap.setBounds((left + r).toInt() - half, cy.toInt() - half, (left + r).toInt() + half, cy.toInt() + half)
            canvas.save()
            canvas.clipPath(Path().apply { addCircle(left + r, cy, r, Path.Direction.CW) })
            cap.draw(canvas)
            canvas.restore()
        }
        canvas.drawText("papacti", left + 2 * r + gap, cy + 15f, word)
    }

    private fun text(size: Float, color: Int, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        textAlign = Paint.Align.CENTER
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }
}
