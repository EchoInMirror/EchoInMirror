package cn.apisium.eim.data.midi

import kotlinx.serialization.Serializable

interface NoteMessage {
    var note: MidiEvent
    var time: Int
    var duration: Int
}

@Serializable
open class NoteMessageImpl(override var note: MidiEvent, override var time: Int, override var duration: Int = 0) : NoteMessage {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NoteMessageImpl) return false

        if (note != other.note) return false
        if (time != other.time) return false
        if (duration != other.duration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = note.hashCode()
        result = 31 * result + time
        result = 31 * result + duration
        return result
    }

    override fun toString(): String {
        return "NoteMessageImpl(note=$note, time=$time, duration=$duration)"
    }
}

fun getNoteMessages(list: ArrayList<Int>): List<NoteMessage> {
    val noteMessages = ArrayList<NoteMessage>()
    val allNoteOns = arrayOfNulls<NoteMessage>(128)
    for (i in 0 until list.size step 2) {
        val note = list[i].toMidiEvent()
        val time = list[i + 1]
        if (!note.isNote || note.note > 127) continue
        val prevNoteOn = allNoteOns[note.note]
        if (prevNoteOn != null) {
            if (note.isNoteOff) prevNoteOn.duration = time - prevNoteOn.time
            noteMessages.add(prevNoteOn)
            allNoteOns[note.note] = null
        }
        if (note.isNoteOn) allNoteOns[note.note] = NoteMessageImpl(note, time)
    }
    allNoteOns.forEach { if (it != null) noteMessages.add(it) }
    noteMessages.sortBy { it.time }
    return noteMessages
}
