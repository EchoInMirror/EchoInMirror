package cn.apisium.eim.impl.clips.midi.editor

import androidx.compose.foundation.gestures.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.actions.doNoteAmountAction
import cn.apisium.eim.actions.doNoteMessageEditAction
import cn.apisium.eim.api.MidiClip
import cn.apisium.eim.api.TrackClip
import cn.apisium.eim.api.processor.Track
import cn.apisium.eim.components.KEYBOARD_KEYS
import cn.apisium.eim.data.getEditUnit
import cn.apisium.eim.data.midi.NoteMessage
import cn.apisium.eim.data.midi.defaultNoteMessage
import cn.apisium.eim.data.midi.toNoteOffEvent
import cn.apisium.eim.data.midi.toNoteOnEvent
import cn.apisium.eim.utils.fitInUnit
import cn.apisium.eim.utils.x
import cn.apisium.eim.utils.y
import cn.apisium.eim.window.panels.playlist.calcScroll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.Cursor
import kotlin.math.roundToInt

private fun Density.getClickedNotes(x: Float, y: Float, notes: List<NoteMessage>,
                                    block: Density.(NoteMessage) -> Boolean = { true }): NoteMessage? {
    currentNote = KEYBOARD_KEYS - ((y + verticalScrollState.value) / noteHeight.toPx()).toInt() - 1
    currentX = ((x + horizontalScrollState.value) / noteWidth.value.toPx()).roundToInt()
    for (i in startNoteIndex until notes.size) {
        val note = notes[i]
        if (note.time > currentX) break
        if (note.note == currentNote && note.time <= currentX && note.time + note.duration >= currentX && block(note))
            return note
    }
    return null
}

private fun playNote(note: NoteMessage, track: Track) {
    if (!playOnEdit) return
    if (EchoInMirror.currentPosition.isPlaying) return
    playingNotes.add(note.toNoteOffEvent())
    track.playMidiEvent(note.toNoteOnEvent())
}

private fun stopAllNotes(track: Track) {
    if (playingNotes.isNotEmpty()) {
        if (!EchoInMirror.currentPosition.isPlaying) playingNotes.forEach { track.playMidiEvent(it) }
        playingNotes.clear()
    }
}

@Suppress("DuplicatedCode")
internal suspend fun PointerInputScope.handleMouseEvent(coroutineScope: CoroutineScope,
                                                        clip: TrackClip<MidiClip>, track: Track) {
    forEachGesture {
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
                        var cursor0 = Cursor.DEFAULT_CURSOR
                        getClickedNotes(event.x, event.y, clip.clip.notes)?.let {
                            val startX = it.time * noteWidth.value.toPx() - horizontalScrollState.value
                            val endX = (it.time + it.duration) * noteWidth.value.toPx() - horizontalScrollState.value
                            cursor0 = if ((event.x < startX + 4 && event.x > startX - 4) ||
                                (event.x < endX + 4 && event.x > endX - 4)) Cursor.E_RESIZE_CURSOR
                            else Cursor.MOVE_CURSOR
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
                }

                if (event.buttons.isPrimaryPressed) {
                    if (event.keyboardModifiers.isCtrlPressed) {
                        action = EditAction.SELECT
                        break
                    }
                    var currentSelectNote = getClickedNotes(event.x, event.y, clip.clip.notes)
                    if (currentSelectNote == null) {
                        selectedNotes = hashSetOf()
                        val noteUnit = getEditUnit()
                        currentSelectNote = defaultNoteMessage(currentNote, currentX.fitInUnit(noteUnit),
                            noteUnit, defaultVelocity)
                        clip.doNoteAmountAction(listOf(currentSelectNote), false)
                        clip.clip.notes.sort()
                        clip.clip.notes.update()
                        selectedNotes.add(currentSelectNote)
                    } else if (!selectedNotes.contains(currentSelectNote)) {
                        selectedNotes = hashSetOf()
                        selectedNotes.add(currentSelectNote)
                        action = EditAction.MOVE
                    }
                    currentSelectedNote = currentSelectNote

                    playNote(currentSelectNote, track)

                    // check is move or resize
                    // if user click on start 4px and end -4px is resize
                    // else will move
                    val startX = currentSelectNote.time * noteWidth.value.toPx() - horizontalScrollState.value
                    val endX = (currentSelectNote.time + currentSelectNote.duration) * noteWidth.value.toPx() -
                            horizontalScrollState.value
                    if (event.x < startX + 4 && event.x > startX - 4) {
                        resizeDirectionRight = false
                        action = EditAction.RESIZE
                        break
                    } else if (event.x < endX + 4 && event.x > endX - 4) {
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
                    selectedNotes = hashSetOf()
                    getClickedNotes(event.x, event.y, clip.clip.notes) ?.let(deletionList::add)
                    action = EditAction.DELETE
                    break
                } else if (event.buttons.isSecondaryPressed) {
                    selectedNotes = hashSetOf()
                    action = EditAction.NONE
                    break
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
                stopAllNotes(track)
                action = EditAction.NONE
                return@awaitPointerEventScope
            }
            if (action == EditAction.SELECT) {
                selectedNotes = hashSetOf()
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
                    EditAction.DELETE -> getClickedNotes(it.position.x, it.position.y, clip.clip.notes)
                    { note -> !deletionList.contains(note) }?.let(deletionList::add)
                    EditAction.MOVE, EditAction.RESIZE -> {
                        val noteUnit = getEditUnit()
                        // calc delta in noteUnit, then check all move notes are in bound
                        deltaX = (((it.position.x + horizontalScrollState.value).coerceAtLeast(0F) - downX) /
                                noteWidth.value.toPx()).fitInUnit(noteUnit)
                        val prevDeltaY = deltaY
                        deltaY = (((it.position.y + verticalScrollState.value).coerceAtLeast(0F) - downY) /
                                noteHeight.toPx()).roundToInt()
                        if (action == EditAction.MOVE) {
                            if (selectedNotesLeft + deltaX < 0) deltaX = -selectedNotesLeft
                            if (selectedNotesTop + deltaY > KEYBOARD_KEYS - 1) deltaY = KEYBOARD_KEYS - 1 - selectedNotesTop
                            if (selectedNotesBottom + deltaY < 0) deltaY = -selectedNotesBottom
                            if (deltaY != prevDeltaY) {
                                stopAllNotes(track)
                                val note = currentSelectedNote
                                if (note != null) playNote(note.copy(note = note.note - deltaY), track)
                            }
                        } else {
                            if (resizeDirectionRight) {
                                if (minNoteDuration + deltaX < noteUnit) deltaX = noteUnit - minNoteDuration
                            } else {
                                if (deltaX > 0) deltaX = 0
                                if (selectedNotesLeft + deltaX < 0) deltaX = -selectedNotesLeft
                            }
                        }
                    }
                    else -> { }
                }
                // detect out of frame then auto scroll
                if (it.position.x < 0) coroutineScope.launch { horizontalScrollState.scrollBy(-1F) }
                if (it.position.x > size.width - 5) coroutineScope.launch { horizontalScrollState.scrollBy(1F) }
                if (it.position.y < 0) coroutineScope.launch { verticalScrollState.scrollBy(-1F) }
                if (it.position.y > size.height) coroutineScope.launch { verticalScrollState.scrollBy(1F) }
                it.consume()
            }
            // calc selected notes
            when (action) {
                EditAction.SELECT -> {
                    val startX = (selectionStartX / noteWidth.value.toPx()).roundToInt()
                    val endX = (selectionX / noteWidth.value.toPx()).roundToInt()
                    val startY = KEYBOARD_KEYS - selectionStartY - 1
                    val endY = KEYBOARD_KEYS - selectionY - 1
                    val minX = minOf(startX, endX)
                    val maxX = maxOf(startX, endX)
                    val minY = minOf(startY, endY)
                    val maxY = maxOf(startY, endY)
                    val list = arrayListOf<NoteMessage>()
                    for (i in startNoteIndex until clip.clip.notes.size) {
                        val note = clip.clip.notes[i]
                        if (note.time > maxX) break
                        if (note.time + note.duration >= minX && note.note in minY..maxY) list.add(note)
                    }
                    selectedNotes.addAll(list)
                }
                EditAction.DELETE -> {
                    clip.doNoteAmountAction(deletionList, true)
                    deletionList.clear()
                }
                EditAction.MOVE -> clip.doNoteMessageEditAction(selectedNotes.toTypedArray(),
                    deltaX, -deltaY, 0)
                EditAction.RESIZE -> {
                    if (resizeDirectionRight)
                        clip.doNoteMessageEditAction(selectedNotes.toTypedArray(), 0, 0, deltaX)
                    else clip.doNoteMessageEditAction(selectedNotes.toTypedArray(), deltaX, 0, -deltaX)
                }
                else -> { }
            }
            selectionStartY = 0
            selectionStartX = 0F
            selectionX = 0F
            selectionY = 0
            deltaX = 0
            deltaY = 0
            action = EditAction.NONE
            stopAllNotes(track)
        }
    }
}
