package cn.apisium.eim.processor.synthesizer

import cn.apisium.eim.api.processor.AudioProcessor
import cn.apisium.eim.data.midi.MidiEvent

abstract class Synthesizer : AudioProcessor {
    protected var notes1: Long = 0
    protected var notes2: Long = 0
    protected var notesData = IntArray(127)

    protected fun markNoteEvent(midi: MidiEvent) {
        if (midi.isNoteOn) {
            if (midi.note >= 64) notes2 = notes2 or (1L shl (midi.note - 63))
            else notes1 = notes1 or (1L shl midi.note)
            notesData[midi.note] = midi.rawData
        } else if (midi.isNoteOff) {
            if (midi.note >= 64) notes2 = notes2 and (1L shl (midi.note - 63)).inv()
            else notes1 = notes1 and (1L shl midi.note).inv()
            notesData[midi.note] = 0
        }
    }

    @Suppress("DuplicatedCode")
    protected inline fun forEachNoteOn(block: (MidiEvent) -> Unit) {
        var i = 0
        var note = notes1
        if (note == 0L) i = 63
        else while (true) {
            val tmp = java.lang.Long.lowestOneBit(note)
            if (tmp == 0L) break
            i += java.lang.Long.numberOfTrailingZeros(tmp)
            block(MidiEvent(notesData[i]))
            note = note xor tmp
        }
        note = notes2
        while (true) {
            val tmp = java.lang.Long.lowestOneBit(note)
            if (tmp == 0L) break
            i += java.lang.Long.numberOfTrailingZeros(tmp)
            block(MidiEvent(notesData[i]))
            note = note xor tmp
        }
    }
}