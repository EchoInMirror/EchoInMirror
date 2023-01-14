package com.eimsound.daw.impl.clips.midi.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.eimsound.audioprocessor.data.midi.NoteMessage
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.actions.doNoteVelocityAction
import com.eimsound.daw.api.MidiClip
import com.eimsound.daw.api.TrackClip
import com.eimsound.daw.components.EditorGrid
import com.eimsound.daw.components.KEYBOARD_DEFAULT_WIDTH
import com.eimsound.daw.components.utils.Stroke1_5PX
import com.eimsound.daw.window.panels.playlist.EditAction
import kotlin.math.roundToInt

@Composable
internal fun EventEditor(clip: TrackClip<MidiClip>) {
    Row {
        Surface(Modifier.width(KEYBOARD_DEFAULT_WIDTH).fillMaxHeight().zIndex(2f), shadowElevation = 5.dp) {

        }
        Box(Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.background)) {
            val primaryColor = MaterialTheme.colorScheme.primary
            var delta by remember { mutableStateOf(0) }
            var selectedNote by remember { mutableStateOf<NoteMessage?>(null) }
            EditorGrid(noteWidth, horizontalScrollState)
            Spacer(Modifier.fillMaxSize().drawBehind {
                val track = EchoInMirror.selectedTrack ?: return@drawBehind
                val noteWidthPx = noteWidth.value.toPx()
                val offsetOfDelta = -delta / 127F * size.height
                val offsetX = if (action == EditAction.MOVE) deltaX * noteWidthPx else 0f
                val scrollX = horizontalScrollState.value
                val startTime = clip.time
                notesInView.forEach {
                    val isSelected = selectedNotes.contains(it)
                    val x = (startTime + it.time) * noteWidthPx - scrollX + 2 + (if (isSelected) offsetX else 0f)
                    val y = size.height * (1 - it.velocity / 127F) + (if (isSelected || selectedNote == it) offsetOfDelta else 0f)
                    drawLine(track.color, Offset(x, y.coerceIn(0f, size.height - 1)), Offset(x, size.height), 4f)
                }
                selectedNotes.forEach {
                    val x = (startTime + it.time) * noteWidthPx - scrollX + offsetX
                    val y = (size.height * (1 - it.velocity / 127F) + offsetOfDelta).coerceIn(0f, size.height - 1)
                    drawRect(primaryColor, Offset(x, y),
                        Size(4f, size.height - y), style = Stroke1_5PX)
                }
            }.pointerInput(clip) {
                detectDragGestures({
                    val x = it.x + horizontalScrollState.value
                    val noteWidthPx = noteWidth.value.toPx()
                    val startTime = clip.time
                    for (i in startNoteIndex until clip.clip.notes.size) {
                        val note = clip.clip.notes[i]
                        val curX = (startTime + note.time) * noteWidthPx
                        if (curX <= x && x <= curX + 4) {
                            if (selectedNotes.isNotEmpty() && !selectedNotes.contains(note)) continue
                            selectedNote = note
                            break
                        }
                    }
                }, {
                    val cur = selectedNote
                    if (cur != null) {
                        selectedNote = null
                        clip.clip.doNoteVelocityAction(
                            if (selectedNotes.isEmpty()) arrayOf(cur) else selectedNotes.toTypedArray(), delta)
                    }
                    delta = 0
                }) { it, _ ->
                    val cur = selectedNote ?: return@detectDragGestures
                    delta = ((1 - it.position.y / size.height) * 127 - cur.velocity).roundToInt().coerceIn(-127, 127)
                }
            })
        }
    }
}
