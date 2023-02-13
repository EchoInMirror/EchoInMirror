package com.eimsound.audioprocessor.data.midi

import androidx.compose.runtime.mutableStateOf
import com.eimsound.audioprocessor.data.EnvelopePoint
import com.eimsound.daw.utils.IManualState
import com.eimsound.daw.utils.mapValue
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer

interface NoteMessage {
    var note: Int
    var velocity: Int
    var time: Int
    var duration: Int
    @get:JsonInclude(value = JsonInclude.Include.NON_DEFAULT)
    var disabled: Boolean
    @get:JsonInclude(value = JsonInclude.Include.NON_NULL, content = JsonInclude.Include.NON_EMPTY)
    var extraData: MutableMap<String, Any>?
    fun copy(note: Int = this.note, velocity: Int = this.velocity, time: Int = this.time,
             duration: Int = this.duration, disabled: Boolean = this.disabled): NoteMessage
}

val NoteMessage.colorSaturation get() = 0.4F + 0.6F * if (disabled) 0F else mapValue(velocity, 0, 127)

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "eim:class")
data class SerializableNoteMessage(val ppq: Int, val notes: Collection<NoteMessage>)

fun defaultNoteMessage(note: Int, time: Int, duration: Int = 0, velocity: Int = 70, disabled: Boolean = false) =
    NoteMessageImpl(note, time, duration, velocity, disabled)

open class NoteMessageImpl(note: Int, override var time: Int, duration: Int = 0, override var velocity: Int = 70,
                           override var disabled: Boolean = false) : NoteMessage {
    override var note = note.coerceIn(0, 127)
        set(value) { field = value.coerceIn(0, 127) }
    override var duration = duration.coerceAtLeast(0)
        set(value) { field = value.coerceAtLeast(0) }
    override var extraData: MutableMap<String, Any>? = null

    override fun copy(note: Int, velocity: Int, time: Int, duration: Int, disabled: Boolean) =
        NoteMessageImpl(note, time, duration, velocity, disabled)

    override fun toString(): String {
        return "NoteMessageImpl(note=$note, time=$time, duration=$duration, velocity=$velocity, disabled=$disabled)"
    }
}

data class ParsedMidiMessages(val notes: List<NoteMessage>, val events: Map<Int, List<EnvelopePoint>>)

fun Collection<MidiEventWithTime>.parse(): ParsedMidiMessages {
    val noteMessages = ArrayList<NoteMessage>()
    val events = HashMap<Int, ArrayList<EnvelopePoint>>()
    val allNoteOns = arrayOfNulls<NoteMessage>(128)
    forEach { (event, time) ->
        if (event.isController) {
            val list = events.getOrPut(event.controller) { ArrayList() }
            list.add(EnvelopePoint(time, event.value / 127F))
            return@forEach
        }
        if (!event.isNote || event.note > 127) return@forEach
        val prevNoteOn = allNoteOns[event.note]
        if (prevNoteOn != null) {
            if (event.isNoteOff) prevNoteOn.duration = time - prevNoteOn.time
            noteMessages.add(prevNoteOn)
            allNoteOns[event.note] = null
        }
        if (event.isNoteOn) allNoteOns[event.note] = defaultNoteMessage(event.note, time, velocity = event.velocity)
    }
    allNoteOns.forEach { if (it != null) noteMessages.add(it) }
    noteMessages.sortBy { it.time }
    return ParsedMidiMessages(noteMessages, events)
}

@Suppress("unused")
fun NoteMessage.toNoteOnEvent(channel: Int = 0) = noteOn(channel, note, velocity)
@Suppress("unused")
fun NoteMessage.toNoteOffEvent(channel: Int = 0) = noteOff(channel, note)
fun NoteMessage.toNoteOnRawData(channel: Int = 0) = 0x90 or channel or (note shl 8) or (velocity shl 16)
fun NoteMessage.toNoteOffRawData(channel: Int = 0) = 0x80 or (70 shl 16) or channel or (note shl 8)

@JsonSerialize(using = NoteMessageListSerializer::class)
interface NoteMessageList : MutableList<NoteMessage>, IManualState {
    fun sort()
}

open class DefaultNoteMessageList : NoteMessageList, ArrayList<NoteMessage>() {
    private var modification = mutableStateOf(0)
    override fun sort() = sortWith { o1, o2 ->
        if (o1.time == o2.time)
            if (o1.duration == o2.duration) o1.note - o2.note else o1.duration - o2.duration
        else o1.time - o2.time
    }
    override fun update() { modification.value++ }
    override fun read() { modification.value }
}

class NoteMessageListSerializer @JvmOverloads constructor(t: Class<NoteMessageList>? = null) :
    StdSerializer<NoteMessageList>(t) {
    override fun serialize(value: NoteMessageList, jgen: JsonGenerator, provider: SerializerProvider?) {
        jgen.writeObject(value.toList())
    }
}

object NoteMessageListTypeReference : TypeReference<MutableList<NoteMessageImpl>>()
