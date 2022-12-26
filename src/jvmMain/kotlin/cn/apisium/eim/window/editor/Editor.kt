package cn.apisium.eim.window.editor

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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.actions.doNoteAmountAction
import cn.apisium.eim.actions.doNoteMessageEditAction
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.PanelDirection
import cn.apisium.eim.commands.*
import cn.apisium.eim.components.*
import cn.apisium.eim.components.splitpane.VerticalSplitPane
import cn.apisium.eim.components.splitpane.rememberSplitPaneState
import cn.apisium.eim.data.midi.*
import cn.apisium.eim.utils.*
import cn.apisium.eim.window.editor.EditAction.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.awt.Cursor
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private enum class EditAction {
    NONE, MOVE, RESIZE, SELECT, DELETE;
    fun isLazy() = this == MOVE || this == RESIZE
}

val manualState = ManualState()
val selectedNotes = mutableStateSetOf<NoteMessage>()
var startNoteIndex = 0
val noteHeight by mutableStateOf(16.dp)
val noteWidth by mutableStateOf(0.4.dp)
val noteUnit by mutableStateOf(96)
val verticalScrollState = ScrollState(0)
val horizontalScrollState = ScrollState(0).apply {
    @Suppress("INVISIBLE_SETTER")
    maxValue = 10000
}
private var selectionStartX = 0F
private var selectionStartY = 0
private var selectionX by mutableStateOf(0F)
private var selectionY by mutableStateOf(0)
private var deltaX by mutableStateOf(0)
private var deltaY by mutableStateOf(0)
private var action by mutableStateOf(NONE)
private var resizeDirectionRight = false
private var cursor by mutableStateOf(Cursor.DEFAULT_CURSOR)
private var currentNote = 0
private var currentX = 0
private val deletionList = mutableStateSetOf<NoteMessage>()

private data class NoteDrawObject(val note: NoteMessage, val offset: Offset, val size: Size)

private fun getClickedNotes(x: Float, y: Float, notes: NoteMessageList, block: (NoteMessage) -> Boolean = { true }): NoteMessage? {
    currentNote = KEYBOARD_KEYS - ((y + verticalScrollState.value) / noteHeight.value).toInt() - 1
    currentX = ((x + horizontalScrollState.value) / noteWidth.value).roundToInt()
    for (i in startNoteIndex until notes.size) {
        val note = notes[i]
        if (note.time > currentX) break
        if (note.note == currentNote && note.time <= currentX && note.time + note.duration >= currentX && block(note))
            return note
    }
    return null
}

@OptIn(DelicateCoroutinesApi::class)
private suspend fun PointerInputScope.handleDrag() {
    forEachGesture {
        awaitPointerEventScope {
            var event: PointerEvent
            var selectedNotesLeft = Int.MAX_VALUE
            var selectedNotesTop = 0
            var selectedNotesBottom = Int.MAX_VALUE
            var minNoteDuration = Int.MAX_VALUE
            do {
                event = awaitPointerEvent(PointerEventPass.Main)
                val track = EchoInMirror.selectedTrack ?: continue
                if (event.type == PointerEventType.Move) {
                    var cursor0 = Cursor.DEFAULT_CURSOR
                    getClickedNotes(event.x, event.y, track.notes)?.let {
                        val startX = it.time * noteWidth.value - horizontalScrollState.value
                        val endX = (it.time + it.duration) * noteWidth.value - horizontalScrollState.value
                        cursor0 = if ((event.x < startX + 4 && event.x > startX - 4) ||
                            (event.x < endX + 4 && event.x > endX - 4)) Cursor.E_RESIZE_CURSOR
                        else Cursor.MOVE_CURSOR
                    }
                    cursor = cursor0
                    continue
                }

                if (event.buttons.isPrimaryPressed) {
                    if (event.keyboardModifiers.isCtrlPressed) {
                        action = SELECT
                        break
                    }
                    var currentSelectNote = getClickedNotes(event.x, event.y, track.notes)
                    if (currentSelectNote == null) {
                        selectedNotes.clear()
                        currentSelectNote = defaultNoteMessage(currentNote, currentX.fitInUnit(noteUnit), noteUnit)
                        track.doNoteAmountAction(listOf(currentSelectNote), false)
                        track.notes.sort()
                        track.notes.update()
                        selectedNotes.add(currentSelectNote)
                    } else if (!selectedNotes.contains(currentSelectNote)) {
                        selectedNotes.clear()
                        selectedNotes.add(currentSelectNote)
                        action = MOVE
                    }

                    // check is move or resize
                    // if user click on start 4px and end -4px is resize
                    // else will move
                    val startX = currentSelectNote.time * noteWidth.value - horizontalScrollState.value
                    val endX = (currentSelectNote.time + currentSelectNote.duration) * noteWidth.value - horizontalScrollState.value
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
                    selectedNotes.clear()
                    getClickedNotes(event.x, event.y, track.notes) ?.let(deletionList::add)
                    action = DELETE
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
            if (drag == null) {
                if (action == DELETE) {
                    track?.doNoteAmountAction(deletionList, true)
                    deletionList.clear()
                }
                action = NONE
                return@awaitPointerEventScope
            }
            if (action == SELECT) {
                selectedNotes.clear()
                selectionStartX = downX
                selectionStartY = (downY / noteHeight.value).toInt()
                selectionX = selectionStartX
                selectionY = selectionStartY
            }

            drag(drag.id) {
                when (action) {
                    SELECT -> {
                        selectionX = (it.position.x.coerceAtMost(size.width.toFloat()) + horizontalScrollState.value).coerceAtLeast(0F)
                        selectionY = ((it.position.y.coerceAtMost(size.height.toFloat()) + verticalScrollState.value) / noteHeight.value).roundToInt()
                    }
                    DELETE -> if (track != null) getClickedNotes(it.position.x, it.position.y, track.notes)
                        { note -> !deletionList.contains(note) }?.let(deletionList::add)
                    MOVE, RESIZE -> {
                        // calc delta in noteUnit, then check all move notes are in bound
                        deltaX = (((it.position.x + horizontalScrollState.value).coerceAtLeast(0F) - downX) /
                                noteWidth.value).fitInUnit(noteUnit)
                        deltaY = (((it.position.y + verticalScrollState.value).coerceAtLeast(0F) - downY) /
                                noteHeight.value).roundToInt()
                        if (action == MOVE) {
                            if (selectedNotesLeft + deltaX < 0) deltaX = -selectedNotesLeft
                            if (selectedNotesTop + deltaY > KEYBOARD_KEYS - 1) deltaY = KEYBOARD_KEYS - 1 - selectedNotesTop
                            if (selectedNotesBottom + deltaY < 0) deltaY = -selectedNotesBottom
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
                if (it.position.x < 0) GlobalScope.launch { horizontalScrollState.scrollBy(-1F) }
                if (it.position.x > size.width - 5) GlobalScope.launch { horizontalScrollState.scrollBy(1F) }
                if (it.position.y < 0) GlobalScope.launch { verticalScrollState.scrollBy(-1F) }
                if (it.position.y > size.height) GlobalScope.launch { verticalScrollState.scrollBy(1F) }
                it.consume()
            }
            // calc selected notes
            if (track != null) when (action) {
                SELECT -> {
                    val startX = (selectionStartX / noteWidth.value).roundToInt()
                    val endX = (selectionX / noteWidth.value).roundToInt()
                    val startY = KEYBOARD_KEYS - selectionStartY - 1
                    val endY = KEYBOARD_KEYS - selectionY - 1
                    val minX = minOf(startX, endX)
                    val maxX = maxOf(startX, endX)
                    val minY = minOf(startY, endY)
                    val maxY = maxOf(startY, endY)
                    val list = arrayListOf<NoteMessage>()
                    for (i in startNoteIndex until track.notes.size) {
                        val note = track.notes[i]
                        if (note.time > maxX) break
                        if (note.time + note.duration >= minX && note.note in minY..maxY) list.add(note)
                    }
                    selectedNotes.addAll(list)
                }
                DELETE -> {
                    track.doNoteAmountAction(deletionList, true)
                    deletionList.clear()
                }
                MOVE -> track.doNoteMessageEditAction(selectedNotes.toTypedArray(), deltaX, -deltaY, 0)
                RESIZE -> {
                    if (resizeDirectionRight) track.doNoteMessageEditAction(selectedNotes.toTypedArray(), 0, 0, deltaX)
                    else track.doNoteMessageEditAction(selectedNotes.toTypedArray(), deltaX, 0, -deltaX)
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
        }
    }
}

@Composable
private fun NotesEditorCanvas() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .scrollable(verticalScrollState, Orientation.Vertical, reverseDirection = true)
        .pointerInput(Unit) { handleDrag() }
    ) {
        val highlightNoteColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03F)
        val outlineColor = MaterialTheme.colorScheme.surfaceVariant
        val primaryColor = MaterialTheme.colorScheme.primary
        EditorGrid(noteWidth, horizontalScrollState)
        Spacer(Modifier.fillMaxSize().drawWithCache {
            val noteWidthPx = noteWidth.toPx()
            val verticalScrollValue = verticalScrollState.value
            val horizontalScrollValue = horizontalScrollState.value
            val noteHeightPx = noteHeight.toPx()
            val notes = arrayListOf<NoteDrawObject>()
            val isLazy = action.isLazy()
            EchoInMirror.selectedTrack?.let { track ->
                startNoteIndex = 0
                var flag = true
                track.notes.read()
                manualState.read()
                for ((index, it) in track.notes.withIndex()) {
                    val y = (KEYBOARD_KEYS - 1 - it.note) * noteHeightPx - verticalScrollValue
                    val x = it.time * noteWidthPx - horizontalScrollValue
                    if (y < -noteHeightPx || y > size.height || deletionList.contains(it)) continue
                    val width = it.duration * noteWidthPx
                    if (x < 0 && width < -x) continue
                    if (x > size.width) break
                    if (flag) {
                        startNoteIndex = index
                        flag = false
                    }
                    if (isLazy && selectedNotes.contains(it)) continue
                    notes.add(NoteDrawObject(it, Offset(x, y.coerceAtLeast(0F)), Size(width, if (y < 0)
                        (noteHeightPx + y).coerceAtLeast(0F) else noteHeightPx)))
                }
            }

            onDrawBehind {
                for (i in (verticalScrollValue / noteHeightPx).toInt()..((verticalScrollValue + size.height) / noteHeightPx).toInt()) {
                    val y = i * noteHeightPx - verticalScrollValue
                    if (y < 0) continue
                    drawLine(outlineColor, Offset(0f, y), Offset(size.width, y), 1F)
                    if (!scales[11 - i % 12]) drawRect(highlightNoteColor, Offset(0f, y), Size(size.width, noteHeightPx))
                }

                val track = EchoInMirror.selectedTrack ?: return@onDrawBehind
                val currentPPQ = EchoInMirror.currentPosition.timeInPPQ
                val isPlaying = EchoInMirror.currentPosition.isPlaying
                notes.forEach {
                    val color = if (isPlaying && it.note.time <= currentPPQ &&
                        it.note.time + it.note.duration >= currentPPQ) primaryColor else track.color
                    drawRoundRect(color, it.offset, it.size, BorderCornerRadius2PX)
                }
                selectedNotes.forEach {
                    var y = (KEYBOARD_KEYS - 1 - it.note) * noteHeightPx - verticalScrollValue
                    val x = it.time * noteWidthPx - horizontalScrollValue
                    val width = it.duration * noteWidthPx
                    val color = if (isPlaying && it.time <= currentPPQ &&
                        it.time + it.duration >= currentPPQ) primaryColor else track.color
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
                    drawRoundRect(color, offset, size, BorderCornerRadius2PX)
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
                val surfaceColor = getSurfaceColor(2.dp)
                Box(Modifier.drawWithContent {
                    drawContent()
                    drawRect(surfaceColor, Offset(0f, -8f), Size(size.width, 8F))
                }) {
                    Timeline(Modifier.zIndex(3F), noteWidth, horizontalScrollState, false, 68.dp)
                }
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
        second(0.dp) {
            Row {

            }
        }

        splitter {
            visiblePart {
                Divider()
            }
        }
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
            Column(Modifier.width(200.dp)) {

            }
            Surface(Modifier.fillMaxSize(), shadowElevation = 2.dp) {
                Column {
                    Box {
                        Column(Modifier.fillMaxSize()) { editorContent() }
                        HorizontalScrollbar(
                            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(),
                            adapter = rememberScrollbarAdapter(horizontalScrollState)
                        )
                    }
                }
            }
        }
    }
}