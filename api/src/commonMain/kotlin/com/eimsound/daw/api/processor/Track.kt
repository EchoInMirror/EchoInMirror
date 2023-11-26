package com.eimsound.daw.api.processor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.audioprocessor.*
import com.eimsound.audioprocessor.data.*
import com.eimsound.audioprocessor.interfaces.Pan
import com.eimsound.audioprocessor.interfaces.Volume
import com.eimsound.daw.api.Colorable
import com.eimsound.daw.api.LevelMeter
import com.eimsound.daw.api.ProjectInformation
import com.eimsound.daw.api.TrackClipList
import com.eimsound.daw.commons.actions.Restorable
import com.eimsound.daw.commons.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

enum class ChannelType {
    STEREO, LEFT, RIGHT, MONO, SIDE
}

/**
 * @see com.eimsound.daw.impl.processor.TrackImpl
 */
interface Track : AudioProcessor, Pan, Volume, MidiEventHandler, Colorable, Renderable {
    override var name: String
    val subTracks: MutableList<Track>
    val preProcessorsChain: MutableList<TrackAudioProcessorWrapper>
    val postProcessorsChain: MutableList<TrackAudioProcessorWrapper>
    val internalProcessorsChain: MutableList<AudioProcessor>
    val levelMeter: LevelMeter
    val clips: TrackClipList
    var height: Int
    var collapsed: Boolean
    var isSolo: Boolean

    override suspend fun processBlock(
        buffers: Array<FloatArray>,
        position: CurrentPosition,
        midiBuffer: ArrayList<Int>
    )
}

/**
 * @see com.eimsound.daw.impl.processor.BusImpl
 */
interface Bus : Track {
    val project: ProjectInformation
    var channelType: ChannelType
    val lastSaveTime: Long
    suspend fun save()
    fun findProcessor(uuid: UUID): TrackAudioProcessorWrapper?
    fun onLoaded(callback: () -> Unit)
}

interface HandledParameter : JsonSerializable {
    val parameter: AudioProcessorParameter
    val points: EnvelopePointList
}

@Serializable
data class DefaultHandledParameter(
    override val parameter: AudioProcessorParameter,
    override val points: EnvelopePointList = DefaultEnvelopePointList()
) : HandledParameter {
    override fun toJson() = buildJsonObject {
        put("id", parameter.id)
        putNotDefault("points", points)
    }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        points.fromJson(json["points"])
    }
}

interface TrackAudioProcessorWrapper : JsonSerializable, Restorable, AutoCloseable {
    var handledParameters: List<HandledParameter>
    val processor: AudioProcessor
    var name: String

    override fun close() {
        processor.close()
    }
}

class DefaultTrackAudioProcessorWrapper(
    override val processor: AudioProcessor
) : TrackAudioProcessorWrapper {
    override var handledParameters by mutableStateOf(emptyList<HandledParameter>())
    private var inited = false
    private var _name by mutableStateOf("")
    override var name
        get() = _name.ifEmpty { processor.name }
        set(value) { _name = if (value == processor.name) "" else value }

    override fun toJson() = buildJsonObject {
        putNotDefault("handledParameters", handledParameters)
        putNotDefault("name", _name)
    }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        if (json["name"]?.asString() != null) _name = json["name"]!!.asString()
        handledParameters = json["handledParameters"]?.let { arr ->
            (arr as JsonArray).mapNotNull { elm ->
                if (elm !is JsonObject) return@mapNotNull null
                val id = elm["id"]?.asString() ?: return@mapNotNull null
                DefaultHandledParameter(
                    processor.parameters.firstOrNull { it.id == id } ?: return@mapNotNull null
                ).apply { fromJson(elm) }
            }
        } ?: emptyList()
    }

    override suspend fun restore(path: Path) {
        withContext(Dispatchers.IO) {
            val file = path.resolve("wrapper.json")
            if (Files.exists(file)) fromJsonFile(file)
            if (inited) processor.restore(path)
            else inited = true
        }
    }

    override suspend fun store(path: Path) {
        withContext(Dispatchers.IO) {
            encodeJsonFile(path.resolve("wrapper.json"), true)
            processor.store(path)
        }
    }

    override fun toString(): String {
        return "DefaultTrackAudioProcessorWrapper(processor=$processor)"
    }
}
