package dev.zakyymuh.motor.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Vehicle photos, copied into the app's own storage and shrunk. A picked photo's Uri
 * stops working once the picker's grant expires, so the app keeps its own copy.
 */
class PhotoStore(private val context: Context) {

    private val dir: File get() = File(context.filesDir, "vehicles").apply { mkdirs() }

    /** Copies the picture at [uri], at most [MAX_SIDE] px on its long side. Returns its file name. */
    suspend fun importPhoto(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decode(uri) ?: return@runCatching null
            val name = "vehicle-${UUID.randomUUID()}.jpg"
            File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            bitmap.recycle()
            name
        }.getOrNull()
    }

    suspend fun load(name: String): Bitmap? = withContext(Dispatchers.IO) {
        File(dir, name).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
    }

    fun delete(name: String?) {
        if (name != null) File(dir, name).delete()
    }

    private fun decode(uri: Uri): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder applies the photo's EXIF rotation, so it comes out upright.
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val (w, h) = info.size.width to info.size.height
                val scale = MAX_SIDE.toFloat() / maxOf(w, h)
                if (scale < 1f) decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        }

    private companion object {
        const val MAX_SIDE = 1024
    }
}
