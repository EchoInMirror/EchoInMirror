package com.eimsound.daw.impl.clips.midi.editor

import androidx.compose.foundation.gestures.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import com.eimsound.audioprocessor.data.midi.NoteMessage
import com.eimsound.audioprocessor.data.midi.defaultNoteMessage
import com.eimsound.audioprocessor.data.midi.toNoteOffEvent
import com.eimsound.audioprocessor.data.midi.toNoteOnEvent
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.actions.doNoteAmountAction
import com.eimsound.daw.actions.doNoteMessageEditAction
import com.eimsound.daw.components.FloatingDialogProvider
import com.eimsound.daw.components.KEYBOARD_KEYS
import com.eimsound.daw.components.utils.EditAction
import com.eimsound.daw.components.utils.HorizontalResize
import com.eimsound.daw.components.utils.Move
import com.eimsound.daw.data.getEditUnit
import com.eimsound.daw.utils.*
import com.eimsound.daw.window.panels.playlist.calcScroll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private var currentX = 0
private var lastSelectedNoteDuration = 0

private fun Density.getClickedNotes(x: Float, y: Float, editor: DefaultMidiClipEditor,
                                    block: Density.(NoteMessage) -> Boolean = { true }): NoteMessage? {
    editor.apply {
        currentNote = KEYBOARD_KEYS - ((y + verticalScrollState.value) / noteHeight.toPx()).toInt() - 1
        currentX = ((x + horizontalScrollState.value) / noteWidth.value.toPx() - clip.time).roundToInt()
        for (i in startNoteIndex..clip.clip.notes.lastIndex) {
            val note = clip.clip.notes[i]
            val startTime = note.time
            if (startTime > currentX) break
            if (note.note == currentNote && startTime <= currentX && startTime + note.duration >= currentX && block(note))
                return note
        }
    }
    return null
}

private fun playNote(note: NoteMessage, editor: DefaultMidiClipEditor) {
    if (!editor.playOnEdit) return
    if (EchoInMirror.currentPosition.isPlaying) return
    editor.playingNotes.add(note.toNoteOffEvent())
    editor.track.playMidiEvent(note.toNoteOnEvent())
}

private fun stopAllNotes(editor: DefaultMidiClipEditor) {
    if (editor.playingNotes.isNotEmpty()) {
        if (!EchoInMirror.currentPosition.isPlaying) editor.playingNotes.forEach { editor.track.playMidiEvent(it) }
        editor.playingNotes.clear()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("DuplicatedCode")
internal suspend fun PointerInputScope.handleMouseEvent(coroutineScope: CoroutineScope,
                                                        editor: DefaultMidiClipEditor,
                                                        floatingDialogProvider: FloatingDialogProvider) {
    forEachGesture { editor.apply {
        awaitPointerEventScope {
            var event: PointerEvent
            var selectedNotesLeft = Int.MAX_VALUE
            var selectedNotesTop = 0
            var selectedNotesBottom = Int.MAX_VALUE
            var minNoteDuration = Int.MAX_VALUE
            do {
                event = awaitPointerEvent(PointerEventPass.Main)
                when (event.type) {
                    PointerEventType.Move -> {
                        var cursor0 = PointerIconDefaults.Default
                        getClickedNotes(event.x, event.y, editor)?.let {
                            val startTime = clip.time + it.time
                            val startX = startTime * noteWidth.value.toPx() - horizontalScrollState.value
                            val endX = (startTime + it.duration) * noteWidth.value.toPx() - horizontalScrollState.value
                            cursor0 = if ((event.x < startX + 4 && event.x > startX - 4) ||
                                (event.x < endX + 4 && event.x > endX - 4)) PointerIconDefaults.HorizontalResize
                            else PointerIconDefaults.Move
                        }
                        cursor = cursor0
                        continue
                    }
                    PointerEventType.Scroll -> {
                        calcScroll(event, noteWidth, horizontalScrollState, coroutineScope) {
                            val newValue = (noteHeight.value + (if (it.scrollDelta.y > 0) -1 else 1)).coerceIn(8F, 32F)
                            if (newValue == noteHeight.value) return@calcScroll
                            val y = it.position.x
                            val oldY = (y + verticalScrollState.value) / noteHeight.toPx()
                            noteHeight = newValue.dp
                            coroutineScope.launch {
                                val noteHeightPx = noteHeight.toPx()
                                verticalScrollState.scrollBy(
                                    (oldY - (y + verticalScrollState.value) / noteHeightPx) * noteHeightPx
                                )
                            }
                        }
                        continue
                    }
                    PointerEventType.Press -> {
                        isEventPanelActive = false
                        if (event.buttons.isPrimaryPressed) {
                            if (event.keyboardModifiers.isCtrlPressed) {
                                action = EditAction.SELECT
                                break
                            }
                            var currentSelectNote = getClickedNotes(event.x, event.y, editor)
                            if (currentSelectNote == null) {
                                val noteUnit = getEditUnit()
                                currentSelectedNote?.apply { lastSelectedNoteDuration = duration }
                                currentSelectNote = defaultNoteMessage(currentNote, currentX.fitInUnit(noteUnit),
                                    lastSelectedNoteDuration.coerceAtLeast(noteUnit), defaultVelocity)
                                clip.doNoteAmountAction(listOf(currentSelectNote), false)
                                clip.clip.notes.sort()
                                clip.clip.notes.update()
                                selectedNotes.clear()
                                selectedNotes.add(currentSelectNote)
                            } else if (!selectedNotes.contains(currentSelectNote)) {
                                selectedNotes.clear()
                                selectedNotes.add(currentSelectNote)
                                action = EditAction.MOVE
                            }
                            currentSelectedNote = currentSelectNote
                            lastSelectedNoteDuration = currentSelectNote.duration

                            playNote(currentSelectNote, editor)

                            // check is move or resize
                            // if user click on start 4px and end -4px is resize
                            // else will move
                            val curTrueStartTime = currentSelectNote.time + clip.time
                            val startX = curTrueStartTime * noteWidth.value.toPx() - horizontalScrollState.value
                            val endX = (curTrueStartTime + currentSelectNote.duration) * noteWidth.value.toPx() -
                                    horizontalScrollState.value
                            val fourDp = 4 * density
                            if (event.x < startX + fourDp && event.x > startX - fourDp) {
                                resizeDirectionRight = false
                                action = EditAction.RESIZE
                                break
                            } else if (event.x < endX + fourDp && event.x > endX - fourDp) {
                                resizeDirectionRight = true
                                action = EditAction.RESIZE
                            } else action = EditAction.MOVE

                            // calc bound of all selected notes
                            selectedNotes.forEach {
                                val top = KEYBOARD_KEYS - it.note - 1
                                if (it.time < selectedNotesLeft) selectedNotesLeft = it.time
                                if (top > selectedNotesTop) selectedNotesTop = top
                                if (top < selectedNotesBottom) selectedNotesBottom = top
                                if (it.duration < minNoteDuration) minNoteDuration = it.duration
                            }
                            break
                        } else if (event.buttons.isForwardPressed) {
                            action = EditAction.SELECT
                            break
                        } else if (event.buttons.isTertiaryPressed) {
                            selectedNotes.clear()
                            getClickedNotes(event.x, event.y, editor) ?.let(deletionList::add)
                            action = EditAction.DELETE
                            break
                        } else if (event.buttons.isSecondaryPressed) {
                            selectedNotes.clear()
                            action = EditAction.NONE
                            openEditorMenu(floatingDialogProvider, event.changes[0].position, editor)
                            continue
                        }
                    }
                }
            } while (!event.changes.fastAll(PointerInputChange::changedToDownIgnoreConsumed))
            val down = event.changes[0]
            val downX = down.position.x + horizontalScrollState.value
            val downY = down.position.y + verticalScrollState.value

            var drag: PointerInputChange?
            do {
                @Suppress("INVISIBLE_MEMBER")
                drag = awaitPointerSlopOrCancellation(down.id, down.type,
                    triggerOnMainAxisSlop = false) { change, _ -> change.consume() }
            } while (drag != null && !drag.isConsumed)
            if (drag == null) {
                if (action == EditAction.DELETE) {
                    clip.doNoteAmountAction(deletionList, true)
                    deletionList.clear()
                }
                stopAllNotes(editor)
                action = EditAction.NONE
                return@awaitPointerEventScope
            }
            if (action == EditAction.SELECT) {
                selectedNotes.clear()
                selectionStartX = downX
                selectionStartY = (downY / noteHeight.toPx()).toInt()
                selectionX = selectionStartX
                selectionY = selectionStartY
            }

            drag(drag.id) {
                when (action) {
                    EditAction.SELECT -> {
                        selectionX = (it.position.x.coerceAtMost(size.width.toFloat()) + horizontalScrollState.value)
                            .coerceAtLeast(0F)
                        selectionY = ((it.position.y.coerceAtMost(size.height.toFloat()) + verticalScrollState.value) /
                                noteHeight.toPx()).roundToInt()
                    }
                    EditAction.DELETE -> getClickedNotes(it.position.x, it.position.y, editor) { note ->
                        !deletionList.contains(note)
                    }?.let(deletionList::add)
                    EditAction.MOVE, EditAction.RESIZE -> {
                        val noteUnit = getEditUnit()
                        // calc delta in noteUnit, then check all move notes are in bound
                        var x = (((it.position.x + horizontalScrollState.value).coerceAtLeast(0F) - downX) /
                                noteWidth.value.toPx()).fitInUnit(noteUnit)
                        var y = (((it.position.y + verticalScrollState.value).coerceAtLeast(0F) - downY) /
                                noteHeight.toPx()).roundToInt()
                        if (action == EditAction.MOVE) {
                            if (selectedNotesLeft + x + clip.time < 0) x = -(selectedNotesLeft + clip.time)
                            if (selectedNotesTop + y > KEYBOARD_KEYS - 1) y = KEYBOARD_KEYS - 1 - selectedNotesTop
                            if (selectedNotesBottom + y < 0) y = -selectedNotesBottom
                            if (y != deltaY) {
                                deltaY = y
                                stopAllNotes(editor)
                                val note = currentSelectedNote
                                if (note != null) playNote(note.copy(note = note.note - y), editor)
                            }
                        } else {
                            if (resizeDirectionRight) {
                                if (minNoteDuration + x < noteUnit) x = noteUnit - minNoteDuration
                            } else {
                                if (x > 0) x = 0
                                if (selectedNotesLeft + x < 0) x = -selectedNotesLeft
                            }
                        }
                        if (x != deltaX) deltaX = x
                    }
                    else -> { }
                }
                // detect out of frame then auto scroll
                if (it.position.x < 0) coroutineScope.launch { horizontalScrollState.scrollBy(-1F) }
                else if (it.position.x > size.width - 5) coroutineScope.launch { horizontalScrollState.scrollBy(1F) }
                if (it.position.y < 0) coroutineScope.launch { verticalScrollState.scrollBy(-1F) }
                else if (it.position.y > size.height) coroutineScope.launch { verticalScrollState.scrollBy(1F) }
                it.consume()
            }
            // calc selected notes
            when (action) {
                EditAction.SELECT -> {
                    val startX = (selectionStartX / noteWidth.value.toPx()).roundToInt()
                    val endX = (selectionX / noteWidth.value.toPx()).roundToInt()
                    val startY = KEYBOARD_KEYS - selectionStartY - 1
                    val endY = KEYBOARD_KEYS - selectionY - 1
                    selectionStartY = 0
                    selectionStartX = 0F
                    selectionX = 0F
                    selectionY = 0
                    val minX = minOf(startX, endX)
                    val maxX = maxOf(startX, endX)
                    val minY = minOf(startY, endY)
                    val maxY = maxOf(startY, endY)
                    val list = arrayListOf<NoteMessage>()
                    val startTime = clip.time
                    for (i in startNoteIndex until clip.clip.notes.size) {
                        val note = clip.clip.notes[i]
                        if (startTime + note.time > maxX) break
                        if (startTime + note.time + note.duration >= minX && note.note in minY..maxY) list.add(note)
                    }
                    selectedNotes.addAll(list)
                }
                EditAction.DELETE -> {
                    clip.doNoteAmountAction(deletionList, true)
                    deletionList.clear()
                }
                EditAction.MOVE -> clip.doNoteMessageEditAction(selectedNotes,
                    deltaX, -deltaY, 0)
                EditAction.RESIZE -> {
                    if (resizeDirectionRight)
                        clip.doNoteMessageEditAction(selectedNotes, 0, 0, deltaX)
                    else clip.doNoteMessageEditAction(selectedNotes, deltaX, 0, -deltaX)
                }
                else -> { }
            }
            deltaX = 0
            deltaY = 0
            action = EditAction.NONE
            stopAllNotes(editor)
        }
    } }
}
