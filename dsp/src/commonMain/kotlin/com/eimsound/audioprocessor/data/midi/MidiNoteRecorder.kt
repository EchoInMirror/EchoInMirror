package com.eimsound.audioprocessor.data.midi

@Suppress("PropertyName", "DEPRECATION")
class MidiNoteRecorder : Iterable<Int> {
    @Deprecated("Dont use this property!")
    var __notes1: ULong = 0u
    @Deprecated("Dont use this property!")
    var __notes2: ULong = 0u

    fun markNote(note: Int) {
        if (note >= 64) __notes2 = __notes2 or (1uL shl (note - 64))
        else __notes1 = __notes1 or (1uL shl note)
    }

    fun unmarkNote(note: Int) {
        if (note >= 64) __notes2 = __notes2 and (1uL shl (note - 64)).inv()
        else __notes1 = __notes1 and (1uL shl note).inv()
    }

    fun reset() {
        __notes1 = 0uL
        __notes2 = 0uL
    }

    fun isMarked(note: Int) = if (note >= 64) __notes2 and (1uL shl (note - 64)) != 0uL else __notes1 and (1uL shl note) != 0uL

    @Suppress("DuplicatedCode")
    inline fun forEachNotes(block: (Int) -> Unit) {
        var i = -1
        var note = __notes1
        while (note != 0uL) {
            val zeros = note.countTrailingZeroBits() + 1
            i += zeros
            note = note shr zeros
            block(i)
        }
        i = 63
        note = __notes2
        while (note != 0uL) {
            val zeros = note.countTrailingZeroBits() + 1
            i += zeros
            note = note shr zeros
            block(i)
        }
    }

    override fun iterator() = MidiNoteIterator(__notes1, __notes2)
}

class MidiNoteIterator(private var notes1: ULong, private var notes2: ULong) : Iterator<Int> {
    private var i = -1
    override fun hasNext() = notes1 != 0uL || notes2 != 0uL
    override fun next(): Int {
        if (notes1 != 0uL) {
            val zeros = notes1.countTrailingZeroBits() + 1
            i += zeros
            notes1 = notes1 shr zeros
            if (notes1 == 0uL) i = 63
            return i
        }
        val zeros = notes2.countTrailingZeroBits() + 1
        i += zeros
        notes2 = notes2 shr zeros
        return i
    }
}
