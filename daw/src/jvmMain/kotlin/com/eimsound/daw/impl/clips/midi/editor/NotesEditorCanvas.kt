@file:OptIn(ExperimentalComposeUiApi::class)

package com.eimsound.daw.impl.clips.midi.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import com.eimsound.audioprocessor.data.midi.NoteMessage
import com.eimsound.audioprocessor.data.midi.colorSaturation
import com.eimsound.audioprocessor.projectDisplayPPQ
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.api.asMidiTrackClipOrNull
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.components.EditorGrid
import com.eimsound.daw.components.KEYBOARD_KEYS
import com.eimsound.daw.components.LocalFloatingDialogProvider
import com.eimsound.daw.components.dragdrop.dropTarget
import com.eimsound.daw.components.utils.BorderCornerRadius2PX
import com.eimsound.daw.components.utils.Stroke1PX
import com.eimsound.daw.components.utils.Stroke1_5PX
import com.eimsound.daw.components.utils.saturate
import com.eimsound.daw.data.defaultScale
import com.eimsound.daw.utils.*
import com.eimsound.daw.window.panels.playlist.EditAction
import kotlin.math.absoluteValue

internal var selectionStartX = 0F
internal var selectionStartY = 0
internal var selectionX by mutableStateOf(0F)
internal var selectionY by mutableStateOf(0)
internal var deltaX by mutableStateOf(0)
internal var deltaY by mutableStateOf(0)
internal var action by mutableStateOf(EditAction.NONE)
internal var resizeDirectionRight = false
@OptIn(ExperimentalComposeUiApi::class)
internal var cursor by mutableStateOf(PointerIconDefaults.Default)
internal var currentNote = 0

private data class NoteDrawObject(val note: NoteMessage, val offset: Offset, val size: Size, val color: Color)
private data class BackingTrack(val track: Track, val notes: ArrayList<NoteDrawObject>)

@Suppress("DuplicatedCode")
@Composable
internal fun NotesEditorCanvas(editor: DefaultMidiClipEditor) {
    val coroutineScope = rememberCoroutineScope()
    val floatingDialogProvider = LocalFloatingDialogProvider.current
    editor.apply {
        Box(
            Modifier.fillMaxSize().clipToBounds().background(MaterialTheme.colorScheme.background)
                .scrollable(verticalScrollState, Orientation.Vertical, reverseDirection = true)
                .onGloballyPositioned { offsetOfRoot = it.positionInRoot() }
                .pointerInput(coroutineScope, editor) {
                    handleMouseEvent(coroutineScope, editor, floatingDialogProvider)
                }
                .dropTarget({ _, _ -> true }) { _, pos ->
                    println(pos)
                    true
                }
        ) {
            val highlightNoteColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05F)
            val outlineColor = MaterialTheme.colorScheme.surfaceVariant
            val primaryColor = MaterialTheme.colorScheme.primary
            val displayPPQ = EchoInMirror.currentPosition.projectDisplayPPQ
            val localDensity = LocalDensity.current
            remember(displayPPQ, localDensity) {
                with (localDensity) { horizontalScrollState.openMaxValue = (noteWidth.value.toPx() * displayPPQ).toInt() }
            }
            val trackClip = editor.clip
            val range = remember(trackClip.time, trackClip.duration) { trackClip.time..(trackClip.time + trackClip.duration) }
            EditorGrid(noteWidth, horizontalScrollState, range)
            Spacer(Modifier.fillMaxSize().drawWithCache {
                val noteWidthPx = noteWidth.value.toPx()
                val verticalScrollValue = verticalScrollState.value
                val horizontalScrollValue = horizontalScrollState.value
                val noteHeightPx = noteHeight.toPx()
                val notes = arrayListOf<NoteDrawObject>()
                val backingsNotes = arrayListOf<BackingTrack>()
                action // read mutable state of action

                // get note draw nodes - start
                val startTime = trackClip.time
                val clip = trackClip.clip
                startNoteIndex = 0
                var flag = true
                clip.notes.read()
                val allNotes = clip.notes.toSet()
                selectedNotes.removeIf { !allNotes.contains(it) }

                val notesInViewList = arrayListOf<NoteMessage>()
                val trackColor = track.color
                for ((index, it) in clip.notes.withIndex()) {
                    val y = (KEYBOARD_KEYS - 1 - it.note) * noteHeightPx - verticalScrollValue
                    val x = (startTime + it.time) * noteWidthPx - horizontalScrollValue
                    if (x > size.width) break
                    notesInViewList.add(it)
                    if (y < -noteHeightPx || y > size.height || deletionList.contains(it)) continue
                    val width = it.duration * noteWidthPx
                    if (x < 0 && width < -x) continue
                    if (flag) {
                        startNoteIndex = index
                        flag = false
                    }
                    if (selectedNotes.contains(it)) continue
                    notes.add(
                        NoteDrawObject(it, Offset(x, y.coerceAtLeast(0F)), Size(width, if (y < 0)
                            (noteHeightPx + y).coerceAtLeast(0F) else noteHeightPx),
                            trackColor.saturate(it.colorSaturation)
                        )
                    )
                }
                notesInView = notesInViewList
                // get note draw nodes - end

                backingTracks.forEach { track ->
                    if (track == EchoInMirror.selectedTrack) return@forEach
                    track.clips.read()
                    val color = track.color
                    val curNotes = arrayListOf<NoteDrawObject>()
                    track.clips.forEach { clip0 ->
                        @Suppress("LABEL_NAME_CLASH") val curClip = clip0.asMidiTrackClipOrNull() ?: return@forEach
                        val clipStartTime = curClip.time
                        for (it in curClip.clip.notes) {
                            val y = (KEYBOARD_KEYS - 1 - it.note) * noteHeightPx - verticalScrollValue
                            val x = (clipStartTime + it.time) * noteWidthPx - horizontalScrollValue
                            if (x > size.width) break
                            if (y < -noteHeightPx || y > size.height || deletionList.contains(it)) continue
                            val width = it.duration * noteWidthPx
                            if (x < 0 && width < -x) continue
                            curNotes.add(
                                NoteDrawObject(it, Offset(x, y.coerceAtLeast(0F)), Size(width, if (y < 0)
                                    (noteHeightPx + y).coerceAtLeast(0F) else noteHeightPx),
                                    color.copy(0.6F + 0.4F * mapValue(it.velocity, 0, 127)))
                            )
                        }
                    }
                    backingsNotes.add(BackingTrack(track, curNotes))
                }

                onDrawBehind {
                    val isDarkTheme = EchoInMirror.windowManager.isDarkTheme
                    for (i in (verticalScrollValue / noteHeightPx).toInt()..((verticalScrollValue + size.height) / noteHeightPx).toInt()) {
                        val y = i * noteHeightPx - verticalScrollValue
                        if (y < 0) continue
                        drawLine(outlineColor, Offset(0f, y), Offset(size.width, y), 1F)
                        if (defaultScale.scale[11 - (i + 4) % 12] != isDarkTheme)
                            drawRect(highlightNoteColor, Offset(0f, y), Size(size.width, noteHeightPx))
                    }

                    backingsNotes.forEach { cur ->
                        val color = cur.track.color.copy(0.16F)
                        cur.notes.forEach { drawRoundRect(color, it.offset, it.size, BorderCornerRadius2PX) }
                    }
                    notes.forEach { drawRoundRect(it.color, it.offset, it.size, BorderCornerRadius2PX) }
                    selectedNotes.forEach {
                        var y = (KEYBOARD_KEYS - 1 - it.note) * noteHeightPx - verticalScrollValue
                        val x = (startTime + it.time) * noteWidthPx - horizontalScrollValue
                        val width = it.duration * noteWidthPx
                        val offset: Offset
                        val size: Size
                        when (action) {
                            EditAction.MOVE -> {
                                y += deltaY * noteHeightPx
                                offset = Offset(x + deltaX * noteWidthPx, y.coerceAtLeast(0F))
                                size = Size(width, if (y < 0) (noteHeightPx + y).coerceAtLeast(0F) else noteHeightPx)
                            }
                            EditAction.RESIZE -> {
                                val height = if (y < 0) (noteHeightPx + y).coerceAtLeast(0F) else noteHeightPx
                                if (y < 0) y = 0F
                                offset = if (resizeDirectionRight) Offset(x, y) else Offset(x + deltaX * noteWidthPx, y)
                                size = Size(width + (if (resizeDirectionRight) deltaX else -deltaX) * noteWidthPx, height)
                            }
                            else -> {
                                offset = Offset(x, y.coerceAtLeast(0F))
                                size = Size(width, if (y < 0) (noteHeightPx + y).coerceAtLeast(0F) else noteHeightPx)
                            }
                        }
                        if (size.height <= 0 || size.width <= 0) return@forEach
                        drawRoundRect(trackColor.saturate(it.colorSaturation), offset, size, BorderCornerRadius2PX)
                        drawRoundRect(primaryColor, offset, size, BorderCornerRadius2PX, Stroke1_5PX)
                    }
                }
            })
            Canvas(Modifier.fillMaxSize().pointerHoverIcon(action.toPointerIcon(cursor))) {
                if (selectionX == 0F && selectionStartX == 0F) return@Canvas
                val scrollX = horizontalScrollState.value
                val scrollY = verticalScrollState.value
                val noteHeightPx = noteHeight.toPx()
                val y = (selectionStartY.coerceAtMost(selectionY).coerceAtMost(KEYBOARD_KEYS - 1) *
                        noteHeightPx - scrollY)
                val pos = Offset(
                    (selectionStartX.coerceAtMost(selectionX) - scrollX).coerceAtLeast(0F),
                    y.coerceAtLeast(0F)
                )
                val size = Size(
                    (selectionX.coerceAtLeast(scrollX.toFloat()) - selectionStartX).absoluteValue,
                    (selectionY.coerceAtLeast((scrollY / noteHeightPx).toInt()) - selectionStartY)
                        .absoluteValue.coerceIn(1, KEYBOARD_KEYS) * noteHeightPx -
                            (if (y < 0) -y % noteHeightPx else 0F)
                )
                drawRect(primaryColor.copy(0.1F), pos, size)
                drawRect(primaryColor, pos, size, style = Stroke1PX)
            }
        }
    }
}