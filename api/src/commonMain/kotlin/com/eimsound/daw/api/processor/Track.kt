package com.eimsound.daw.api.processor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.audioprocessor.AudioProcessor
import com.eimsound.audioprocessor.CurrentPosition
import com.eimsound.audioprocessor.MidiEventHandler
import com.eimsound.audioprocessor.Renderable
import com.eimsound.audioprocessor.dsp.Disabled
import com.eimsound.audioprocessor.dsp.Pan
import com.eimsound.audioprocessor.dsp.Solo
import com.eimsound.audioprocessor.dsp.Volume
import com.eimsound.daw.api.Colorable
import com.eimsound.daw.api.ProjectInformation
import com.eimsound.daw.api.TrackClipList
import com.eimsound.daw.utils.JsonSerializable
import com.eimsound.daw.utils.LevelMeter
import com.eimsound.daw.utils.asBoolean
import com.eimsound.daw.utils.putNotDefault
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

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

interface TrackAudioProcessorWrapper : JsonSerializable {
    val processor: AudioProcessor
    var isBypassed: Boolean
}

class DefaultTrackAudioProcessorWrapper(
    override val processor: AudioProcessor,
    isBypassed: Boolean = false
) : TrackAudioProcessorWrapper {
    override var isBypassed by mutableStateOf(isBypassed)
    override fun toJson() = buildJsonObject {
        put("processor", if (processor is JsonSerializable) processor.toJson() else JsonPrimitive(processor.id))
        putNotDefault("isBypassed", isBypassed)
    }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        json["isBypassed"]?.let { isBypassed = it.asBoolean() }
    }

    override fun toString(): String {
        return "DefaultTrackAudioProcessorWrapper(processor=$processor, isBypassed=$isBypassed)"
    }
}
