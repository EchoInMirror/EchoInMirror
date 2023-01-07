package cn.apisium.eim.impl.clips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.apisium.eim.api.*
import cn.apisium.eim.data.midi.*
import cn.apisium.eim.utils.randomId
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File

class MidiClipImpl(json: JsonNode?, override val factory: ClipFactory<MidiClip>) : MidiClip {
    override val notes = DefaultNoteMessageList()
    override val id = json?.get("id")?.asText() ?: randomId()
    override var duration by mutableStateOf(json?.get("duration")?.asInt() ?: 0)

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
    override fun save(clip: Clip, path: String) {
        if (clip !is MidiClipImpl) return
        jacksonObjectMapper().writeValue(File(path, "$clip.json"), clip)
    }

    override fun processBlock(clip: TrackClip<*>, buffers: Array<FloatArray>, position: CurrentPosition
                              , midiBuffer: ArrayList<Int>, noteRecorder: MidiNoteRecorder, pendingNoteOns: LongArray) {
        val c = clip.clip
        if (c !is MidiClipImpl) return
        val blockEndSample = position.timeInSamples + position.bufferSize
        val startTime = clip.time
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
}
