package com.eimsound.daw.impl.clips.midi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastForEach
import com.eimsound.audioprocessor.CurrentPosition
import com.eimsound.audioprocessor.convertPPQToSamples
import com.eimsound.audioprocessor.convertSamplesToPPQ
import com.eimsound.audioprocessor.data.DefaultEnvelopePointList
import com.eimsound.audioprocessor.data.EnvelopePoint
import com.eimsound.audioprocessor.data.midi.*
import com.eimsound.daw.api.*
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.components.EnvelopeEditor
import com.eimsound.daw.impl.clips.midi.editor.DefaultMidiClipEditor
import com.eimsound.daw.utils.binarySearch
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

data class MidiCCEventImpl(override val id: Int, override val points: DefaultEnvelopePointList) : MidiCCEvent
object MidiCCEventImplTypeReference : TypeReference<MutableList<MidiCCEventImpl>>()

class MidiClipImpl(json: JsonNode?, factory: ClipFactory<MidiClip>) : AbstractClip<MidiClip>(json, factory), MidiClip {
    override val notes = DefaultNoteMessageList()
    override val events = mutableListOf<MidiCCEvent>(MidiCCEventImpl(1, DefaultEnvelopePointList().apply {
        add(EnvelopePoint(0, 0F, 0.2F))
        add(EnvelopePoint(600, 80F))
    }))
    override val isExpandable = true
    val ccPrevValues = ByteArray(128)

    init {
        if (json != null) {
            notes.addAll(json["notes"].traverse(jacksonObjectMapper()).readValueAs(NoteMessageListTypeReference))
            events.addAll(json["events"].traverse(jacksonObjectMapper()).readValueAs(MidiCCEventImplTypeReference))
            notes.update()
        }
    }

    override fun toString(): String {
        return "MidiClipImpl(factory=$factory, notes=${notes.size}, id='$id')"
    }
}

class MidiClipFactoryImpl : ClipFactory<MidiClip> {
    override val name = "MIDIClip"
    override fun createClip() = MidiClipImpl(null, this)
    override fun createClip(path: String, json: JsonNode) = MidiClipImpl(json, this)
    override fun getEditor(clip: TrackClip<MidiClip>, track: Track) = DefaultMidiClipEditor(clip, track)

    override fun processBlock(clip: TrackClip<MidiClip>, buffers: Array<FloatArray>, position: CurrentPosition,
                              midiBuffer: ArrayList<Int>, noteRecorder: MidiNoteRecorder, pendingNoteOns: LongArray) {
        val c = clip.clip as MidiClipImpl
        val blockEndSample = position.timeInSamples + position.bufferSize
        val startTime = clip.time
        val notes = c.notes
        if (clip.currentIndex == -1) {
            // use binary search to find the first note that is after the start of the block
            val startPPQ = position.timeInPPQ - startTime
            clip.currentIndex = notes.binarySearch { it.time <= startPPQ }
        }
        clip.clip.events.fastForEach {
            if (it.id !in 0..127) return@fastForEach
            for (i in 0 until position.bufferSize step position.ppq) {
                val ppq = position.convertSamplesToPPQ(position.timeInSamples + i) - startTime
                val value = it.points.getValue(ppq).toInt()
                val byteValue = value.toByte()
                if (byteValue == c.ccPrevValues[it.id]) continue
                c.ccPrevValues[it.id] = byteValue
                midiBuffer.add(controllerEvent(0, it.id, value).rawData)
                midiBuffer.add(i)
            }
        }
        for (i in clip.currentIndex..notes.lastIndex) {
            val note = notes[i]
            val startTimeInSamples = position.convertPPQToSamples(startTime + note.time)
            if (startTimeInSamples > blockEndSample) break
            clip.currentIndex = i + 1
            if (startTimeInSamples < position.timeInSamples || note.disabled) continue
            val noteOnTime = (startTimeInSamples - position.timeInSamples).toInt().coerceAtLeast(0)
            if (noteRecorder.isMarked(note.note)) {
                noteRecorder.unmarkNote(note.note)
                midiBuffer.add(note.toNoteOffRawData())
                midiBuffer.add(noteOnTime)
            }
            midiBuffer.add(note.toNoteOnRawData())
            midiBuffer.add(noteOnTime)
            val endTimeInSamples = position.convertPPQToSamples(startTime + note.time + note.duration)
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
    override fun playlistContent(clip: TrackClip<MidiClip>, track: Track, contentColor: Color,
                                 noteWidth: MutableState<Dp>, startPPQ: Float, widthPPQ: Float) {
        Box {
            clip.clip.notes.read()
            Canvas(Modifier.fillMaxSize()) {
                val noteWidthPx = noteWidth.value.toPx()
                val trackHeightPx = size.height
                val height = (trackHeightPx / 128).coerceAtLeast(1F)
                val startId = clip.clip.notes.binarySearch { it.time <= startPPQ }
                val endTime = startPPQ + widthPPQ
                for (i in startId..clip.clip.notes.lastIndex) {
                    val note = clip.clip.notes[i]
                    if (note.time > endTime) break
                    val y = trackHeightPx - trackHeightPx / 128 * note.note
                    drawLine(
                        contentColor, Offset(noteWidthPx * (note.time - startPPQ), y),
                        Offset(noteWidthPx * (note.time + note.duration - startPPQ), y),
                        height
                    )
                }
            }
            clip.clip.events.forEach {
                it.points.read()
                remember(it.points) {
                    EnvelopeEditor(it.points, 0..127)
                }.Editor(startPPQ, contentColor, noteWidth, false, clipStartTime = clip.start, style = Stroke(0.5F))
            }
        }
    }

    override fun toString(): String {
        return "MidiClipFactoryImpl(name='$name')"
    }
}
