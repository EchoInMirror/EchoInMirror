package com.eimsound.audioprocessor.data.midi

import androidx.compose.runtime.mutableStateOf
import com.eimsound.daw.commons.Disabled
import com.eimsound.daw.utils.IManualState
import com.eimsound.daw.utils.mapValue
import com.eimsound.daw.commons.json.*
import kotlinx.serialization.json.*

/**
 * Consider make this class open.
 * @see [DefaultNoteMessage]
 */
interface NoteMessage : JsonSerializable, Disabled {
    var note: Int
    var velocity: Int
    var time: Int
    var duration: Int
    var extraData: MutableMap<String, Any>?
    fun copy(note: Int = this.note, velocity: Int = this.velocity, time: Int = this.time,
             duration: Int = this.duration, disabled: Boolean = this.isDisabled): NoteMessage
}

val NoteMessage.colorSaturation get() = 0.4F + 0.6F * if (isDisabled) 0F else mapValue(velocity, 0, 127)

class SerializableNoteMessages(var ppq: Int = 96, notes: List<NoteMessage>? = null) : JsonSerializable {
    private val notesP = arrayListOf<NoteMessage>().apply { if (notes != null) addAll(notes) }
    val notes: List<NoteMessage> = notesP

    override fun toJson() = buildJsonObject {
        put("ppq", ppq)
        putNotDefault("notes", notes)
    }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        ppq = json["ppq"]!!.jsonPrimitive.int
        notesP.clear()
        json["notes"]?.jsonArray?.forEach { notesP.add(DefaultNoteMessage().apply { fromJson(it) }) }
    }
}

fun defaultNoteMessage(note: Int, time: Int, duration: Int = 0, velocity: Int = 70, disabled: Boolean = false) =
    DefaultNoteMessage(note, time, duration, velocity, disabled)

open class DefaultNoteMessage(
    initNote: Int = 0, override var time: Int = 0,
    initDuration: Int = 0, override var velocity: Int = 70,
    override var isDisabled: Boolean = false,
): NoteMessage {
    override var note = initNote.coerceIn(0, 127)
        set(value) { field = value.coerceIn(0, 127) }
    override var duration = initDuration.coerceAtLeast(0)
        set(value) { field = value.coerceAtLeast(0) }
    override var extraData: MutableMap<String, Any>? = null

    override fun copy(note: Int, velocity: Int, time: Int, duration: Int, disabled: Boolean) =
        DefaultNoteMessage(note, time, duration, velocity, disabled)

    override fun toJson() = buildJsonObject {
        put("note", note)
        put("time", time)
        put("duration", duration)
        put("velocity", velocity)
        putNotDefault("isDisabled", isDisabled)
        if (extraData != null) putNotDefault("extraData", Json.encodeToJsonElement(extraData!!))
    }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        note = json["note"]!!.jsonPrimitive.int
        time = json["time"]!!.jsonPrimitive.int
        duration = json["duration"]!!.jsonPrimitive.int
        velocity = json["velocity"]!!.jsonPrimitive.int
        isDisabled = json["isDisabled"]?.jsonPrimitive?.boolean ?: false
        extraData = json["extraData"]?.jsonObject?.let { Json.decodeFromJsonElement(it) }
    }

    override fun toString(): String {
        return "DefaultNoteMessage(note=$note, time=$time, duration=$duration, velocity=$velocity, isDisabled=$isDisabled)"
    }
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
