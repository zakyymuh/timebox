package dev.zakyymuh.timebox.ui.collection

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zakyymuh.timebox.core.collection.DexPhotos
import dev.zakyymuh.timebox.core.collection.DexShot
import dev.zakyymuh.timebox.ui.common.RoundButton
import dev.zakyymuh.timebox.ui.common.softFieldColors
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Small

private val PhotoShape = RoundedCornerShape(28.dp)

/**
 * A plant's photos, newest first: swipe through the big photo. Each note sits in a bubble
 * at the top, popping in the first time its photo is shown. Tap a photo to change its
 * note; the camera adds a photo.
 */
@Composable
fun PhotoDiary(shots: List<DexShot>, wished: Boolean, onAdd: () -> Unit, onEditNote: (DexShot) -> Unit) {
    val pager = rememberPagerState { shots.size }
    // Ids whose bubble already popped, so swiping back does not pop it again.
    val popped = remember { mutableSetOf<Long>() }
    // A photo just added is the newest: show it.
    val newest = shots.firstOrNull()?.id
    LaunchedEffect(newest) { if (newest != null) pager.scrollToPage(0) }

    Column {
        Box {
            if (shots.isEmpty()) {
                DexPhoto(
                    null,
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(PhotoShape).clickable(onClick = onAdd),
                    wished = wished,
                    side = 1080,
                    emojiSize = 72.sp,
                )
            } else {
                HorizontalPager(state = pager, key = { shots[it].id }, pageSpacing = 12.dp) { page ->
                    val shot = shots[page]
                    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(PhotoShape).clickable { onEditNote(shot) }) {
                        DexPhoto(shot.file, Modifier.matchParentSize(), wished = wished, side = 1080, emojiSize = 72.sp)
                        if (shot.note.isNotBlank()) {
                            NoteBubble(
                                shot,
                                showing = pager.currentPage == page,
                                popped = popped,
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp, start = 24.dp, end = 24.dp),
                            )
                        }
                    }
                }
            }
            RoundButton(
                glyph = "📷",
                onClick = onAdd,
                container = Fresh.Page,
                description = "Add a photo",
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
            )
        }
        if (shots.isNotEmpty()) {
            val page = pager.currentPage.coerceIn(0, shots.lastIndex)
            if (shots.size > 1) Dots(shots.size, page, Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp))
            Text(
                DexPhotos.caption(shots[page].takenOn, page, shots.size),
                style = Small,
                color = Fresh.InkSoft,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
            )
        }
    }
}

/** The note, in a rounded bubble with a white border. It pops in once, then stays still. */
@Composable
private fun NoteBubble(shot: DexShot, showing: Boolean, popped: MutableSet<Long>, modifier: Modifier) {
    val scale = remember(shot.id) { Animatable(if (shot.id in popped) 1f else 0f) }
    LaunchedEffect(shot.id, showing) {
        if (showing && shot.id !in popped) {
            popped += shot.id
            scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 320f))
        }
    }
    Text(
        shot.note,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                alpha = scale.value.coerceIn(0f, 1f)
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
            }
            .background(Fresh.Ink.copy(alpha = 0.72f), RoundedCornerShape(50))
            .border(2.dp, Color.White, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun Dots(count: Int, selected: Int, modifier: Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
        repeat(count) { i ->
            Box(Modifier.size(if (i == selected) 8.dp else 6.dp).background(if (i == selected) Fresh.Ink else Fresh.InkSoft.copy(alpha = 0.35f), CircleShape))
        }
    }
}

/** Asks for a photo's short note: for a just-picked photo, or to change one ([initial]). Optional. */
@Composable
fun PhotoNoteDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    initial: String = "",
    title: String = "Add a note",
    confirm: String = "Save photo",
) {
    var note by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fresh.Page,
        title = { Text(title) },
        text = {
            Column {
                TextField(
                    value = note,
                    onValueChange = { note = it.take(DexPhotos.MAX_NOTE) },
                    placeholder = { Text("e.g. first new leaf!", color = Fresh.InkSoft) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = softFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "${note.length}/${DexPhotos.MAX_NOTE} · optional",
                    style = Small,
                    color = Fresh.InkSoft,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(note) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
