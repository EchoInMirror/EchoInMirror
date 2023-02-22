package com.eimsound.daw.impl.clips.audio

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.eimsound.audioprocessor.*
import com.eimsound.audioprocessor.data.AudioThumbnail
import com.eimsound.audioprocessor.data.midi.MidiNoteRecorder
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.api.*
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.components.Waveform
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.nio.file.Path
import kotlin.io.path.name

class AudioClipImpl(json: JsonObject?, factory: ClipFactory<AudioClip>, target: AudioSource? = null):
    AbstractClip<AudioClip>(json, factory), AudioClip {
    override var target: AudioSource = target ?:
    AudioSourceManager.instance.createAudioSource(json?.get("target") ?: throw IllegalStateException("No target"))
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
    override fun toJson() = super.toJson().apply {
        put("target", target.toJson())
    }

    override fun fromJson(json: JsonElement) {
        TODO("Not yet implemented")
    }

    override var thumbnail by mutableStateOf(AudioThumbnail(this.audioSource))
    override val name: String?
        get() {
            var source: AudioSource? = target
            while (source != null) {
                if (source is FileAudioSource) return source.file.name
                source = source.source
            }
            return null
        }

    override fun close() { target.close() }
}

class AudioClipFactoryImpl: AudioClipFactory {
    override val name = "AudioClip"
    override fun createClip(path: Path) = AudioClipImpl(null, this,
        AudioSourceManager.instance.createAudioSource(path))

    override fun createClip() = AudioClipImpl(null, this)
    override fun createClip(path: String, json: JsonObject) = AudioClipImpl(json, this)
    override fun getEditor(clip: TrackClip<AudioClip>, track: Track): ClipEditor? = null

    override fun processBlock(clip: TrackClip<AudioClip>, buffers: Array<FloatArray>, position: CurrentPosition,
                              midiBuffer: ArrayList<Int>, noteRecorder: MidiNoteRecorder, pendingNoteOns: LongArray) {
        clip.clip.audioSource.factor = position.sampleRate.toDouble() / clip.clip.audioSource.source!!.sampleRate
        clip.clip.audioSource.getSamples(
            position.timeInSamples - position.convertPPQToSamples(clip.time - clip.start),
            buffers
        )
    }

    @Composable
    override fun playlistContent(clip: TrackClip<AudioClip>, track: Track, contentColor: Color,
                                 noteWidth: MutableState<Dp>, startPPQ: Float, widthPPQ: Float) {
        val curPos = EchoInMirror.currentPosition

        val startSeconds = curPos.convertPPQToSeconds(startPPQ)
        val endSeconds = curPos.convertPPQToSeconds(startPPQ + widthPPQ)
        val isDrawMinAndMax = noteWidth.value.value < 1
        Waveform(clip.clip.thumbnail, startSeconds, endSeconds, contentColor, isDrawMinAndMax)
    }

    override fun save(clip: AudioClip, path: String) { }

    override fun toString(): String {
        return "MidiClipFactoryImpl(name='$name')"
    }
}
