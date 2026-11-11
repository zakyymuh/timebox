package dev.zakyymuh.timebox.ui.collection

import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.zakyymuh.timebox.ServiceLocator
import dev.zakyymuh.timebox.ui.theme.Fresh

/**
 * Returns a function that asks "camera or gallery?" and hands the chosen picture to
 * [onPicked]. Neither path needs a permission: the camera app writes into our FileProvider
 * file, and the system photo picker grants just the photos picked.
 */
@Composable
fun rememberPhotoPicker(onPicked: (Uri) -> Unit): () -> Unit =
    rememberPhotosPicker(max = 1) { onPicked(it.first()) }

/**
 * Like [rememberPhotoPicker], but the gallery lets up to [max] photos be picked at once,
 * handed over in the order picked. The camera still takes one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberPhotosPicker(max: Int = MAX_AT_ONCE, onPicked: (List<Uri>) -> Unit): () -> Unit {
    val context = LocalContext.current
    var asking by remember { mutableStateOf(false) }
    var shot by rememberSaveable { mutableStateOf<Uri?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) shot?.let { onPicked(listOf(it)) }
    }
    val one = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onPicked(listOf(it)) }
    }
    // The multiple picker needs a limit of at least 2.
    val many = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(max.coerceAtLeast(2))) { uris ->
        if (uris.isNotEmpty()) onPicked(uris)
    }

    if (asking) {
        ModalBottomSheet(onDismissRequest = { asking = false }, containerColor = Fresh.Page) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Option("📷   Take a photo") {
                    asking = false
                    val uri = ServiceLocator.photoStore(context).cameraUri()
                    shot = uri
                    try {
                        camera.launch(uri)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, "No camera app found", Toast.LENGTH_SHORT).show()
                    }
                }
                Option(if (max > 1) "🖼   Choose from gallery (up to $max)" else "🖼   Choose from gallery") {
                    asking = false
                    val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    if (max > 1) many.launch(request) else one.launch(request)
                }
            }
        }
    }
    return { asking = true }
}

/** How many photos the gallery hands over in one go. */
const val MAX_AT_ONCE = 10

@Composable
private fun Option(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.titleMedium,
        color = Fresh.Ink,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 18.dp),
    )
}
