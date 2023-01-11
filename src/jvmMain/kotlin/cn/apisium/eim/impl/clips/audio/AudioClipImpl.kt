package cn.apisium.eim.impl.clips.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.*
import cn.apisium.eim.api.processor.Track
import cn.apisium.eim.data.midi.MidiNoteRecorder
import cn.apisium.eim.impl.audiosources.DefaultFileAudioSource
import cn.apisium.eim.impl.audiosources.DefaultResampledAudioSource
import com.fasterxml.jackson.databind.JsonNode

class AudioClipImpl(json: JsonNode?, factory: ClipFactory<AudioClip>): AbstractClip<AudioClip>(json, factory), AudioClip {
    private val target = DefaultFileAudioSource("E:\\CloudMusic\\Halozy - Kiss & Crazy (extended mix).flac")
    override var audioSource: ResampledAudioSource = DefaultResampledAudioSource(target,
        EchoInMirror.currentPosition.sampleRate.toDouble() / target.sampleRate)
    override val defaultDuration get() = EchoInMirror.currentPosition.convertSamplesToPPQ(audioSource.length)
}

class AudioClipFactoryImpl: ClipFactory<AudioClip> {
    override val name = "AudioClip"
    override fun createClip() = AudioClipImpl(null, this)
    override fun createClip(path: String, json: JsonNode) = AudioClipImpl(json, this)
    override fun getEditor(clip: TrackClip<AudioClip>, track: Track): ClipEditor? = null

    override fun processBlock(clip: TrackClip<AudioClip>, buffers: Array<FloatArray>, position: CurrentPosition,
                              midiBuffer: ArrayList<Int>, noteRecorder: MidiNoteRecorder, pendingNoteOns: LongArray) {
        clip.clip.audioSource.factor = position.sampleRate.toDouble() / clip.clip.audioSource.source!!.sampleRate
        clip.clip.audioSource.getSamples(position.timeInSamples, buffers)
    }

    @Composable
    override fun playlistContent(clip: AudioClip, track: Track, contentColor: Color, trackHeight: Dp, noteWidth: MutableState<Dp>) {
    }

    override fun save(clip: AudioClip, path: String) {
    }

    override fun toString(): String {
        return "MidiClipFactoryImpl(name='$name')"
    }
}
