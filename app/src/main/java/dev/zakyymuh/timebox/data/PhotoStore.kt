package dev.zakyymuh.timebox.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Plant photos, kept inside the app (filesDir/photos) so they survive the gallery being
 * cleaned and need no storage permission. Every photo is shrunk to [MAX_SIDE] and turned
 * upright on the way in, so a 50 MP camera shot costs a few hundred KB, not 15 MB.
 */
class PhotoStore(private val context: Context) : PhotoFiles {

    private val dir: File get() = File(context.filesDir, "photos").apply { mkdirs() }

    /** Decoded thumbnails, so scrolling the grid does not decode each photo again. */
    private val cache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    /** Where the camera writes its shot before it is imported. Shared through FileProvider. */
    fun cameraUri(): Uri {
        val shot = File(File(context.cacheDir, "camera").apply { mkdirs() }, "shot.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.photos", shot)
    }

    /** Copies [uri] in as a new upright, shrunk JPEG. Returns its file name, or null if unreadable. */
    suspend fun import(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0) return@runCatching null
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleFor(maxOf(bounds.outWidth, bounds.outHeight), MAX_SIDE)
            }
            val raw = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return@runCatching null
            val degrees = resolver.openInputStream(uri)?.use { rotationOf(ExifInterface(it)) } ?: 0
            val upright = scaleAndRotate(raw, MAX_SIDE, degrees)
            val name = "${UUID.randomUUID()}.jpg"
            File(dir, name).outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            name
        }.getOrNull()
    }

    /** A bitmap of [name] about [side] px on its long edge, or null if the file is gone. */
    suspend fun load(name: String, side: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$name@$side"
        cache.get(key) ?: run {
            val file = File(dir, name)
            if (!file.exists()) return@run null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleFor(maxOf(bounds.outWidth, bounds.outHeight), side)
            }
            BitmapFactory.decodeFile(file.path, options)?.also { cache.put(key, it) }
        }
    }

    override fun delete(name: String) {
        File(dir, name).delete()
        cache.snapshot().keys.filter { it.startsWith("$name@") }.forEach(cache::remove)
    }

    private fun sampleFor(longEdge: Int, target: Int): Int {
        var sample = 1
        while (longEdge / (sample * 2) >= target) sample *= 2
        return sample
    }

    private fun rotationOf(exif: ExifInterface): Int =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    private fun scaleAndRotate(source: Bitmap, maxSide: Int, degrees: Int): Bitmap {
        val scale = minOf(1f, maxSide.toFloat() / maxOf(source.width, source.height))
        if (scale == 1f && degrees == 0) return source
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(degrees.toFloat())
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    companion object {
        const val MAX_SIDE = 1280
    }
}
