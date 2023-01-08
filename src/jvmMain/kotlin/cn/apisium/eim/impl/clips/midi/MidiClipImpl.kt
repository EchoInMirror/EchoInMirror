package cn.apisium.eim.impl.clips.midi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.apisium.eim.api.*
import cn.apisium.eim.api.processor.Track
import cn.apisium.eim.data.midi.*
import cn.apisium.eim.impl.clips.midi.editor.MidiClipEditor
import cn.apisium.eim.utils.randomId
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class MidiClipImpl(json: JsonNode?, override val factory: ClipFactory<MidiClip>) : MidiClip {
    override val notes = DefaultNoteMessageList()
    override val id = json?.get("id")?.asText() ?: randomId()

    init {
        if (json != null) {
            notes.addAll(json["notes"].traverse(jacksonObjectMapper()).readValueAs(NoteMessageListTypeReference))
            notes.update()
        }
    }
}

class MidiClipFactoryImpl : ClipFactory<MidiClip> {
    override val name = "MIDIClip"
    override fun createClip() = MidiClipImpl(null, this)
    override fun createClip(path: String, json: JsonNode) = MidiClipImpl(json, this)
    override fun getEditor(clip: TrackClip<MidiClip>, track: Track) = MidiClipEditor(clip, track)

    override fun processBlock(clip: TrackClip<MidiClip>, buffers: Array<FloatArray>, position: CurrentPosition,
                              midiBuffer: ArrayList<Int>, noteRecorder: MidiNoteRecorder, pendingNoteOns: LongArray) {
        val c = clip.clip
        if (c !is MidiClipImpl) return
        val blockEndSample = position.timeInSamples + position.bufferSize
        val startTime = clip.time
        if (clip.currentIndex < 0) {
            // use binary search to find the first note that is after the start of the block
            clip.currentIndex = c.notes.binarySearch { it.time - startTime }
        }
        for (i in clip.currentIndex until c.notes.size) {
            val note = c.notes[i]
            val startTimeInSamples = position.convertPPQToSamples(startTime + note.time)
            val endTimeInSamples = position.convertPPQToSamples(startTime + note.time + note.duration)
            if (startTimeInSamples < position.timeInSamples) continue
            if (startTimeInSamples > blockEndSample) break
            clip.currentIndex = i + 1
            val noteOnTime = (startTimeInSamples - position.timeInSamples).toInt().coerceAtLeast(0)
            if (noteRecorder.isMarked(note.note)) {
                noteRecorder.unmarkNote(note.note)
                midiBuffer.add(note.toNoteOffRawData())
                midiBuffer.add(noteOnTime)
            }
            midiBuffer.add(note.toNoteOnRawData())
            midiBuffer.add(noteOnTime)
            val endTime = endTimeInSamples - position.timeInSamples
            if (endTimeInSamples > blockEndSample) {
                pendingNoteOns[note.note] = endTime
                noteRecorder.markNote(note.note)
            } else {
                midiBuffer.add(note.toNoteOffRawData())
                midiBuffer.add((endTimeInSamples - position.timeInSamples).toInt().coerceAtLeast(0))
            }
        }
    }

    @Composable
    override fun playlistContent(clip: MidiClip, track: Track, trackHeight: Dp, noteWidth: MutableState<Dp>) {
        val height = (trackHeight / 128).coerceAtLeast(0.5.dp)
        clip.notes.read()
        clip.notes.forEach {
            Box(
                Modifier.size(noteWidth.value * it.duration, height)
                .absoluteOffset(noteWidth.value * it.time, trackHeight - trackHeight / 128 * it.note)
                .background(track.color))
        }
    }
}
