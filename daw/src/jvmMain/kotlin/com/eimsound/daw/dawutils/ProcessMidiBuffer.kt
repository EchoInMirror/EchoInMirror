package com.eimsound.daw.dawutils

import com.eimsound.audioprocessor.PlayPosition
import com.eimsound.audioprocessor.convertPPQToSamples
import com.eimsound.daw.utils.lowerBound
import com.eimsound.dsp.data.midi.*

fun processMIDIBuffer(
    notes: List<NoteMessage>, position: PlayPosition, midiBuffer: ArrayList<Int>, startTime: Int,
    timeInSamples: Long, noteRecorder: MidiNoteTimeRecorder, currentIndex: Int,
): Int {
    var index = currentIndex
    if (index == -1) {
        // use binary search to find the first note that is after the start of the block
        val startPPQ = position.timeInPPQ - startTime
        index = notes.lowerBound { it.time <= startPPQ }
    }
    val blockEndSample = timeInSamples + position.bufferSize
    if (index > 0) index--
    for (i in index..notes.lastIndex) {
        val note = notes[i]
        val startTimeInSamples = position.convertPPQToSamples(startTime + note.time)
        if (startTimeInSamples > blockEndSample) break
        index = i + 1
        if (startTimeInSamples < timeInSamples || note.isDisabled) continue
        val noteOnTime = (startTimeInSamples - timeInSamples).toInt().coerceAtLeast(0)
        if (noteRecorder.noteRecorder.isMarked(note.note)) {
            noteRecorder.noteRecorder.unmarkNote(note.note)
            midiBuffer.add(note.toNoteOffRawData())
            midiBuffer.add(noteOnTime)
        }
        midiBuffer.add(note.toNoteOnRawData())
        midiBuffer.add(noteOnTime)
        val endTimeInSamples = position.convertPPQToSamples(startTime + note.time + note.duration)
        val endTime = (endTimeInSamples - timeInSamples).toInt().coerceAtLeast(0)
        if (endTimeInSamples > blockEndSample) {
            noteRecorder.markNoteOn(note.note, endTime)
        } else {
            midiBuffer.add(note.toNoteOffRawData())
            midiBuffer.add(endTime)
        }
    }

    return index
}