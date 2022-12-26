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
import cn.apisium.eim.actions.doAddNoteMessage
import cn.apisium.eim.api.DeleteCommand
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.PanelDirection
import cn.apisium.eim.components.*
import cn.apisium.eim.components.splitpane.VerticalSplitPane
import cn.apisium.eim.components.splitpane.rememberSplitPaneState
import cn.apisium.eim.data.midi.NoteMessage
import cn.apisium.eim.data.midi.defaultNoteMessage
import cn.apisium.eim.data.midi.noteOff
import cn.apisium.eim.data.midi.noteOn
import cn.apisium.eim.utils.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

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

private data class NoteDrawObject(val note: NoteMessage, val offset: Offset, val size: Size)

@OptIn(DelicateCoroutinesApi::class)
private suspend fun PointerInputScope.handleDrag() {
    forEachGesture {
        awaitPointerEventScope {
            var event: PointerEvent
            val currentNote: Int
            val currentX: Int
            var isSelection = false
            do {
                event = awaitPointerEvent(PointerEventPass.Main)
                val track = EchoInMirror.selectedTrack ?: continue

                if (event.buttons.isPrimaryPressed) {
                    selectedNotes.clear()
                    if (event.keyboardModifiers.isCtrlPressed) {
                        isSelection = true
                        break
                    }
                    currentNote = KEYBOARD_KEYS - ((event.y + verticalScrollState.value) / noteHeight.value).toInt() - 1
                    currentX = ((event.x + horizontalScrollState.value) / noteWidth.value).roundToInt()
                    for (i in startNoteIndex until track.notes.size) {
                        val note = track.notes[i]
                        if (note.time > currentX) break
                        if (note.note == currentNote && note.time <= currentX && note.time + note.duration >= currentX) {
                            selectedNotes.add(note)
                            break
                        }
                    }
                    if (selectedNotes.isEmpty()) {
                        val note = defaultNoteMessage(currentNote, currentX.fitInUnit(noteUnit), noteUnit)
                        track.doAddNoteMessage(arrayOf(note), false)
                        track.notes.sort()
                        track.notes.update()
                        selectedNotes.add(note)
                    }
                    break
                } else if (event.buttons.isForwardPressed) {
                    selectedNotes.clear()
                    break
                }
            } while (!event.changes.fastAll(PointerInputChange::changedToDownIgnoreConsumed))
            val down = event.changes[0]

            var drag: PointerInputChange?
            do {
                @Suppress("INVISIBLE_MEMBER")
                drag = awaitPointerSlopOrCancellation(down.id, down.type,
                    triggerOnMainAxisSlop = false) {change, _ -> change.consume() }
            } while (drag != null && !drag.isConsumed)
            if (drag == null) return@awaitPointerEventScope
            if (isSelection || event.buttons.isForwardPressed) {
                selectionStartX = event.x + horizontalScrollState.value
                selectionStartY = ((event.y + verticalScrollState.value) / noteHeight.value).toInt()
            }

            drag(drag.id) {
                apply {
                    if (isSelection || event.buttons.isForwardPressed) {
                        selectionX = (it.position.x.coerceAtMost(size.width.toFloat()) + horizontalScrollState.value).coerceAtLeast(0F)
                        selectionY = ((it.position.y.coerceAtMost(size.height.toFloat()) + verticalScrollState.value) / noteHeight.value).roundToInt()
                        // detect out of frame then auto scroll
                        if (it.position.x < 0) GlobalScope.launch { horizontalScrollState.scrollBy(-1F) }
                        if (it.position.x > size.width - 5) GlobalScope.launch { horizontalScrollState.scrollBy(1F) }
                        if (it.position.y < 0) GlobalScope.launch { verticalScrollState.scrollBy(-1F) }
                        if (it.position.y > size.height) GlobalScope.launch { verticalScrollState.scrollBy(1F) }
                    }
                }
                it.consume()
            }
            // calc selected notes
            val track = EchoInMirror.selectedTrack
            if (track != null && (isSelection || event.buttons.isForwardPressed)) {
                val startX = (selectionStartX / noteWidth.value).roundToInt()
                val endX = (selectionX / noteWidth.value).roundToInt()
                val startY = KEYBOARD_KEYS - selectionStartY - 1
                val endY = KEYBOARD_KEYS - selectionY - 1
                val minX = minOf(startX, endX)
                val maxX = maxOf(startX, endX)
                val minY = minOf(startY, endY)
                val maxY = maxOf(startY, endY)
                for (i in startNoteIndex until track.notes.size) {
                    val note = track.notes[i]
                    if (note.time > maxX) break
                    if (note.time + note.duration >= minX && note.note in minY..maxY) {
                        selectedNotes.add(note)
                    }
                }
            }
            selectionStartY = 0
            selectionStartX = 0F
            selectionX = 0F
            selectionY = 0
        }
    }
}

@Composable
private fun NotesEditorCanvas() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .scrollable(verticalScrollState, Orientation.Vertical, reverseDirection = true)
        .pointerInput(Unit) { handleDrag() }) {
        val highlightNoteColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03F)
        val outlineColor = MaterialTheme.colorScheme.surfaceVariant
        val primaryColor = MaterialTheme.colorScheme.primary
        EditorGrid(noteWidth, horizontalScrollState)
        Spacer(Modifier.fillMaxSize().drawWithCache {
            val noteWidthPx = noteWidth.toPx()
            val verticalScrollValue = verticalScrollState.value
            val horizontalScrollValue = horizontalScrollState.value
            val noteHeightPx = noteHeight.toPx()
            val notes = EchoInMirror.selectedTrack?.let { track ->
                startNoteIndex = 0
                track.notes.read()
                manualState.read()
                val list = arrayListOf<NoteDrawObject>()
                for ((index, it) in track.notes.withIndex()) {
                    val y = (KEYBOARD_KEYS - 1 - it.note) * noteHeightPx - verticalScrollValue
                    val x = it.time * noteWidthPx - horizontalScrollValue
                    if (y < -noteHeightPx || y > size.height) continue
                    val width = it.duration * noteWidthPx
                    if (x < 0 && width < -x) continue
                    if (x > size.width) break
                    if (startNoteIndex == 0) startNoteIndex = index
                    val pos = Offset(x, y.coerceAtLeast(0F))
                    val size = Size(width, if (y < 0)
                        (noteHeightPx + y).coerceAtLeast(0F) else noteHeightPx)
                    list.add(NoteDrawObject(it, pos, size))
                }
                return@let list
            }
            onDrawBehind {
                for (i in (verticalScrollValue / noteHeightPx).toInt()..((verticalScrollValue + size.height) / noteHeightPx).toInt()) {
                    val y = i * noteHeightPx - verticalScrollValue
                    if (y < 0) continue
                    drawLine(
                        color = outlineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1F
                    )
                    if (scales[i % 12]) {
                        drawRect(
                            color = highlightNoteColor,
                            topLeft = Offset(0f, y),
                            size = Size(size.width, noteHeightPx)
                        )
                    }
                }

                val track = EchoInMirror.selectedTrack ?: return@onDrawBehind
                val invertsColor = track.color.inverts()
                val currentPPQ = EchoInMirror.currentPosition.timeInPPQ
                val isPlaying = EchoInMirror.currentPosition.isPlaying
                notes?.forEach {
                    val isPlayingNote = isPlaying && it.note.time <= currentPPQ && it.note.time + it.note.duration >= currentPPQ
                    drawRoundRect(if (isPlayingNote) invertsColor else track.color, it.offset, it.size, BorderCornerRadius2PX)
                    if (selectedNotes.contains(it.note)) {
                        drawRoundRect(primaryColor, it.offset, it.size, BorderCornerRadius2PX, Stroke1PX)
                    }
                }
            }
        })
        Canvas(Modifier.fillMaxSize()) {
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
                    Timeline(Modifier.zIndex(3F), noteWidth, horizontalScrollState, 68.dp)
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

    init {
        EchoInMirror.commandManager.registerCommandHandler(DeleteCommand) {
            if (EchoInMirror.windowManager.activePanel != Editor || selectedNotes.isEmpty()) return@registerCommandHandler
            val track = EchoInMirror.selectedTrack ?: return@registerCommandHandler
            track.doAddNoteMessage(selectedNotes.toTypedArray(), true)
            selectedNotes.clear()
        }
    }

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
                        Column(Modifier.padding(top = 8.dp).fillMaxSize()) { editorContent() }
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
