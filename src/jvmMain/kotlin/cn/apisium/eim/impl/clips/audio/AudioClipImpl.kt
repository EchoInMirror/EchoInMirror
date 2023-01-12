package cn.apisium.eim.impl.clips.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.*
import cn.apisium.eim.api.processor.Track
import cn.apisium.eim.data.midi.MidiNoteRecorder
import cn.apisium.eim.impl.audiosources.DefaultFileAudioSource
import cn.apisium.eim.impl.audiosources.DefaultResampledAudioSource
import cn.apisium.eim.utils.datastructure.AudioThumbnail
import com.fasterxml.jackson.databind.JsonNode

class AudioClipImpl(json: JsonNode?, factory: ClipFactory<AudioClip>): AbstractClip<AudioClip>(json, factory), AudioClip {
    private val target = DefaultFileAudioSource("E:\\CloudMusic\\Halozy - Kiss & Crazy (extended mix).flac")
    override val audioSource: ResampledAudioSource = DefaultResampledAudioSource(target,
        EchoInMirror.currentPosition.sampleRate.toDouble() / target.sampleRate)
    override val defaultDuration get() = EchoInMirror.currentPosition.convertSamplesToPPQ(audioSource.length)
    override val thumbnail = AudioThumbnail(target)
}

class AudioClipFactoryImpl: ClipFactory<AudioClip> {
    override val name = "AudioClip"
    override fun createClip() = AudioClipImpl(null, this)
    override fun createClip(path: String, json: JsonNode) = AudioClipImpl(json, this)
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
    override fun playlistContent(clip: TrackClip<AudioClip>, track: Track, contentColor: Color, trackHeight: Dp,
                                 noteWidth: MutableState<Dp>, contentWidth: Dp, scrollState: ScrollState) {
        with(LocalDensity.current) {
            val noteWidthPx = noteWidth.value.toPx()
            val scrollX = scrollState.value
            val frameStartPPQ = scrollX / noteWidthPx
            val frameLengthPPQ = contentWidth.toPx() / noteWidthPx
            var endPPQ = (clip.time + clip.duration).toFloat()
            if (clip.time > frameStartPPQ + frameLengthPPQ || endPPQ < frameStartPPQ) return
            val startPPQ = (frameStartPPQ - (clip.time - clip.start)).coerceAtLeast(clip.start.toFloat())
            endPPQ = startPPQ + frameLengthPPQ.coerceAtMost(clip.duration.toFloat())
            val curPos = EchoInMirror.currentPosition

            val channels = clip.clip.thumbnail.channels
            val startSeconds = curPos.convertPPQToSeconds(startPPQ)
            val endSeconds = curPos.convertPPQToSeconds(endPPQ)
            val widthInPx = (endPPQ - startPPQ) * noteWidthPx.toDouble()
            Canvas(Modifier.fillMaxSize()) {
                val channelHeight = trackHeight.toPx() / channels
                val halfChannelHeight = channelHeight / 2
                clip.clip.thumbnail.query(widthInPx, startSeconds, endSeconds, 0.5F) { x, ch, min, max ->
                    val y = channelHeight * ch + halfChannelHeight
                    val curX = startPPQ * noteWidthPx + x
                    drawLine(
                        contentColor,
                        Offset(curX, y - max * halfChannelHeight),
                        Offset(curX, y - min * halfChannelHeight),
                        0.5F
                    )
                }
            }
        }
    }

    override fun save(clip: AudioClip, path: String) {
    }

    override fun toString(): String {
        return "MidiClipFactoryImpl(name='$name')"
    }
}
