package com.eimsound.daw.impl.clips.midi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.eimsound.audioprocessor.CurrentPosition
import com.eimsound.audioprocessor.convertPPQToSamples
import com.eimsound.audioprocessor.convertSamplesToPPQ
import com.eimsound.audioprocessor.data.DefaultEnvelopePointList
import com.eimsound.audioprocessor.data.EnvelopePoint
import com.eimsound.audioprocessor.data.MIDI_CC_RANGE
import com.eimsound.audioprocessor.data.midi.*
import com.eimsound.daw.api.*
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.components.EnvelopeEditor
import com.eimsound.daw.impl.clips.midi.editor.DefaultMidiClipEditor
import com.eimsound.daw.utils.binarySearch
import com.eimsound.daw.utils.putNotDefault
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.*

class MidiClipImpl(json: JsonObject?, factory: ClipFactory<MidiClip>) : AbstractClip<MidiClip>(json, factory), MidiClip {
    override val notes = DefaultNoteMessageList()
    override val events: MutableMidiCCEvents = mutableStateMapOf(
        1 to DefaultEnvelopePointList().apply {
            add(EnvelopePoint(96, 0.2F))
            add(EnvelopePoint(96 * 6, 0.8F))
        },
    )
    override val isExpandable = true

    val ccPrevValues = ByteArray(128)

    init {
        if (json != null) fromJson(json)
    }

    override fun toJson() = buildJsonObject {
        put("id", id)
        put("factory", factory.name)
        putNotDefault("notes", Json.encodeToJsonElement<List<NoteMessage>>(notes))
        putNotDefault("events", Json.encodeToJsonElement<MidiCCEvents>(events))
    }

    override fun fromJson(json: JsonElement) {
        super.fromJson(json)
        notes.clear()
        events.clear()
        json as JsonObject
        json["notes"]?.let {
            notes.addAll(Json.decodeFromJsonElement<List<NoteMessage>>(it))
            notes.update()
        }
        json["events"]?.let {
            val e2: MutableMidiCCEvents = hashMapOf()
            Json.decodeFromJsonElement<MidiCCEvents>(it).forEach { (id, points) ->
                e2[id] = DefaultEnvelopePointList().apply { addAll(points) }
            }
            events.putAll(e2)
        }
    }

    override fun toString(): String {
        return "MidiClipImpl(factory=$factory, notes=${notes.size}, id='$id')"
    }
}

private val logger = KotlinLogging.logger { }
class MidiClipFactoryImpl : MidiClipFactory {
    override val name = "MIDIClip"
    override fun createClip() = MidiClipImpl(null, this).apply {
        logger.info { "Creating clip \"${this.id}\"" }
    }
    override fun createClip(path: String, json: JsonObject): MidiClipImpl {
        logger.info { "Creating clip ${json["id"]} in $path" }
        return MidiClipImpl(json, this)
    }
    override fun getEditor(clip: TrackClip<MidiClip>, track: Track) = DefaultMidiClipEditor(clip, track)

    override fun processBlock(clip: TrackClip<MidiClip>, buffers: Array<FloatArray>, position: CurrentPosition,
                              midiBuffer: ArrayList<Int>, noteRecorder: MidiNoteRecorder, pendingNoteOns: LongArray) {
        val c = clip.clip as MidiClipImpl
        val timeInSamples = position.timeInSamples
        val blockEndSample = timeInSamples + position.bufferSize
        val startTime = clip.time
        val notes = c.notes
        if (clip.currentIndex == -1) {
            // use binary search to find the first note that is after the start of the block
            val startPPQ = position.timeInPPQ - startTime
            clip.currentIndex = notes.binarySearch { it.time <= startPPQ }
        }
        clip.clip.events.forEach { (id, points) ->
            if (id !in 0..127) return@forEach
            for (i in 0 until position.bufferSize step position.ppq) {
                val ppq = position.convertSamplesToPPQ(timeInSamples + i) - startTime
                val value = points.getValue(ppq).toInt()
                val byteValue = value.toByte()
                if (byteValue == c.ccPrevValues[id]) continue
                c.ccPrevValues[id] = byteValue
                midiBuffer.add(controllerEvent(0, id, value).rawData)
                midiBuffer.add(i)
            }
        }
        for (i in clip.currentIndex..notes.lastIndex) {
            val note = notes[i]
            val startTimeInSamples = position.convertPPQToSamples(startTime + note.time)
            if (startTimeInSamples > blockEndSample) break
            clip.currentIndex = i + 1
            if (startTimeInSamples < timeInSamples || note.disabled) continue
            val noteOnTime = (startTimeInSamples - timeInSamples).toInt().coerceAtLeast(0)
            if (noteRecorder.isMarked(note.note)) {
                noteRecorder.unmarkNote(note.note)
                midiBuffer.add(note.toNoteOffRawData())
                midiBuffer.add(noteOnTime)
            }
            midiBuffer.add(note.toNoteOnRawData())
            midiBuffer.add(noteOnTime)
            val endTimeInSamples = position.convertPPQToSamples(startTime + note.time + note.duration)
            val endTime = endTimeInSamples - timeInSamples
            if (endTimeInSamples > blockEndSample) {
                pendingNoteOns[note.note] = endTime
                noteRecorder.markNote(note.note)
            } else {
                midiBuffer.add(note.toNoteOffRawData())
                midiBuffer.add((endTimeInSamples - timeInSamples).toInt().coerceAtLeast(0))
            }
        }
    }

    @Composable
    override fun PlaylistContent(clip: TrackClip<MidiClip>, track: Track, contentColor: Color,
                                 noteWidth: MutableState<Dp>, startPPQ: Float, widthPPQ: Float) {
        Box {
            clip.clip.notes.read()
            Canvas(Modifier.fillMaxSize().graphicsLayer { }) {
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
            clip.clip.events.forEach { (_, points) ->
                points.read()
                remember(points) {
                    EnvelopeEditor(points, MIDI_CC_RANGE)
                }.Editor(startPPQ, contentColor, noteWidth, false, clipStartTime = clip.start, stroke = 0.5F)
            }
        }
    }

    override fun toString(): String {
        return "MidiClipFactoryImpl(name='$name')"
    }
}
