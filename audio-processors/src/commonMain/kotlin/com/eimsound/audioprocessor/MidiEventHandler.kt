package com.eimsound.audioprocessor

import com.eimsound.audioprocessor.data.midi.MidiEvent
import com.eimsound.audioprocessor.data.midi.allNotesOff

interface MidiEventHandler {
    fun playMidiEvent(midiEvent: MidiEvent, time: Int = 0)
    fun stopAllNotes() {
        playMidiEvent(allNotesOff(0))
    }
}
