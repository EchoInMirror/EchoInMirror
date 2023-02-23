package com.eimsound.audioprocessor.data.midi

import androidx.compose.runtime.mutableStateOf
import com.eimsound.audioprocessor.data.EnvelopePoint
import com.eimsound.daw.utils.IManualState
import com.eimsound.daw.utils.mapValue
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonArray

/**
 * Consider make this class open.
 * @see [DefaultNoteMessage]
 */
@Serializable
sealed interface NoteMessage {
    var note: Int
    var velocity: Int
    var time: Int
    var duration: Int
    var disabled: Boolean
    var extraData: MutableMap<String, Any>?
    fun copy(note: Int = this.note, velocity: Int = this.velocity, time: Int = this.time,
             duration: Int = this.duration, disabled: Boolean = this.disabled): NoteMessage
}

val NoteMessage.colorSaturation get() = 0.4F + 0.6F * if (disabled) 0F else mapValue(velocity, 0, 127)

@Serializable
data class SerializableNoteMessages(val ppq: Int, val notes: Collection<NoteMessage>) {
    @OptIn(ExperimentalSerializationApi::class)
    @Suppress("unused")
    @EncodeDefault
    val className = "NoteMessages"
}

fun defaultNoteMessage(note: Int, time: Int, duration: Int = 0, velocity: Int = 70, disabled: Boolean = false) =
    DefaultNoteMessage(note, time, duration, velocity, disabled)

open class DefaultNoteMessage(note: Int, override var time: Int, duration: Int = 0, override var velocity: Int = 70,
                           override var disabled: Boolean = false) : NoteMessage {
    override var note = note.coerceIn(0, 127)
        set(value) { field = value.coerceIn(0, 127) }
    override var duration = duration.coerceAtLeast(0)
        set(value) { field = value.coerceAtLeast(0) }
    override var extraData: MutableMap<String, Any>? = null

    override fun copy(note: Int, velocity: Int, time: Int, duration: Int, disabled: Boolean) =
        DefaultNoteMessage(note, time, duration, velocity, disabled)

    override fun toString(): String {
        return "DefaultNoteMessage(note=$note, time=$time, duration=$duration, velocity=$velocity, disabled=$disabled)"
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

@Serializable(NoteMessageListSerializer::class)
interface NoteMessageList : MutableList<NoteMessage>, IManualState {
    fun sort()
}

@Serializable
open class DefaultNoteMessageList : NoteMessageList, ArrayList<NoteMessage>() {
    @Transient
    private var modification = mutableStateOf(0)
    override fun sort() = sortWith { o1, o2 ->
        if (o1.time == o2.time)
            if (o1.duration == o2.duration) o1.note - o2.note else o1.duration - o2.duration
        else o1.time - o2.time
    }
    override fun update() { modification.value++ }
    override fun read() { modification.value }
}

object NoteMessageListSerializer : KSerializer<NoteMessageList> {
    private val serializer = NoteMessage.serializer()
    private val listSerializer = ListSerializer(serializer)
    override val descriptor: SerialDescriptor = listSerializer.descriptor

    override fun serialize(encoder: Encoder, value: NoteMessageList) {
        listSerializer.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder) = DefaultNoteMessageList().apply {
        val v = decoder.decodeSerializableValue(listSerializer)
        addAll(if (decoder is JsonDecoder) {
            decoder.decodeJsonElement().jsonArray.map { decoder.json.decodeFromJsonElement(serializer, it) }
        } else v)
    }
}
