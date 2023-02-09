package com.eimsound.daw.impl.clips.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.eimsound.audioprocessor.*
import com.eimsound.audioprocessor.data.AudioThumbnail
import com.eimsound.audioprocessor.data.midi.MidiNoteRecorder
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.api.*
import com.eimsound.daw.api.processor.Track
import com.fasterxml.jackson.databind.JsonNode
import kotlin.io.path.name
import kotlin.math.absoluteValue

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

private const val STEP_IN_PX = 0.5F

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

        val channels = clip.clip.thumbnail.channels
        val startSeconds = curPos.convertPPQToSeconds(startPPQ)
        val endSeconds = curPos.convertPPQToSeconds(startPPQ + widthPPQ)
        val isDrawMinAndMax = noteWidth.value.value < 1
        Canvas(Modifier.fillMaxSize()) {
            val channelHeight = (size.height / channels) - 2
            val halfChannelHeight = channelHeight / 2
            val drawHalfChannelHeight = halfChannelHeight - 1
            if (isDrawMinAndMax) {
                clip.clip.thumbnail.query(size.width.toDouble(), startSeconds, endSeconds, 0.5F) { x, ch, min, max ->
                    val y = 2 + channelHeight * ch + halfChannelHeight
                    if (min == 0F && max == 0F) {
                        drawLine(contentColor, Offset(x, y), Offset(x + STEP_IN_PX, y), STEP_IN_PX)
                        return@query
                    }
                    drawLine(
                        contentColor,
                        Offset(x, y - max.absoluteValue * drawHalfChannelHeight),
                        Offset(x, y + min.absoluteValue * drawHalfChannelHeight),
                        0.5F
                    )
                }
            } else {
                clip.clip.thumbnail.query(size.width.toDouble(), startSeconds, endSeconds, STEP_IN_PX) { x, ch, min, max ->
                    val v = if (max.absoluteValue > min.absoluteValue) max else min
                    val y = 2 + channelHeight * ch + halfChannelHeight
                    if (v == 0F) {
                        drawLine(contentColor, Offset(x, y), Offset(x + STEP_IN_PX, y), STEP_IN_PX)
                        return@query
                    }
                    drawLine(
                        contentColor,
                        Offset(x, y),
                        Offset(x, y - v * drawHalfChannelHeight),
                        STEP_IN_PX
                    )
                }
            }
        }
    }

    override fun save(clip: AudioClipImpl, path: String) = clip

    override fun toString(): String {
        return "MidiClipFactoryImpl(name='$name')"
    }
}
