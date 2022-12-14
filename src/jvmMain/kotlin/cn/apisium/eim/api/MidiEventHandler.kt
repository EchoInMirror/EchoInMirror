package cn.apisium.eim.api

import cn.apisium.eim.data.midi.MidiEvent
import cn.apisium.eim.data.midi.noteOff

interface MidiEventHandler {
    fun playMidiEvent(midiEvent: MidiEvent, time: Int = 0)
    fun stopAllNotes() {
        for (i in 0..127) playMidiEvent(noteOff(0, i))
    }
}
