package cn.apisium.eim.api

import cn.apisium.eim.data.midi.MidiEvent
import cn.apisium.eim.data.midi.allNotesOff

interface MidiEventHandler {
    fun playMidiEvent(midiEvent: MidiEvent, time: Int = 0)
    fun stopAllNotes() {
        playMidiEvent(allNotesOff(0))
    }
}
