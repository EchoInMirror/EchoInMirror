package com.eimsound.daw.api.processor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.audioprocessor.*
import com.eimsound.audioprocessor.data.DefaultEnvelopePointList
import com.eimsound.audioprocessor.data.EnvelopePointList
import com.eimsound.audioprocessor.data.fromJson
import com.eimsound.audioprocessor.data.putNotDefault
import com.eimsound.audioprocessor.dsp.Disabled
import com.eimsound.audioprocessor.dsp.Pan
import com.eimsound.audioprocessor.dsp.Solo
import com.eimsound.audioprocessor.dsp.Volume
import com.eimsound.daw.api.Colorable
import com.eimsound.daw.api.ProjectInformation
import com.eimsound.daw.api.TrackClipList
import com.eimsound.daw.utils.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

enum class ChannelType {
    STEREO, LEFT, RIGHT, MONO, SIDE
}

/**
 * @see com.eimsound.daw.impl.processor.TrackImpl
 */
interface Track : AudioProcessor, Pan, Volume, Solo, Disabled, MidiEventHandler, Colorable, Renderable {
    override var name: String
    val subTracks: MutableList<Track>
    val preProcessorsChain: MutableList<TrackAudioProcessorWrapper>
    val postProcessorsChain: MutableList<TrackAudioProcessorWrapper>
    val internalProcessorsChain: MutableList<AudioProcessor>
    val levelMeter: LevelMeter
    val clips: TrackClipList
    var height: Int
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
}

interface HandledParameter : JsonSerializable {
    val parameter: IAudioProcessorParameter
    val points: EnvelopePointList
}

@Serializable
data class DefaultHandledParameter(
    override val parameter: IAudioProcessorParameter,
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

interface TrackAudioProcessorWrapper : JsonSerializable, AudioProcessor {
    var handledParameters: List<HandledParameter>
}

class DefaultTrackAudioProcessorWrapper(
    val processor: AudioProcessor
) : TrackAudioProcessorWrapper, AudioProcessor by processor {
    override var handledParameters by mutableStateOf(emptyList<HandledParameter>())

    override fun toJson() = buildJsonObject {
        put("processor", if (processor is JsonSerializable) processor.toJson() else JsonPrimitive(processor.id))
        putNotDefault("handledParameters", handledParameters)
    }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
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

    override fun toString(): String {
        return "DefaultTrackAudioProcessorWrapper(processor=$processor)"
    }
}
