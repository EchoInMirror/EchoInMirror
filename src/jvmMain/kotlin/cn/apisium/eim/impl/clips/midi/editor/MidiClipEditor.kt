package cn.apisium.eim.impl.clips.midi.editor

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
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.actions.doNoteAmountAction
import cn.apisium.eim.api.*
import cn.apisium.eim.api.processor.Track
import cn.apisium.eim.components.Keyboard
import cn.apisium.eim.components.PlayHead
import cn.apisium.eim.components.Timeline
import cn.apisium.eim.components.splitpane.VerticalSplitPane
import cn.apisium.eim.components.splitpane.rememberSplitPaneState
import cn.apisium.eim.data.midi.*
import cn.apisium.eim.utils.CLIPBOARD_MANAGER
import cn.apisium.eim.utils.OBJECT_MAPPER
import cn.apisium.eim.utils.mutableStateSetOf
import cn.apisium.eim.utils.openMaxValue
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
        selectedNotes.clear()
    }

    override fun copy() {
        if (selectedNotes.isEmpty()) return
        CLIPBOARD_MANAGER?.setText(AnnotatedString(OBJECT_MAPPER.writeValueAsString(
            NoteMessageWithInfo(EchoInMirror.currentPosition.ppq, selectedNotes.toSet()))))
    }

    override fun cut() {
        if (EchoInMirror.selectedTrack == null) return
        copy()
        clip.doNoteAmountAction(selectedNotes, true)
        selectedNotes.clear()
    }

    override fun paste() {
        val content = CLIPBOARD_MANAGER?.getText()?.text ?: return
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
            selectedNotes.clear()
            selectedNotes.addAll(data.notes)
        } catch (ignored: Throwable) { ignored.printStackTrace() }
    }

    override fun selectAll() {
        selectedNotes.clear()
        selectedNotes.addAll(clip.clip.notes)
    }
}