package dev.zakyymuh.timebox.ui.collection

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.zakyymuh.timebox.ServiceLocator
import dev.zakyymuh.timebox.ui.theme.Fresh

private val greyscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * A plant's photo, cropped to fill [modifier]'s shape. [wished] shows it grey and faded,
 * like an entry not yet caught. With no photo: a pot on pastel, or a grey ? for a wish.
 */
@Composable
fun DexPhoto(
    photo: String?,
    modifier: Modifier = Modifier,
    wished: Boolean = false,
    side: Int = 480,
    emojiSize: TextUnit = 40.sp,
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, photo, side) {
        value = photo?.let { ServiceLocator.photoStore(context).load(it, side)?.asImageBitmap() }
    }
    Box(
        modifier = modifier.background(if (wished) Fresh.Card else Fresh.Pastel),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = if (wished) greyscale else null,
                modifier = Modifier.matchParentSize().alpha(if (wished) 0.55f else 1f),
            )
        } else if (photo == null) {
            Text(
                text = if (wished) "?" else "🪴",
                fontSize = emojiSize,
                color = Fresh.InkSoft,
                modifier = Modifier.alpha(if (wished) 0.5f else 1f),
            )
        }
    }
}
