package cn.apisium.eim.api

import androidx.compose.runtime.mutableStateOf
import cn.apisium.eim.data.midi.MidiNoteRecorder
import cn.apisium.eim.data.midi.NoteMessageList
import cn.apisium.eim.utils.IManualState
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import kotlin.jvm.Throws

class NoSuchFactoryException(name: String): Exception("No such factory: $name")

interface ClipFactory<T: Clip> {
    val name: String
    fun createClip(): T
    fun createClip(path: String, json: JsonNode): T
    fun processBlock(clip: TrackClip<*>, buffers: Array<FloatArray>, position: CurrentPosition,
                     midiBuffer: ArrayList<Int>, noteRecorder: MidiNoteRecorder, pendingNoteOns: LongArray)
    fun save(clip: Clip, path: String)
}

interface ClipManager {
    val factories: Map<String, ClipFactory<*>>
    fun registerClipFactory(factory: ClipFactory<*>)
    @Throws(NoSuchFactoryException::class)
    suspend fun createClip(factory: String): Clip
    @Throws(NoSuchFactoryException::class)
    suspend fun createClip(path: String, json: JsonNode): Clip
    @Throws(NoSuchFactoryException::class)
    suspend fun createClip(path: String, id: String): Clip
    fun <T: Clip> createTrackClip(clip: T): TrackClip<T>
}

@Suppress("UNCHECKED_CAST")
val ClipManager.defaultMidiClipFactory get() = factories["MIDIClip"] as ClipFactory<MidiClip>

interface Clip {
    val id: String
    var duration: Int
    @get:JsonSerialize(using = ClipFactoryNameSerializer::class)
    val factory: ClipFactory<*>
}

interface MidiClip : Clip {
    val notes: NoteMessageList
}
interface AudioClip : Clip

interface TrackClip<T: Clip> {
    var time: Int
    @get:JsonSerialize(using = ClipIdSerializer::class)
    val clip: T
    @get:JsonIgnore
    var currentIndex: Int
}
interface TrackClipList : MutableList<TrackClip<Clip>>, IManualState {
    fun sort()
}

class DefaultTrackClipList : TrackClipList, ArrayList<TrackClip<Clip>>() {
    private var modification = mutableStateOf(0)
    override fun sort() = sortWith { o1, o2 ->
        if (o1.time == o2.time) o1.clip.duration - o2.clip.duration
        else o1.time - o2.time
    }
    override fun update() { modification.value++ }
    override fun read() { modification.value }
}

class ClipIdSerializer @JvmOverloads constructor(t: Class<Clip>? = null) :
    StdSerializer<Clip>(t) {
    override fun serialize(value: Clip, jgen: JsonGenerator, provider: SerializerProvider?) {
        jgen.writeString(value.id)
    }
}

class ClipFactoryNameSerializer @JvmOverloads constructor(t: Class<ClipFactory<*>>? = null) :
    StdSerializer<ClipFactory<*>>(t) {
    override fun serialize(value: ClipFactory<*>, jgen: JsonGenerator, provider: SerializerProvider?) {
        jgen.writeString(value.name)
    }
}
