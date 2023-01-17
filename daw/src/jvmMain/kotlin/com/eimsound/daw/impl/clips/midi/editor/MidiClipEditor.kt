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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.eimsound.audioprocessor.data.midi.*
import com.eimsound.audioprocessor.projectDisplayPPQ
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.actions.doNoteAmountAction
import com.eimsound.daw.api.ClipEditor
import com.eimsound.daw.api.MidiClip
import com.eimsound.daw.api.TrackClip
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.components.Keyboard
import com.eimsound.daw.components.PlayHead
import com.eimsound.daw.components.Timeline
import com.eimsound.daw.components.splitpane.VerticalSplitPane
import com.eimsound.daw.components.splitpane.rememberSplitPaneState
import com.eimsound.daw.data.getEditUnit
import com.eimsound.daw.utils.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.math.roundToInt

var selectedNotes by mutableStateOf(hashSetOf<NoteMessage>())
val backingTracks = mutableStateSetOf<Track>()
var startNoteIndex = 0
var noteHeight by mutableStateOf(16.dp)
var noteWidth = mutableStateOf(0.4.dp)
val verticalScrollState = ScrollState(noteHeight.value.toInt() * 50)
val horizontalScrollState = ScrollState(0).apply {
    openMaxValue = (noteWidth.value * EchoInMirror.currentPosition.projectDisplayPPQ).value.toInt()
}

@Composable
private fun EditorContent(clip: TrackClip<MidiClip>, track: Track) {
    VerticalSplitPane(splitPaneState = rememberSplitPaneState(100F)) {
        first(0.dp) {
            Column(Modifier.fillMaxSize()) {
                val localDensity = LocalDensity.current
                var contentWidth by remember { mutableStateOf(0.dp) }
                val range = remember(clip.time, clip.duration) { clip.time..(clip.time + clip.duration) }
                Timeline(Modifier.zIndex(3F), noteWidth, horizontalScrollState, range, 68.dp)
                Box(Modifier.weight(1F).onGloballyPositioned { with(localDensity) { contentWidth = it.size.width.toDp() } }) {
                    Row(Modifier.fillMaxSize().zIndex(-1F)) {
                        Surface(Modifier.verticalScroll(verticalScrollState).zIndex(5f), shadowElevation = 4.dp) {
                            Keyboard(
                                { it, p -> EchoInMirror.selectedTrack?.playMidiEvent(noteOn(0, it, (127 * p).toInt())) },
                                { it, _ -> EchoInMirror.selectedTrack?.playMidiEvent(noteOff(0, it)) },
                                Modifier, noteHeight
                            )
                        }
                        NotesEditorCanvas(clip, track)
                    }
                    PlayHead(noteWidth, horizontalScrollState, contentWidth, 68.dp)
                    VerticalScrollbar(
                        rememberScrollbarAdapter(verticalScrollState),
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                    )
                }
            }
        }
        second(20.dp) { EventEditor(clip) }

        splitter { visiblePart { Divider() } }
    }
}

class MidiClipEditor(private val clip: TrackClip<MidiClip>, private val track: Track) : ClipEditor {
    private var copiedNotes: List<NoteMessage>? = null
    @Composable
    override fun content() {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(200.dp)) { EditorControls(clip.clip) }
            Surface(Modifier.fillMaxSize(), shadowElevation = 2.dp) {
                Column {
                    Box {
                        Column(Modifier.fillMaxSize()) { EditorContent(clip, track) }
                        HorizontalScrollbar(
                            rememberScrollbarAdapter(horizontalScrollState),
                            Modifier.align(Alignment.TopStart).fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    override fun delete() {
        if (selectedNotes.isEmpty()) return
        clip.doNoteAmountAction(selectedNotes, true)
        selectedNotes = hashSetOf()
    }

    fun copyAsString() = if (selectedNotes.isEmpty()) "" else OBJECT_MAPPER.writeValueAsString(
        NoteMessageWithInfo(EchoInMirror.currentPosition.ppq, selectedNotes.toSet())
    )

    fun copyToClipboardAsString() {
        if (selectedNotes.isEmpty()) return
        CLIPBOARD_MANAGER?.setText(AnnotatedString(copyAsString()))
    }

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

    fun pasteFromString(content: String) {
        try {
            val data = ObjectMapper()
                .registerModule(kotlinModule())
                .registerModule(
                    SimpleModule()
                    .addAbstractTypeMapping(NoteMessage::class.java, NoteMessageImpl::class.java))
                .readValue<NoteMessageWithInfo>(content)
            val scale = EchoInMirror.currentPosition.ppq.toDouble() / data.ppq
            data.notes.forEach {
                it.time = (it.time * scale).roundToInt()
                it.duration = (it.duration * scale).roundToInt()
            }
            clip.doNoteAmountAction(data.notes)
            selectedNotes = data.notes.toHashSet()
        } catch (ignored: Throwable) { ignored.printStackTrace() }
    }

    fun pasteFromClipboard() {
        pasteFromString(CLIPBOARD_MANAGER?.getText()?.text ?: return)
    }

    override fun selectAll() {
        selectedNotes = clip.clip.notes.toHashSet()
    }
}