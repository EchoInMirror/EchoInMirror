package cn.apisium.eim.data.midi

@Suppress("PropertyName", "DEPRECATION")
class MidiNoteRecorder {
    @Deprecated("Dont use this property!")
    var __notes1: ULong = 0u
    @Deprecated("Dont use this property!")
    var __notes2: ULong = 0u

    fun markNote(note: Int) {
        if (note >= 64) __notes2 = __notes2 or (1uL shl (note - 63))
        else __notes1 = __notes1 or (1uL shl note)
    }

    fun unmarkNote(note: Int) {
        if (note >= 64) __notes2 = __notes2 and (1uL shl (note - 63)).inv()
        else __notes1 = __notes1 and (1uL shl note).inv()
    }

    fun reset() {
        __notes1 = 0uL
        __notes2 = 0uL
    }

    @Suppress("DuplicatedCode")
    inline fun forEachNotes(block: (Int) -> Unit) {
        var i = 0
        var note = __notes1
        if (note == 0uL) i = 63
        else while (true) {
            val tmp = note.takeLowestOneBit()
            if (tmp == 0uL) break
            val zeros = tmp.countTrailingZeroBits() + 1
            i += zeros
            block(i)
            note = note shr zeros
        }
        note = __notes2
        while (true) {
            val tmp = note.takeLowestOneBit()
            if (tmp == 0uL) break
            val zeros = tmp.countTrailingZeroBits() + 1
            i += zeros
            block(i)
            note = note shr zeros
        }
    }
}
