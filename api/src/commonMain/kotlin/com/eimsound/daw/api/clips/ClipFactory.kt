package com.eimsound.daw.api.clips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.eimsound.audioprocessor.CurrentPosition
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.commons.MultiSelectableEditor
import com.eimsound.daw.commons.SerializableEditor
import com.eimsound.dsp.data.midi.MidiNoteTimeRecorder
import kotlinx.serialization.json.JsonObject
import java.nio.file.Path
import java.util.ArrayList


interface ClipEditor : MultiSelectableEditor {
    @Composable
    fun Editor()
}

interface MidiClipEditor: ClipEditor, SerializableEditor, MultiSelectableEditor {
    val clip: TrackClip<MidiClip>
}

data class ClipActionResult<T: Clip>(val clip: T, val time: Int, val duration: Int, val start: Int = 0)

interface ClipSplitResult<T: Clip> {
    val clip: T
    val start: Int
    fun revert()
}

interface ClipFactory<T: Clip> {
    val name: String
    fun createClip(): T
    fun createClip(path: Path, json: JsonObject): T
    fun processBlock(
        clip: TrackClip<T>, buffers: Array<FloatArray>, position: CurrentPosition,
        midiBuffer: ArrayList<Int>, noteRecorder: MidiNoteTimeRecorder
    )
    fun save(clip: T, path: Path) { }
    fun getEditor(clip: TrackClip<T>): ClipEditor?
    fun split(clip: TrackClip<T>, time: Int): ClipSplitResult<T>
    fun copy(clip: T): T
    fun merge(clips: Collection<TrackClip<*>>): List<ClipActionResult<T>>
    fun canMerge(clip: TrackClip<*>): Boolean

    @Composable
    fun PlaylistContent(
        clip: TrackClip<T>, track: Track, contentColor: Color,
        noteWidth: MutableState<Dp>, startPPQ: Float, widthPPQ: Float
    )

    @Composable
    fun MenuContent(clips: List<TrackClip<*>>, close: () -> Unit) { }
}
