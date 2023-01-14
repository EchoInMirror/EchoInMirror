package com.eimsound.daw.impl.clips.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.eimsound.audioprocessor.*
import com.eimsound.audioprocessor.data.AudioThumbnail
import com.eimsound.audioprocessor.data.midi.MidiNoteRecorder
import com.eimsound.audiosources.DefaultFileAudioSource
import com.eimsound.audiosources.DefaultResampledAudioSource
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.api.*
import com.eimsound.daw.api.processor.Track
import com.fasterxml.jackson.databind.JsonNode
import kotlin.math.absoluteValue

class AudioClipImpl(json: JsonNode?, factory: ClipFactory<AudioClip>): AbstractClip<AudioClip>(json, factory), AudioClip {
    private val target = DefaultFileAudioSource("E:\\CloudMusic\\Halozy - Kiss & Crazy (extended mix).flac")
    override val audioSource: ResampledAudioSource = DefaultResampledAudioSource(target,
        EchoInMirror.currentPosition.sampleRate.toDouble() / target.sampleRate)
    override val defaultDuration get() = EchoInMirror.currentPosition.convertSamplesToPPQ(audioSource.length)
    override val maxDuration get() = defaultDuration
    override val thumbnail = AudioThumbnail(target)
}

private const val STEP_IN_PX = 0.5F

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
                                 noteWidth: MutableState<Dp>, startPPQ: Float, widthPPQ: Float) {
        val curPos = EchoInMirror.currentPosition

        val channels = clip.clip.thumbnail.channels
        val startSeconds = curPos.convertPPQToSeconds(startPPQ)
        val endSeconds = curPos.convertPPQToSeconds(startPPQ + widthPPQ)
        val isDrawMinAndMax = noteWidth.value.value < 1
        Canvas(Modifier.fillMaxSize()) {
            val channelHeight = (trackHeight.toPx() - 4) / channels
            val halfChannelHeight = channelHeight / 2
            if (isDrawMinAndMax) {
                clip.clip.thumbnail.query(size.width.toDouble(), startSeconds, endSeconds, 0.5F) { x, ch, min, max ->
                    val y = 2 + channelHeight * ch + halfChannelHeight
                    drawLine(
                        contentColor,
                        Offset(x, y - max.absoluteValue * halfChannelHeight),
                        Offset(x, y + min.absoluteValue * halfChannelHeight),
                        0.5F
                    )
                }
            } else {
                clip.clip.thumbnail.query(size.width.toDouble(), startSeconds, endSeconds, STEP_IN_PX) { x, ch, min, max ->
                    val y = 2 + channelHeight * ch + halfChannelHeight
                    val v = if (max.absoluteValue > min.absoluteValue) max else min
                    drawLine(
                        contentColor,
                        Offset(x, y),
                        Offset(x, y - v * halfChannelHeight),
                        STEP_IN_PX
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
