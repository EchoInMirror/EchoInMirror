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
import com.fasterxml.jackson.databind.JsonNode
import kotlin.io.path.name

class AudioClipImpl(json: JsonNode?, factory: ClipFactory<AudioClipImpl>): AbstractClip<AudioClipImpl>(json, factory), AudioClip {
    override var target: AudioSource = AudioSourceManager.instance.createAudioSource(json?.get("target") ?: throw IllegalStateException("No target"))
        set(value) {
            if (field == value) return
            field = value
            audioSource = AudioSourceManager.instance.createResampledSource(value)
            audioSource.factor = EchoInMirror.currentPosition.sampleRate.toDouble() / target.sampleRate
            thumbnail = AudioThumbnail(value)
        }
    override var audioSource = AudioSourceManager.instance.createResampledSource(target).apply {
        factor = EchoInMirror.currentPosition.sampleRate.toDouble() / target.sampleRate
    }
    override val defaultDuration get() = EchoInMirror.currentPosition.convertSamplesToPPQ(audioSource.length)
    override val maxDuration get() = defaultDuration
    override var thumbnail by mutableStateOf(AudioThumbnail(target))
    override val name: String?
        get() {
            var source: AudioSource? = target
            while (source != null) {
                if (source is FileAudioSource) return source.file.name
                source = source.source
            }
            return null
        }
}

class AudioClipFactoryImpl: ClipFactory<AudioClipImpl> {
    override val name = "AudioClip"
    override fun createClip() = AudioClipImpl(null, this)
    override fun createClip(path: String, json: JsonNode) = AudioClipImpl(json, this)
    override fun getEditor(clip: TrackClip<AudioClipImpl>, track: Track): ClipEditor? = null

    override fun processBlock(clip: TrackClip<AudioClipImpl>, buffers: Array<FloatArray>, position: CurrentPosition,
                              midiBuffer: ArrayList<Int>, noteRecorder: MidiNoteRecorder, pendingNoteOns: LongArray) {
        clip.clip.audioSource.factor = position.sampleRate.toDouble() / clip.clip.audioSource.source!!.sampleRate
        clip.clip.audioSource.getSamples(
            position.timeInSamples - position.convertPPQToSamples(clip.time - clip.start),
            buffers
        )
    }

    @Composable
    override fun playlistContent(clip: TrackClip<AudioClipImpl>, track: Track, contentColor: Color,
                                 noteWidth: MutableState<Dp>, startPPQ: Float, widthPPQ: Float) {
        val curPos = EchoInMirror.currentPosition

        val startSeconds = curPos.convertPPQToSeconds(startPPQ)
        val endSeconds = curPos.convertPPQToSeconds(startPPQ + widthPPQ)
        val isDrawMinAndMax = noteWidth.value.value < 1
        Waveform(clip.clip.thumbnail, startSeconds, endSeconds, contentColor, isDrawMinAndMax)
    }

    override fun save(clip: AudioClipImpl, path: String) { }

    override fun toString(): String {
        return "MidiClipFactoryImpl(name='$name')"
    }
}
