package com.eimsound.daw.impl.clips.midi.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.zIndex
import com.eimsound.audioprocessor.oneBarPPQ
import com.eimsound.audioprocessor.projectDisplayPPQ
import com.eimsound.daw.actions.doNoteAmountAction
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.clips.MidiClip
import com.eimsound.daw.api.clips.MidiClipEditor
import com.eimsound.daw.api.clips.TrackClip
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.api.window.EditorExtension
import com.eimsound.daw.commons.IManualStateValue
import com.eimsound.daw.commons.ManualStateValue
import com.eimsound.daw.commons.json.JsonIgnoreDefaults
import com.eimsound.daw.commons.SerializableEditor
import com.eimsound.daw.components.*
import com.eimsound.daw.components.splitpane.VerticalSplitPane
import com.eimsound.daw.components.splitpane.rememberSplitPaneState
import com.eimsound.daw.components.utils.EditAction
import com.eimsound.daw.dawutils.openMaxValue
import com.eimsound.daw.utils.*
import com.eimsound.daw.window.panels.playlist.playlistTrackControllerMinWidth
import com.eimsound.daw.window.panels.playlist.Playlist
import com.eimsound.dsp.data.midi.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.math.roundToInt

// Focusable
@Composable
private fun EditorContent(editor: DefaultMidiClipEditor) {
    editor.apply {
        VerticalSplitPane(splitPaneState = rememberSplitPaneState(100F)) {
            first(TIMELINE_HEIGHT) {
                Column(Modifier.fillMaxSize().clearFocus().scalableNoteWidth(noteWidth, horizontalScrollState)) {
                    val localDensity = LocalDensity.current
                    var contentWidth by remember { mutableStateOf(0.dp) }
                    clip.track?.clips?.read()
                    val range = remember(clip.time, clip.duration) { clip.time..(clip.time + clip.duration) }
                    Timeline(Modifier.zIndex(3F), noteWidth, horizontalScrollState, range, 68.dp, EchoInMirror.editUnit,
                        EchoInMirror.currentPosition.oneBarPPQ,
                        { EchoInMirror.currentPosition.timeInPPQ = it }
                    ) {
                        clip.time = it.first
                        clip.duration = it.range
                        clip.track?.clips?.update()
                    }
                    Box(Modifier.weight(1F).onPlaced {
                        with(localDensity) { contentWidth = it.size.width.toDp() }
                    }) {
                        Row(Modifier.fillMaxSize().zIndex(-1F)) {
                            Surface(Modifier.verticalScroll(verticalScrollState).zIndex(5f), shadowElevation = 4.dp) {
                                Keyboard(
                                    { it, p -> EchoInMirror.selectedTrack?.playMidiEvent(noteOn(0, it, (127 * p).toInt())) },
                                    { it, _ -> EchoInMirror.selectedTrack?.playMidiEvent(noteOff(0, it)) },
                                    Modifier, noteHeight, swapColor = EchoInMirror.windowManager.isDarkTheme
                                )
                            }
                            NotesEditorCanvas(editor)
                        }
                        Box {
                            PlayHead(noteWidth, horizontalScrollState,
                                (EchoInMirror.currentPosition.ppqPosition * EchoInMirror.currentPosition.ppq).toFloat(),
                                contentWidth, 68.dp)
                        }
                        VerticalScrollbar(
                            rememberScrollbarAdapter(verticalScrollState),
                            Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                        )
                    }
                }
            }
            second(eventSelectorHeight) { EventEditor(editor) }

            splitter { visiblePart { Divider() } }
        }
    }
}

class DefaultMidiClipEditor(override val clip: TrackClip<MidiClip>) : MidiClipEditor {
    val selectedNotes = mutableStateSetOf<NoteMessage>()
    var noteHeight by mutableStateOf(16.dp)
    var noteWidth = mutableStateOf(0.4.dp)
    val verticalScrollState = ScrollState(noteHeight.value.toInt() * 50)
    val horizontalScrollState = ScrollState(0).apply {
        openMaxValue = (noteWidth.value * EchoInMirror.currentPosition.projectDisplayPPQ).value.toInt()
    }
    var selectedEvent: EventType? by mutableStateOf(VelocityEvent(this))
    internal val backingTracks: IManualStateValue<WeakHashMap<Track, Unit>> = ManualStateValue(WeakHashMap<Track, Unit>())
    internal var startNoteIndex = 0
    internal val playingNotes = arrayListOf<MidiEvent>()
    internal val deletionList = mutableStateSetOf<NoteMessage>()
    internal val muteList = mutableStateSetOf<NoteMessage>()
    internal var currentSelectedNote by mutableStateOf<NoteMessage?>(null)
    internal var notesInView by mutableStateOf(arrayListOf<NoteMessage>())
    internal var isEventPanelActive = false
    internal var offsetOfRoot = Offset.Zero

    internal var selectionStartX = 0F
    internal var selectionStartY = 0
    internal var selectionX by mutableStateOf(0F)
    internal var selectionY by mutableStateOf(0)
    internal var deltaX by mutableStateOf(0)
    internal var deltaY by mutableStateOf(0)
    internal var action by mutableStateOf(EditAction.NONE)
    internal var resizeDirectionRight = false
    internal var cursor by mutableStateOf(PointerIcon.Default)

    override val hasSelected get() = !selectedNotes.isEmpty()
    override val canPaste get() = copiedNotes?.isNotEmpty() == true

    companion object {
        var copiedNotes: List<NoteMessage>? = null
        var playOnEdit by mutableStateOf(true)
        var defaultVelocity by mutableStateOf(70)
        var lastSelectedNoteDuration = 0
        val notesEditorExtensions: MutableList<EditorExtension> = mutableStateListOf()
    }

    @Composable
    override fun Editor() {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width((Playlist.playlistTrackControllerPanelState.position / LocalDensity.current.density).dp
                .coerceAtLeast(playlistTrackControllerMinWidth))) {
                EditorControls(this@DefaultMidiClipEditor)
            }
            Surface(Modifier.fillMaxSize(), shadowElevation = 2.dp) {
                Box {
                    Column(Modifier.fillMaxSize()) { EditorContent(this@DefaultMidiClipEditor) }
                    HorizontalScrollbar(
                        rememberScrollbarAdapter(horizontalScrollState),
                        Modifier.align(Alignment.TopStart).fillMaxWidth()
                    )
                }
            }
        }
    }

    override fun delete() {
        if (isEventPanelActive) selectedEvent?.delete()
        else {
            if (selectedNotes.isEmpty()) return
            clip.doNoteAmountAction(selectedNotes, true)
            selectedNotes.clear()
        }
    }

    override fun copyAsString(): String {
        val editor = selectedEvent
        return if (isEventPanelActive && editor is SerializableEditor) editor.copyAsString()
        else if (selectedNotes.isEmpty()) "" else JsonIgnoreDefaults.encodeToString(
            SerializableNoteMessages(EchoInMirror.currentPosition.ppq, copyAsObject())
        )
    }

    private fun copyAsObject(): List<NoteMessage> {
        val startTime = selectedNotes.minOf { it.time }
        return selectedNotes.map { it.copy(time = it.time - startTime) }
    }

    override fun copy() {
        if (isEventPanelActive) selectedEvent?.copy()
        else if (selectedNotes.isNotEmpty()) copiedNotes = copyAsObject()
    }

    override fun paste() {
        if (isEventPanelActive) selectedEvent?.paste()
        else {
            if (copiedNotes.isNullOrEmpty()) return
            val startTime = EchoInMirror.currentPosition.timeInPPQ.fitInUnitCeil(EchoInMirror.editUnit)
            val notes = copiedNotes!!.fastMap { it.copy(time = it.time + startTime) }
            clip.doNoteAmountAction(notes, false)
            selectedNotes.clear()
            selectedNotes.addAll(notes)
        }
    }

    override fun canPasteFromString(value: String): Boolean {
        val editor = selectedEvent
        if (isEventPanelActive && editor is SerializableEditor) return editor.canPasteFromString(value)
        return value.contains("NoteMessages")
    }

    override fun pasteFromString(value: String) {
        val editor = selectedEvent
        if (isEventPanelActive && editor is SerializableEditor) {
            editor.pasteFromString(value)
            return
        }
        if (!value.contains("NoteMessages")) return
        try {
            val data = SerializableNoteMessages().apply { fromJson(Json.parseToJsonElement(value)) }
            val scale = EchoInMirror.currentPosition.ppq.toDouble() / data.ppq
            data.notes.forEach {
                it.time = (it.time * scale).roundToInt()
                it.duration = (it.duration * scale).roundToInt()
            }
            clip.doNoteAmountAction(data.notes)
            selectedNotes.clear()
            selectedNotes.addAll(data.notes)
        } catch (ignored: Throwable) { ignored.printStackTrace() }
    }

    override fun selectAll() {
        if (isEventPanelActive) selectedEvent?.selectAll()
        else {
            selectedNotes.clear()
            selectedNotes.addAll(clip.clip.notes)
        }
    }

    override fun duplicate() {
        if (selectedNotes.isEmpty()) return
        val startTime = EchoInMirror.editUnit.fitInUnitCeil(selectedNotes.maxOf { it.time + it.duration })
        val firstTime = selectedNotes.minOf { it.time }
        val notes = selectedNotes.map { it.copy(time = it.time - firstTime + startTime) }
        clip.doNoteAmountAction(notes, false)
        selectedNotes.clear()
        selectedNotes.addAll(notes)
    }

    internal var currentX = 0
    private var currentNote = 0
    internal inline fun Density.getClickedNotes(
        offset: Offset, block: Density.(NoteMessage) -> Boolean = { true }
    ): NoteMessage? {
        currentNote = KEYBOARD_KEYS - ((offset.y + verticalScrollState.value) / noteHeight.toPx()).toInt() - 1
        currentX = ((offset.x + horizontalScrollState.value) / noteWidth.value.toPx() - clip.time).roundToInt()
        for (i in startNoteIndex..clip.clip.notes.lastIndex) {
            val note = clip.clip.notes[i]
            val startTime = note.time
            if (startTime > currentX) break
            if (note.note == currentNote && startTime <= currentX && startTime + note.duration >= currentX && block(note))
                return note
        }
        return null
    }

    internal fun createNewNote(): NoteMessage {
        val noteUnit = EchoInMirror.editUnit
        currentSelectedNote?.apply { lastSelectedNoteDuration = duration }
        val newNote = defaultNoteMessage(
            currentNote,
            currentX.fitInUnit(noteUnit),
            lastSelectedNoteDuration.coerceAtLeast(noteUnit),
            defaultVelocity
        )
        clip.doNoteAmountAction(listOf(newNote), false)
        clip.clip.notes.sort()
        clip.clip.notes.update()
        selectedNotes.clear()
        selectedNotes.add(newNote)
        return newNote
    }
}
