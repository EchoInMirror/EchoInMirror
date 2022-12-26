package cn.apisium.eim.data.midi

import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.Serializable
import kotlin.collections.ArrayList

interface NoteMessage {
    var note: Int
    var velocity: Int
    var time: Int
    var duration: Int
}

fun defaultNoteMessage(note: Int, time: Int, duration: Int = 0, velocity: Int = 70) = NoteMessageImpl(note, time, duration, velocity)

@Serializable
open class NoteMessageImpl(override var note: Int, override var time: Int, override var duration: Int = 0,
                           override var velocity: Int = 70) : NoteMessage {
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

interface NoteMessageList : MutableList<NoteMessage> {
    fun sort()
    fun update()
    fun read()
}

@Suppress("unused")
inline fun <R> NoteMessageList.readWith(block: NoteMessageList.() -> R): R {
    read()
    return this.block()
}

@Suppress("unused")
inline fun NoteMessageList.updateWith(block: NoteMessageList.() -> Unit) {
    this.block()
    update()
}

class NoteMessageListImpl : NoteMessageList, ArrayList<NoteMessage>() {
    private var modification = mutableStateOf(0)
    override fun sort() = sortWith { o1, o2 -> o1.time - o2.time }
    override fun update() { modification.value++ }
    override fun read() { modification.value }
}
