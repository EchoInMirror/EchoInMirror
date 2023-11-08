package com.eimsound.daw.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.eimsound.audioprocessor.AudioSource
import com.eimsound.audioprocessor.CurrentPosition
import com.eimsound.audioprocessor.ResampledAudioSource
import com.eimsound.audioprocessor.data.AudioThumbnail
import com.eimsound.audioprocessor.data.BaseEnvelopePointList
import com.eimsound.audioprocessor.data.EnvelopePointList
import com.eimsound.audioprocessor.data.midi.MidiNoteRecorder
import com.eimsound.audioprocessor.data.midi.NoteMessageList
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.utils.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.nio.file.Path
import java.util.*

interface ClipEditor : MultiSelectableEditor {
    @Composable
    fun Editor()
}

interface MidiClipEditor: ClipEditor, SerializableEditor, MultiSelectableEditor {
    val clip: TrackClip<MidiClip>
}

@Serializable(with = ClipFactoryNameSerializer::class)
interface ClipFactory<T: Clip> {
    val name: String
    fun createClip(): T
    fun createClip(path: Path, json: JsonObject): T
    fun processBlock(clip: TrackClip<T>, buffers: Array<FloatArray>, position: CurrentPosition,
                     midiBuffer: ArrayList<Int>, noteRecorder: MidiNoteRecorder, pendingNoteOns: LongArray)
    fun save(clip: T, path: Path) { }
    fun getEditor(clip: TrackClip<T>): ClipEditor?
    @Composable
    fun PlaylistContent(clip: TrackClip<T>, track: Track, contentColor: Color,
                        noteWidth: MutableState<Dp>, startPPQ: Float, widthPPQ: Float
    )
}

/**
 * @see com.eimsound.daw.impl.clips.midi.MidiClipFactoryImpl
 */
interface MidiClipFactory: ClipFactory<MidiClip>

/**
 * @see com.eimsound.daw.impl.clips.audio.AudioClipFactoryImpl
 */
interface AudioClipFactory: ClipFactory<AudioClip> {
    fun createClip(path: Path): AudioClip
}

/**
 * @see com.eimsound.daw.impl.clips.ClipManagerImpl
 */
interface ClipManager : Reloadable {
    companion object {
        val instance by lazy { ServiceLoader.load(ClipManager::class.java).first()!! }
    }

    val factories: Map<String, ClipFactory<*>>

    @Throws(NoSuchFactoryException::class)
    suspend fun createClip(factory: String): Clip
    @Throws(NoSuchFactoryException::class)
    suspend fun createClip(path: Path, json: JsonObject): Clip
    fun <T: Clip> createTrackClip(clip: T, time: Int = 0, duration: Int = clip.defaultDuration.coerceAtLeast(0),
                                  start: Int = 0, track: Track? = null): TrackClip<T>
    suspend fun createTrackClip(path: Path, json: JsonObject): TrackClip<Clip>
}

val ClipManager.defaultMidiClipFactory get() = factories["MIDIClip"] as MidiClipFactory
val ClipManager.defaultAudioClipFactory get() = factories["AudioClip"] as AudioClipFactory

interface Clip : JsonSerializable {
    val id: String
    val name: String
    val factory: ClipFactory<*>
    @Transient
    val defaultDuration: Int
    @Transient
    val isExpandable: Boolean
    @Transient
    val maxDuration: Int
}

typealias MidiCCEvents = Map<Int, BaseEnvelopePointList>
typealias MutableMidiCCEvents = MutableMap<Int, EnvelopePointList>

interface MidiClip : Clip {
    val notes: NoteMessageList
    val events: MutableMidiCCEvents
}
interface AudioClip : Clip, AutoCloseable {
    var target: AudioSource
    @Transient
    val audioSource: ResampledAudioSource
    @Transient
    val thumbnail: AudioThumbnail
    val volumeEnvelope: EnvelopePointList
}

abstract class AbstractClip<T: Clip>(json: JsonObject?, override val factory: ClipFactory<T>) : Clip {
    override var id = json?.get("id")?.asString() ?: randomId()
    override val name: String = ""
    override val isExpandable = false
    override val defaultDuration = -1
    override val maxDuration = -1

    override fun toString(): String {
        return "MidiClipImpl(factory=$factory, id='$id')"
    }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        id = json["id"]!!.asString()
    }
}

/**
 * @see com.eimsound.daw.impl.clips.TrackClipImpl
 */
interface TrackClip<T: Clip> : JsonSerializable {
    var time: Int
    var duration: Int
    var start: Int
    val clip: T
    @Transient
    var currentIndex: Int
    @Transient
    var track: Track?
    fun reset()
    fun copy(time: Int = this.time, duration: Int = this.duration, start: Int = this.start,
             clip: T = this.clip, currentIndex: Int = this.currentIndex, track: Track? = this.track
    ): TrackClip<T>
}

@Suppress("UNCHECKED_CAST", "unused")
fun TrackClip<*>.asMidiTrackClip() = this as TrackClip<MidiClip>
@Suppress("UNCHECKED_CAST")
fun TrackClip<*>.asMidiTrackClipOrNull() = if (clip is MidiClip) this as TrackClip<MidiClip> else null

interface TrackClipList : MutableList<TrackClip<*>>, IManualState {
    fun sort()
}

class DefaultTrackClipList(private val track: Track) : TrackClipList, ArrayList<TrackClip<*>>() {
    @Transient private var modification = mutableStateOf(0)
    override fun sort() = sortWith { o1, o2 ->
        if (o1.time == o2.time) o1.duration - o2.duration
        else o1.time - o2.time
    }
    override fun update() { modification.value++ }
    override fun read() { modification.value }
    override fun add(element: TrackClip<*>): Boolean {
        val r = super.add(element)
        element.track = track
        return r
    }
    override fun add(index: Int, element: TrackClip<*>) {
        super.add(index, element)
        element.track = track
    }
    override fun addAll(elements: Collection<TrackClip<*>>): Boolean {
        val r = super.addAll(elements)
        if (r) elements.forEach { it.track = track }
        return r
    }
    override fun addAll(index: Int, elements: Collection<TrackClip<*>>): Boolean {
        val r = super.addAll(index, elements)
        if (r) elements.forEach { it.track = track }
        return r
    }
    override fun remove(element: TrackClip<*>): Boolean {
        val r = super.remove(element)
        if (r) element.track = null
        return r
    }
    override fun removeAt(index: Int): TrackClip<*> {
        val r = super.removeAt(index)
        r.track = null
        return r
    }
    override fun removeAll(elements: Collection<TrackClip<*>>): Boolean {
        val r = super.removeAll(elements.toSet())
        if (r) elements.forEach { it.track = null }
        return r
    }
}

object ClipFactoryNameSerializer : KSerializer<ClipFactory<*>> {
    override val descriptor = String.serializer().descriptor
    override fun serialize(encoder: Encoder, value: ClipFactory<*>) { encoder.encodeString(value.name) }
    override fun deserialize(decoder: Decoder): ClipFactory<*> {
        val name = decoder.decodeString()
        return ClipManager.instance.factories[name] ?: throw NoSuchFactoryException(name)
    }
}
