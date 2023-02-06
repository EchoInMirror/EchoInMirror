package com.eimsound.daw.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.eimsound.audioprocessor.CurrentPosition
import com.eimsound.audioprocessor.ResampledAudioSource
import com.eimsound.audioprocessor.data.AudioThumbnail
import com.eimsound.audioprocessor.data.EnvelopePointList
import com.eimsound.audioprocessor.data.midi.MidiNoteRecorder
import com.eimsound.audioprocessor.data.midi.NoteMessageList
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.utils.IManualState
import com.eimsound.daw.utils.NoSuchFactoryException
import com.eimsound.daw.utils.randomId
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File

interface ClipEditor {
    @Composable fun Content()
    fun delete()
    fun copy()
    fun cut()
    fun paste()
    fun selectAll()
}

interface MidiClipEditor: ClipEditor {
    val clip: TrackClip<MidiClip>
    val track: Track
    fun copyAsString(): String
    fun pasteFromString(value: String)
}

interface ClipFactory<T: Clip> {
    val name: String
    fun createClip(): T
    fun createClip(path: String, json: JsonNode): T
    fun processBlock(clip: TrackClip<T>, buffers: Array<FloatArray>, position: CurrentPosition,
                     midiBuffer: ArrayList<Int>, noteRecorder: MidiNoteRecorder, pendingNoteOns: LongArray)
    fun save(clip: T, path: String) {
        jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValue(File("$path.json"), clip)
    }
    fun getEditor(clip: TrackClip<T>, track: Track): ClipEditor?
    @Composable
    fun playlistContent(clip: TrackClip<T>, track: Track, contentColor: Color,
                        noteWidth: MutableState<Dp>, startPPQ: Float, widthPPQ: Float
    )
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
    fun <T: Clip> createTrackClip(clip: T, time: Int = 0, duration: Int = clip.defaultDuration.coerceAtLeast(0),
                                  start: Int = 0, track: Track? = null): TrackClip<T>
    suspend fun createTrackClip(path: String, json: JsonNode): TrackClip<Clip>
}

@Suppress("UNCHECKED_CAST")
val ClipManager.defaultMidiClipFactory get() = factories["MIDIClip"] as ClipFactory<MidiClip>
@Suppress("UNCHECKED_CAST")
val ClipManager.defaultAudioClipFactory get() = factories["AudioClip"] as ClipFactory<AudioClip>

interface Clip {
    val id: String
    val name: String?
    @get:JsonSerialize(using = ClipFactoryNameSerializer::class)
    val factory: ClipFactory<*>
    @get:JsonIgnore
    val defaultDuration: Int
    @get:JsonIgnore
    val isExpandable: Boolean
    @get:JsonIgnore
    val maxDuration: Int
}

interface MidiCCEvent {
    val id: Int
    val points: EnvelopePointList
}

interface MidiClip : Clip {
    val notes: NoteMessageList
    val events: MutableList<MidiCCEvent>
}
interface AudioClip : Clip {
    val audioSource: ResampledAudioSource
    val thumbnail: AudioThumbnail
}

abstract class AbstractClip<T: Clip>(json: JsonNode?, override val factory: ClipFactory<T>) : Clip {
    override val id = json?.get("id")?.asText() ?: randomId()
    override val name: String? = null
    override val isExpandable = false
    override val defaultDuration = -1
    override val maxDuration = -1

    override fun toString(): String {
        return "MidiClipImpl(factory=$factory, id='$id')"
    }
}

interface TrackClip<T: Clip> {
    var time: Int
    var duration: Int
    var start: Int
    @get:JsonSerialize(using = ClipIdSerializer::class)
    val clip: T
    @get:JsonIgnore
    var currentIndex: Int
    @get:JsonIgnore
    var track: Track?
    fun reset()
}

@Suppress("UNCHECKED_CAST", "unused")
fun TrackClip<*>.asMidiTrackClip() = this as TrackClip<MidiClip>
@Suppress("UNCHECKED_CAST")
fun TrackClip<*>.asMidiTrackClipOrNull() = if (clip is MidiClip) this as TrackClip<MidiClip> else null

interface TrackClipList : MutableList<TrackClip<*>>, IManualState {
    fun sort()
}

class DefaultTrackClipList(@JsonIgnore private val track: Track) : TrackClipList, ArrayList<TrackClip<*>>() {
    private var modification = mutableStateOf(0)
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
