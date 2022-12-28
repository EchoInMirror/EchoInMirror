package cn.apisium.eim.data.midi

import androidx.compose.runtime.mutableStateOf
import cn.apisium.eim.utils.IManualState
import kotlin.collections.ArrayList

interface NoteMessage {
    var note: Int
    var velocity: Int
    var time: Int
    var duration: Int
    var disabled: Boolean
    fun copy(note: Int = this.note, velocity: Int = this.velocity, time: Int = this.time,
             duration: Int = this.duration, disabled: Boolean = this.disabled): NoteMessage
}

data class NoteMessageWithInfo(val ppq: Int, val notes: Collection<NoteMessage>)

fun defaultNoteMessage(note: Int, time: Int, duration: Int = 0, velocity: Int = 70, disabled: Boolean = false) =
    NoteMessageImpl(note, time, duration, velocity, disabled)

open class NoteMessageImpl(note: Int, time: Int, duration: Int = 0, override var velocity: Int = 70,
                           override var disabled: Boolean = false) : NoteMessage {
    override var note = note.coerceIn(0, 127)
        set(value) { field = value.coerceIn(0, 127) }
    override var time = time.coerceAtLeast(0)
        set(value) { field = value.coerceAtLeast(0) }
    override var duration = duration.coerceAtLeast(0)
        set(value) { field = value.coerceAtLeast(0) }
    override fun copy(note: Int, velocity: Int, time: Int, duration: Int, disabled: Boolean) =
        NoteMessageImpl(note, time, duration, velocity, disabled)

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
        if (note.isNoteOn) allNoteOns[note.note] = defaultNoteMessage(note.note, time, velocity = note.velocity)
    }
    allNoteOns.forEach { if (it != null) noteMessages.add(it) }
    noteMessages.sortBy { it.time }
    return noteMessages
}

@Suppress("unused")
fun NoteMessage.toNoteOnEvent(channel: Int = 0) = noteOn(channel, note, velocity)
@Suppress("unused")
fun NoteMessage.toNoteOffEvent(channel: Int = 0) = noteOff(channel, note)
fun NoteMessage.toNoteOnRawData(channel: Int = 0) = 0x90 or channel or (note shl 8) or (velocity shl 16)
fun NoteMessage.toNoteOffRawData(channel: Int = 0) = 0x80 or (70 shl 16) or channel or (note shl 8)

interface NoteMessageList : MutableList<NoteMessage>, IManualState {
    fun sort()
}

class NoteMessageListImpl : NoteMessageList, ArrayList<NoteMessage>() {
    private var modification = mutableStateOf(0)
    override fun sort() = sortWith { o1, o2 ->
        if (o1.time == o2.time)
            if (o1.duration == o2.duration) o1.note - o2.note else o1.duration - o2.duration
        else o1.time - o2.time
    }
    override fun update() { modification.value++ }
    override fun read() { modification.value }
}
