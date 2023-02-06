package com.eimsound.daw.impl.clips.midi.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.eimsound.audioprocessor.data.midi.*
import com.eimsound.audioprocessor.projectDisplayPPQ
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.actions.doNoteAmountAction
import com.eimsound.daw.api.MidiClip
import com.eimsound.daw.api.MidiClipEditor
import com.eimsound.daw.api.TrackClip
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.components.Keyboard
import com.eimsound.daw.components.PlayHead
import com.eimsound.daw.components.TIMELINE_HEIGHT
import com.eimsound.daw.components.Timeline
import com.eimsound.daw.components.splitpane.VerticalSplitPane
import com.eimsound.daw.components.splitpane.rememberSplitPaneState
import com.eimsound.daw.data.getEditUnit
import com.eimsound.daw.utils.fitInUnitCeil
import com.eimsound.daw.utils.mutableStateSetOf
import com.eimsound.daw.utils.openMaxValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.math.roundToInt

@Composable
private fun EditorContent(editor: DefaultMidiClipEditor) {
    editor.apply {
        VerticalSplitPane(splitPaneState = rememberSplitPaneState(100F)) {
            first(TIMELINE_HEIGHT) {
                Column(Modifier.fillMaxSize()) {
                    val localDensity = LocalDensity.current
                    var contentWidth by remember { mutableStateOf(0.dp) }
                    val clip = clip
                    val range = remember(clip.time, clip.duration) { clip.time..(clip.time + clip.duration) }
                    Timeline(Modifier.zIndex(3F), noteWidth, horizontalScrollState, range, 68.dp)
                    Box(Modifier.weight(1F).onGloballyPositioned {
                        with(localDensity) { contentWidth = it.size.width.toDp() }
                    }) {
                        Row(Modifier.fillMaxSize().zIndex(-1F)) {
                            Surface(Modifier.verticalScroll(verticalScrollState).zIndex(5f), shadowElevation = 4.dp) {
                                Keyboard(
                                    { it, p -> EchoInMirror.selectedTrack?.playMidiEvent(noteOn(0, it, (127 * p).toInt())) },
                                    { it, _ -> EchoInMirror.selectedTrack?.playMidiEvent(noteOff(0, it)) },
                                    Modifier, noteHeight
                                )
                            }
                            NotesEditorCanvas(editor)
                        }
                        PlayHead(noteWidth, horizontalScrollState, contentWidth, 68.dp)
                        VerticalScrollbar(
                            rememberScrollbarAdapter(verticalScrollState),
                            Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                        )
                    }
                }
            }
            second(20.dp) { EventEditor(editor) }

            splitter { visiblePart { Divider() } }
        }
    }
}

private var copiedNotes: List<NoteMessage>? = null

class DefaultMidiClipEditor(override val clip: TrackClip<MidiClip>, override val track: Track) : MidiClipEditor {
    internal val selectedNotes = mutableStateSetOf<NoteMessage>()
    internal val backingTracks = mutableStateSetOf<Track>()
    internal var startNoteIndex = 0
    internal var noteHeight by mutableStateOf(16.dp)
    internal var noteWidth = mutableStateOf(0.4.dp)
    internal val verticalScrollState = ScrollState(noteHeight.value.toInt() * 50)
    internal val horizontalScrollState = ScrollState(0).apply {
        openMaxValue = (noteWidth.value * EchoInMirror.currentPosition.projectDisplayPPQ).value.toInt()
    }
    internal var playOnEdit by mutableStateOf(true)
    internal var defaultVelocity by mutableStateOf(70)
    internal val playingNotes = arrayListOf<MidiEvent>()
    internal val deletionList = mutableStateSetOf<NoteMessage>()
    internal var currentSelectedNote by mutableStateOf<NoteMessage?>(null)
    internal var notesInView by mutableStateOf(arrayListOf<NoteMessage>())

    @Composable
    override fun Content() {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(200.dp)) { EditorControls(this@DefaultMidiClipEditor) }
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
        if (selectedNotes.isEmpty()) return
        clip.doNoteAmountAction(selectedNotes, true)
        selectedNotes.clear()
    }

    override fun copyAsString() = if (selectedNotes.isEmpty()) "" else jacksonObjectMapper().writeValueAsString(
        NoteMessageWithInfo(EchoInMirror.currentPosition.ppq, selectedNotes.toSet())
    )

//    fun copyToClipboardAsString() {
//        if (selectedNotes.isEmpty()) return
//        CLIPBOARD_MANAGER?.setText(AnnotatedString(copyAsString()))
//    }

    override fun copy() {
        if (selectedNotes.isEmpty()) return
        val startTime = selectedNotes.minOf { it.time }
        copiedNotes = selectedNotes.map { it.copy(time = it.time - startTime) }
    }

    override fun paste() {
        if (copiedNotes == null) return
        val startTime = EchoInMirror.currentPosition.timeInPPQ.fitInUnitCeil(getEditUnit())
        val notes = copiedNotes!!.map { it.copy(time = it.time + startTime) }
        clip.doNoteAmountAction(notes, false)
    }

    override fun cut() {
        if (EchoInMirror.selectedTrack == null) return
        copy()
        delete()
    }

    override fun pasteFromString(value: String) {
        try {
            val data = ObjectMapper()
                .registerModule(kotlinModule())
                .registerModule(
                    SimpleModule()
                    .addAbstractTypeMapping(NoteMessage::class.java, NoteMessageImpl::class.java))
                .readValue<NoteMessageWithInfo>(value)
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

//    fun pasteFromClipboard() {
//        pasteFromString(CLIPBOARD_MANAGER?.getText()?.text ?: return)
//    }

    override fun selectAll() {
        selectedNotes.clear()
        selectedNotes.addAll(clip.clip.notes)
    }
}