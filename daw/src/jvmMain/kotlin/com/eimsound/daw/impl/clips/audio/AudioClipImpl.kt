package com.eimsound.daw.impl.clips.audio

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.eimsound.audioprocessor.*
import com.eimsound.audioprocessor.data.*
import com.eimsound.audioprocessor.data.midi.MidiNoteRecorder
import com.eimsound.daw.api.*
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.components.EnvelopeEditor
import com.eimsound.daw.components.Waveform
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.nio.file.Path
import kotlin.io.path.name

class AudioClipImpl(json: JsonObject?, factory: ClipFactory<AudioClip>, target: AudioSource? = null):
    AbstractClip<AudioClip>(json, factory), AudioClip {
    override var target: AudioSource = target ?:
    AudioSourceManager.instance.createAudioSource(json?.get("target") as? JsonObject ?: throw IllegalStateException("No target"))
        set(value) {
            if (field == value) return
            close()
            field = value
            audioSource = AudioSourceManager.instance.createResampledSource(value)
            audioSource.factor = EchoInMirror.currentPosition.sampleRate.toDouble() / target.sampleRate
            thumbnail = AudioThumbnail(audioSource)
        }
    override var audioSource = AudioSourceManager.instance.createResampledSource(this.target).apply {
        factor = EchoInMirror.currentPosition.sampleRate.toDouble() / this@AudioClipImpl.target.sampleRate
    }
    override val defaultDuration get() = EchoInMirror.currentPosition.convertSamplesToPPQ(audioSource.length)
    override val maxDuration get() = defaultDuration
    override var thumbnail by mutableStateOf(AudioThumbnail(this.audioSource))
    override val volumeEnvelope = DefaultEnvelopePointList().apply { fromJson(json?.get("volumeEnvelope")) }
    override val name: String
        get() {
            var source: AudioSource? = target
            while (source != null) {
                if (source is FileAudioSource) return source.file.name
                source = source.source
            }
            return ""
        }

    override fun close() { target.close() }

    override fun toJson() = buildJsonObject {
        put("id", id)
        put("factory", factory.name)
        put("target", target.toJson())
        putNotDefault("volumeEnvelope", volumeEnvelope)
    }

    override fun fromJson(json: JsonElement) {
        super.fromJson(json)
        json as JsonObject
        json["target"]?.let { target = AudioSourceManager.instance.createAudioSource(it as JsonObject) }
        volumeEnvelope.fromJson(json["volumeEnvelope"])
    }
}

private val logger = KotlinLogging.logger { }
class AudioClipFactoryImpl: AudioClipFactory {
    override val name = "AudioClip"
    override fun createClip(path: Path) = AudioClipImpl(null, this,
        AudioSourceManager.instance.createAudioSource(path))

    override fun createClip() = AudioClipImpl(null, this).apply {
        logger.info { "Creating clip \"${this.id}\"" }
    }
    override fun createClip(path: Path, json: JsonObject): AudioClipImpl {
        logger.info { "Creating clip ${json["id"]} in $path" }
        return AudioClipImpl(json, this)
    }
    override fun getEditor(clip: TrackClip<AudioClip>, track: Track) = AudioClipEditor(clip, track)

    override fun processBlock(clip: TrackClip<AudioClip>, buffers: Array<FloatArray>, position: CurrentPosition,
                              midiBuffer: ArrayList<Int>, noteRecorder: MidiNoteRecorder, pendingNoteOns: LongArray) {
        clip.clip.audioSource.factor = position.sampleRate.toDouble() / clip.clip.audioSource.source!!.sampleRate
        val clipTime = clip.time - clip.start
        clip.clip.audioSource.getSamples(
            position.timeInSamples - position.convertPPQToSamples(clipTime),
            buffers
        )
        val volume = clip.clip.volumeEnvelope.getValue(position.timeInPPQ - clipTime, 1F)
        repeat(buffers.size) { i ->
            repeat(buffers[i].size) { j ->
                buffers[i][j] *= volume
            }
        }
    }

    @Composable
    override fun PlaylistContent(clip: TrackClip<AudioClip>, track: Track, contentColor: Color,
                                 noteWidth: MutableState<Dp>, startPPQ: Float, widthPPQ: Float) {
        val isDrawMinAndMax = noteWidth.value.value < 1
        Box {
            Waveform(clip.clip.thumbnail, EchoInMirror.currentPosition, startPPQ, widthPPQ, clip.clip.volumeEnvelope, contentColor, isDrawMinAndMax)
            remember(clip) {
                EnvelopeEditor(clip.clip.volumeEnvelope, VOLUME_RANGE, 1F, true)
            }.Editor(startPPQ, contentColor, noteWidth, false, clipStartTime = clip.start, stroke = 0.5F)
        }
    }

    override fun save(clip: AudioClip, path: Path) { }

    override fun toString(): String {
        return "MidiClipFactoryImpl(name='$name')"
    }
}
