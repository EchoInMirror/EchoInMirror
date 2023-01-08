package cn.apisium.eim.window.panels.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.zIndex
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.actions.doNoteAmountAction
import cn.apisium.eim.actions.doNoteMessageEditAction
import cn.apisium.eim.api.MidiClip
import cn.apisium.eim.api.asMidiTrackClipOrNull
import cn.apisium.eim.api.processor.Track
import cn.apisium.eim.api.projectDisplayPPQ
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.PanelDirection
import cn.apisium.eim.commands.*
import cn.apisium.eim.components.*
import cn.apisium.eim.components.dragdrop.dropTarget
import cn.apisium.eim.components.splitpane.VerticalSplitPane
import cn.apisium.eim.components.splitpane.rememberSplitPaneState
import cn.apisium.eim.data.defaultScale
import cn.apisium.eim.data.getEditUnit
import cn.apisium.eim.data.midi.*
import cn.apisium.eim.utils.*
import cn.apisium.eim.window.panels.editor.EditAction.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.Cursor
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

internal enum class EditAction {
    NONE, MOVE, RESIZE, SELECT, DELETE
}

var selectedNotes by mutableStateOf(hashSetOf<NoteMessage>())
val backingTracks = mutableStateSetOf<Track>()
var startNoteIndex = 0
var noteHeight by mutableStateOf(16.dp)
var noteWidth = mutableStateOf(0.4.dp)
val verticalScrollState = ScrollState(noteHeight.value.toInt() * 50)
val horizontalScrollState = ScrollState(0).apply {
    openMaxValue = (noteWidth.value * EchoInMirror.currentPosition.projectDisplayPPQ).value.toInt()
}
var playOnEdit by mutableStateOf(true)
var defaultVelocity by mutableStateOf(70)
private var selectionStartX = 0F
private var selectionStartY = 0
private var selectionX by mutableStateOf(0F)
private var selectionY by mutableStateOf(0)
internal var deltaX by mutableStateOf(0)
private var deltaY by mutableStateOf(0)
internal var action by mutableStateOf(NONE)
private var resizeDirectionRight = false
private var cursor by mutableStateOf(Cursor.DEFAULT_CURSOR)
private var currentNote = 0
private var currentX = 0
private val deletionList = mutableStateSetOf<NoteMessage>()
private val playingNotes = arrayListOf<MidiEvent>()
internal var currentSelectedNote by mutableStateOf<NoteMessage?>(null)
internal var notesInView by mutableStateOf(arrayListOf<NoteMessage>())

private data class NoteDrawObject(val note: NoteMessage, val offset: Offset, val size: Size, val color: Color)
private data class BackingTrack(val track: Track, val notes: ArrayList<NoteDrawObject>)

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

private fun playNote(note: NoteMessage) {
    if (!playOnEdit) return
    val track = EchoInMirror.selectedTrack
    if (EchoInMirror.currentPosition.isPlaying || track == null) return
    playingNotes.add(note.toNoteOffEvent())
    track.playMidiEvent(note.toNoteOnEvent())
}

private fun stopAllNotes() {
    val track = EchoInMirror.selectedTrack
    if (playingNotes.isNotEmpty() && track != null) {
        if (!EchoInMirror.currentPosition.isPlaying) playingNotes.forEach { track.playMidiEvent(it) }
        playingNotes.clear()
    }
}

fun Density.calcScroll(event: PointerEvent, noteWidth: MutableState<Dp>, horizontalScrollState: ScrollState,
                       coroutineScope: CoroutineScope, onVerticalScroll: (PointerInputChange) -> Unit) {
    if (event.keyboardModifiers.isCtrlPressed) {
        val change = event.changes[0]
        if (event.keyboardModifiers.isAltPressed) onVerticalScroll(change)
        else {
            val x = change.position.x
            val oldX = (x + horizontalScrollState.value) / noteWidth.value.toPx()
            val newValue = (noteWidth.value.value +
                    (if (change.scrollDelta.y > 0) -0.05F else 0.05F)).coerceIn(0.06f, 3.2f)
            if (newValue != noteWidth.value.value) {
                horizontalScrollState.openMaxValue = (noteWidth.value * EchoInMirror.currentPosition.projectDisplayPPQ).toPx().toInt()
                noteWidth.value = newValue.dp
                coroutineScope.launch {
                    val noteWidthPx = noteWidth.value.toPx()
                    horizontalScrollState.scrollBy(
                        (oldX - (x + horizontalScrollState.value) / noteWidthPx) * noteWidthPx
                    )
                }
            }
        }
        change.consume()
    }
}

@Suppress("DuplicatedCode")
private suspend fun PointerInputScope.handleMouseEvent(coroutineScope: CoroutineScope) {
    forEachGesture {
        awaitPointerEventScope {
            var event: PointerEvent
            var selectedNotesLeft = Int.MAX_VALUE
            var selectedNotesTop = 0
            var selectedNotesBottom = Int.MAX_VALUE
            var minNoteDuration = Int.MAX_VALUE
            do {
                event = awaitPointerEvent(PointerEventPass.Main)
                if (EchoInMirror.selectedTrack == null) continue
                val trackClip = EchoInMirror.selectedClip?.asMidiTrackClipOrNull() ?: continue
                val clip = trackClip.clip
                when (event.type) {
                    PointerEventType.Move -> {
                        var cursor0 = Cursor.DEFAULT_CURSOR
                        getClickedNotes(event.x, event.y, clip.notes)?.let {
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
                        action = SELECT
                        break
                    }
                    var currentSelectNote = getClickedNotes(event.x, event.y, clip.notes)
                    if (currentSelectNote == null) {
                        selectedNotes = hashSetOf()
                        val noteUnit = getEditUnit()
                        currentSelectNote = defaultNoteMessage(currentNote, currentX.fitInUnit(noteUnit), noteUnit, defaultVelocity)
                        trackClip.doNoteAmountAction(listOf(currentSelectNote), false)
                        clip.notes.sort()
                        clip.notes.update()
                        selectedNotes.add(currentSelectNote)
                    } else if (!selectedNotes.contains(currentSelectNote)) {
                        selectedNotes = hashSetOf()
                        selectedNotes.add(currentSelectNote)
                        action = MOVE
                    }
                    currentSelectedNote = currentSelectNote

                    playNote(currentSelectNote)

                    // check is move or resize
                    // if user click on start 4px and end -4px is resize
                    // else will move
                    val startX = currentSelectNote.time * noteWidth.value.toPx() - horizontalScrollState.value
                    val endX = (currentSelectNote.time + currentSelectNote.duration) * noteWidth.value.toPx() - horizontalScrollState.value
                    if (event.x < startX + 4 && event.x > startX - 4) {
                        resizeDirectionRight = false
                        action = RESIZE
                        break
                    } else if (event.x < endX + 4 && event.x > endX - 4) {
                        resizeDirectionRight = true
                        action = RESIZE
                    } else action = MOVE

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
                    action = SELECT
                    break
                } else if (event.buttons.isTertiaryPressed) {
                    selectedNotes = hashSetOf()
                    getClickedNotes(event.x, event.y, clip.notes) ?.let(deletionList::add)
                    action = DELETE
                    break
                } else if (event.buttons.isSecondaryPressed) {
                    selectedNotes = hashSetOf()
                    action = NONE
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
            val track = EchoInMirror.selectedTrack
            val clip = EchoInMirror.selectedClip?.asMidiTrackClipOrNull()
            if (drag == null) {
                if (action == DELETE) {
                    clip?.doNoteAmountAction(deletionList, true)
                    deletionList.clear()
                }
                stopAllNotes()
                action = NONE
                return@awaitPointerEventScope
            }
            if (action == SELECT) {
                selectedNotes = hashSetOf()
                selectionStartX = downX
                selectionStartY = (downY / noteHeight.toPx()).toInt()
                selectionX = selectionStartX
                selectionY = selectionStartY
            }

            drag(drag.id) {
                when (action) {
                    SELECT -> {
                        selectionX = (it.position.x.coerceAtMost(size.width.toFloat()) + horizontalScrollState.value).coerceAtLeast(0F)
                        selectionY = ((it.position.y.coerceAtMost(size.height.toFloat()) + verticalScrollState.value) / noteHeight.toPx()).roundToInt()
                    }
                    DELETE -> if (track != null && clip is MidiClip) getClickedNotes(it.position.x, it.position.y, clip.notes)
                        { note -> !deletionList.contains(note) }?.let(deletionList::add)
                    MOVE, RESIZE -> {
                        val noteUnit = getEditUnit()
                        // calc delta in noteUnit, then check all move notes are in bound
                        deltaX = (((it.position.x + horizontalScrollState.value).coerceAtLeast(0F) - downX) /
                                noteWidth.value.toPx()).fitInUnit(noteUnit)
                        val prevDeltaY = deltaY
                        deltaY = (((it.position.y + verticalScrollState.value).coerceAtLeast(0F) - downY) /
                                noteHeight.toPx()).roundToInt()
                        if (action == MOVE) {
                            if (selectedNotesLeft + deltaX < 0) deltaX = -selectedNotesLeft
                            if (selectedNotesTop + deltaY > KEYBOARD_KEYS - 1) deltaY = KEYBOARD_KEYS - 1 - selectedNotesTop
                            if (selectedNotesBottom + deltaY < 0) deltaY = -selectedNotesBottom
                            if (deltaY != prevDeltaY) {
                                stopAllNotes()
                                val note = currentSelectedNote
                                if (note != null) playNote(note.copy(note = note.note - deltaY))
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
            if (track != null && clip is MidiClip) when (action) {
                SELECT -> {
                    val startX = (selectionStartX / noteWidth.value.toPx()).roundToInt()
                    val endX = (selectionX / noteWidth.value.toPx()).roundToInt()
                    val startY = KEYBOARD_KEYS - selectionStartY - 1
                    val endY = KEYBOARD_KEYS - selectionY - 1
                    val minX = minOf(startX, endX)
                    val maxX = maxOf(startX, endX)
                    val minY = minOf(startY, endY)
                    val maxY = maxOf(startY, endY)
                    val list = arrayListOf<NoteMessage>()
                    for (i in startNoteIndex until clip.notes.size) {
                        val note = clip.notes[i]
                        if (note.time > maxX) break
                        if (note.time + note.duration >= minX && note.note in minY..maxY) list.add(note)
                    }
                    selectedNotes.addAll(list)
                }
                DELETE -> {
                    clip.doNoteAmountAction(deletionList, true)
                    deletionList.clear()
                }
                MOVE -> clip.doNoteMessageEditAction(selectedNotes.toTypedArray(), deltaX, -deltaY, 0)
                RESIZE -> {
                    if (resizeDirectionRight) clip.doNoteMessageEditAction(selectedNotes.toTypedArray(), 0, 0, deltaX)
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
            action = NONE
            stopAllNotes()
        }
    }
}

@Suppress("DuplicatedCode")
@Composable
private fun NotesEditorCanvas() {
    val coroutineScope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize().clipToBounds().background(MaterialTheme.colorScheme.background)
        .scrollable(verticalScrollState, Orientation.Vertical, reverseDirection = true)
        .pointerInput(Unit) { handleMouseEvent(coroutineScope) }
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
        EditorGrid(noteWidth, horizontalScrollState)
        Spacer(Modifier.fillMaxSize().drawWithCache {
            val noteWidthPx = noteWidth.value.toPx()
            val verticalScrollValue = verticalScrollState.value
            val horizontalScrollValue = horizontalScrollState.value
            val noteHeightPx = noteHeight.toPx()
            val notes = arrayListOf<NoteDrawObject>()
            val backingsNotes = arrayListOf<BackingTrack>()
            action // read mutable state of action
            EchoInMirror.selectedClip?.let { trackClip ->
                val clip = trackClip.clip
                if (clip !is MidiClip) return@let
                val startTime = trackClip.time
                startNoteIndex = 0
                var flag = true
                clip.notes.read()
                val allNotes = clip.notes.toSet()
                selectedNotes.removeIf { !allNotes.contains(it) }

                val notesInViewList = arrayListOf<NoteMessage>()
                val color = EchoInMirror.selectedTrack!!.color
                for ((index, it) in clip.notes.withIndex()) {
                    val y = (KEYBOARD_KEYS - 1 - it.note) * noteHeightPx - verticalScrollValue
                    val x = startTime + it.time * noteWidthPx - horizontalScrollValue
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
                        color.copy(0.6F + 0.4F * mapValue(it.velocity, 0, 127)))
                    )
                }
                notesInView = notesInViewList
            }
//           TODO: backingTracks.forEach { track ->
//                if (track == EchoInMirror.selectedTrack) return@forEach
//                track.notes.read()
//                val color = track.color
//                val curNotes = arrayListOf<NoteDrawObject>()
//                for (it in track.notes) {
//                    val y = (KEYBOARD_KEYS - 1 - it.note) * noteHeightPx - verticalScrollValue
//                    val x = it.time * noteWidthPx - horizontalScrollValue
//                    if (x > size.width) break
//                    if (y < -noteHeightPx || y > size.height || deletionList.contains(it)) continue
//                    val width = it.duration * noteWidthPx
//                    if (x < 0 && width < -x) continue
//                    curNotes.add(
//                        NoteDrawObject(it, Offset(x, y.coerceAtLeast(0F)), Size(width, if (y < 0)
//                        (noteHeightPx + y).coerceAtLeast(0F) else noteHeightPx),
//                        color.copy(0.6F + 0.4F * mapValue(it.velocity, 0, 127)))
//                    )
//                }
//                backingsNotes.add(BackingTrack(track, curNotes))
//            }

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
                val track = EchoInMirror.selectedTrack ?: return@onDrawBehind
                val trackClip = EchoInMirror.selectedClip ?: return@onDrawBehind
                val trackColor = track.color
                selectedNotes.forEach {
                    var y = (KEYBOARD_KEYS - 1 - it.note) * noteHeightPx - verticalScrollValue
                    val x = trackClip.time + it.time * noteWidthPx - horizontalScrollValue
                    val width = it.duration * noteWidthPx
                    val offset: Offset
                    val size: Size
                    when (action) {
                        MOVE -> {
                            y += deltaY * noteHeightPx
                            offset = Offset(x + deltaX * noteWidthPx, y.coerceAtLeast(0F))
                            size = Size(width, if (y < 0) (noteHeightPx + y).coerceAtLeast(0F) else noteHeightPx)
                        }
                        RESIZE -> {
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
                    drawRoundRect(trackColor.copy(0.6F + 0.4F * mapValue(it.velocity, 0, 127)),
                        offset, size, BorderCornerRadius2PX)
                    drawRoundRect(primaryColor, offset, size, BorderCornerRadius2PX, Stroke1_5PX)
                }
            }
        })
        Canvas(Modifier.fillMaxSize().pointerHoverIcon(PointerIcon(Cursor(
            when (action) {
                MOVE -> Cursor.MOVE_CURSOR
                RESIZE -> Cursor.E_RESIZE_CURSOR
                DELETE -> Cursor.DEFAULT_CURSOR
                else -> cursor
            }
        )))) {
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
            drawRect(primaryColor.copy(alpha = 0.1F), pos, size)
            drawRect(primaryColor, pos, size, style = Stroke1PX)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun editorContent() {
    VerticalSplitPane(splitPaneState = rememberSplitPaneState(100F)) {
        first(0.dp) {
            Column(Modifier.fillMaxSize().onPointerEvent(PointerEventType.Press) {
                EchoInMirror.windowManager.activePanel = Editor
            }) {
                val localDensity = LocalDensity.current
                var contentWidth by remember { mutableStateOf(0.dp) }
                Timeline(Modifier.zIndex(3F), noteWidth, horizontalScrollState, false, 68.dp)
                Box(Modifier.weight(1F).onGloballyPositioned { with(localDensity) { contentWidth = it.size.width.toDp() } }) {
                    Row(Modifier.fillMaxSize().zIndex(-1F)) {
                        Surface(Modifier.verticalScroll(verticalScrollState).zIndex(5f), shadowElevation = 4.dp) {
                            Keyboard(
                                { it, p -> EchoInMirror.selectedTrack?.playMidiEvent(noteOn(0, it, (127 * p).toInt())) },
                                { it, _ -> EchoInMirror.selectedTrack?.playMidiEvent(noteOff(0, it)) },
                                Modifier, noteHeight
                            )
                        }
                        NotesEditorCanvas()
                    }
                    PlayHead(noteWidth, horizontalScrollState, contentWidth, 68.dp)
                    VerticalScrollbar(
                        rememberScrollbarAdapter(verticalScrollState),
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                    )
                }
            }
        }
        second(20.dp) { EventEditor() }

        splitter { visiblePart { Divider() } }
    }

}

object Editor: Panel {
    override val name = "编辑器"
    override val direction = PanelDirection.Horizontal

    init { registerCommandHandlers() }

    @Composable
    override fun icon() {
        Icon(Icons.Default.Piano, "Editor")
    }

    @Composable
    override fun content() {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(200.dp)) { EditorControls() }
            Surface(Modifier.fillMaxSize(), shadowElevation = 2.dp) {
                Column {
                    Box {
                        Column(Modifier.fillMaxSize()) { editorContent() }
                        HorizontalScrollbar(
                            rememberScrollbarAdapter(horizontalScrollState),
                            Modifier.align(Alignment.TopStart).fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
