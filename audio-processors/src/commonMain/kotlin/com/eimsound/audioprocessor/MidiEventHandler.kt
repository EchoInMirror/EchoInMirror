package com.eimsound.audioprocessor

import com.eimsound.dsp.data.midi.MidiEvent
import com.eimsound.dsp.data.midi.allNotesOff

interface MidiEventHandler {
    fun playMidiEvent(midiEvent: MidiEvent, time: Int = 0)
    fun stopAllNotes() {
        playMidiEvent(allNotesOff(0))
    }
}
