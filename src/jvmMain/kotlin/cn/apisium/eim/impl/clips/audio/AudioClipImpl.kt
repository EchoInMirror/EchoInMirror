package cn.apisium.eim.impl.clips.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.*
import cn.apisium.eim.api.processor.Track
import cn.apisium.eim.data.midi.MidiNoteRecorder
import cn.apisium.eim.utils.audiosources.DefaultResampledAudioSource
import cn.apisium.eim.utils.audiosources.MemoryAudioSource
import cn.apisium.eim.utils.audiosources.ResampledAudioSource
import cn.apisium.eim.utils.audiosources.convertFormatToPcm
import com.fasterxml.jackson.databind.JsonNode
import java.io.File
import javax.sound.sampled.AudioSystem

class AudioClipImpl(json: JsonNode?, factory: ClipFactory<AudioClip>): AbstractClip<AudioClip>(json, factory), AudioClip {
    private val target = MemoryAudioSource(
        convertFormatToPcm(
            AudioSystem.getAudioInputStream(File("E:\\CloudMusic\\Halozy - Kiss & Crazy (extended mix).flac"))
        )
    )
    override var audioSource: ResampledAudioSource = DefaultResampledAudioSource(target,
        EchoInMirror.currentPosition.sampleRate.toDouble() / target.sampleRate)
    override val defaultDuration get() = EchoInMirror.currentPosition.convertSamplesToPPQ(audioSource.length)

    init {
        println(target.sampleRate)
    }
}

class AudioClipFactoryImpl: ClipFactory<AudioClip> {
    override val name = "AudioClip"
    override fun createClip() = AudioClipImpl(null, this)
    override fun createClip(path: String, json: JsonNode) = AudioClipImpl(json, this)
    override fun getEditor(clip: TrackClip<AudioClip>, track: Track) = TODO()

    override fun processBlock(clip: TrackClip<AudioClip>, buffers: Array<FloatArray>, position: CurrentPosition,
                              midiBuffer: ArrayList<Int>, noteRecorder: MidiNoteRecorder, pendingNoteOns: LongArray) {
        clip.clip.audioSource.factor = position.sampleRate.toDouble() / clip.clip.audioSource.source.sampleRate
        clip.clip.audioSource.getSamples(position.timeInSamples.toInt(), buffers)
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
