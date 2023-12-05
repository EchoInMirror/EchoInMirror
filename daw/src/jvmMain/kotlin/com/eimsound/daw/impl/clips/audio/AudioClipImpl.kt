package com.eimsound.daw.impl.clips.audio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.eimsound.audioprocessor.*
import com.eimsound.audiosources.*
import com.eimsound.daw.api.*
import com.eimsound.daw.api.clips.*
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.commons.json.asFloat
import com.eimsound.daw.commons.json.asString
import com.eimsound.daw.commons.json.putNotDefault
import com.eimsound.daw.components.*
import com.eimsound.dsp.data.*
import com.eimsound.dsp.data.midi.MidiNoteTimeRecorder
import com.eimsound.dsp.detectBPM
import com.eimsound.dsp.timestretcher.TimeStretcher
import com.eimsound.dsp.timestretcher.TimeStretcherManager
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class AudioClipImpl(
    factory: ClipFactory<AudioClip>, target: AudioSource? = null
): AbstractClip<AudioClip>(factory), AudioClip {
    private var _target: AudioSource? = null
    private lateinit var fifo: AudioBufferQueue
    override var target: AudioSource
        get() = _target ?: throw IllegalStateException("Target is not set")
        set(value) {
            if (_target == value) return
            close()
            _target = value
            _thumbnail = AudioThumbnail(value)
            fifo = AudioBufferQueue(value.channels, 20480)
            timeStretcher?.initialise(target.sampleRate, EchoInMirror.currentPosition.bufferSize, target.channels)
        }
    override var timeStretcher: TimeStretcher? = null
    override val timeInSeconds: Float
        get() = target.timeInSeconds * (timeStretcher?.speedRatio ?: 1F)
    override val defaultDuration get() = EchoInMirror.currentPosition
        .convertSamplesToPPQ((target.length * (timeStretcher?.speedRatio ?: 1F)).roundToLong())
    override val maxDuration get() = defaultDuration
    private var _thumbnail by mutableStateOf<AudioThumbnail?>(null)
    override val thumbnail get() = _thumbnail ?: throw IllegalStateException("Thumbnail is not set")
    override val volumeEnvelope = DefaultEnvelopePointList()
    override val name: String
        get() {
            var source: AudioSource? = target
            while (source != null) {
                if (source is FileAudioSource) return source.file.name
                source = source.source
            }
            return ""
        }

    init {
        if (target != null) this.target = target
    }

    override fun close() {
        _target?.close()
        timeStretcher?.close()
        timeStretcher = null
    }

    override fun toJson() = buildJsonObject {
        put("id", id)
        put("factory", factory.name)
        timeStretcher?.apply {
            put("timeStretcher", name)
            putNotDefault("semitones", semitones)
            if (speedRatio != 1F) put("speedRatio", speedRatio)
        }
        put("target", target.toJson())
        putNotDefault("volumeEnvelope", volumeEnvelope)
    }

    override fun fromJson(json: JsonElement) {
        super.fromJson(json)
        json as JsonObject
        json["target"]?.let { target = AudioSourceManager.instance.createAudioSource(it as JsonObject) }
        json["volumeEnvelope"]?.let { volumeEnvelope.fromJson(it) }
        json["timeStretcher"]?.let {
            val name = it.asString()
            val timeStretcher = TimeStretcherManager.createTimeStretcher(name)
            if (timeStretcher == null) logger.warn { "Time stretcher \"$name\" not found" }
            else {
                this.timeStretcher?.close()
                this.timeStretcher = timeStretcher
                timeStretcher.semitones = json["semitones"]?.asFloat() ?: 0F
                timeStretcher.speedRatio = json["speedRatio"]?.asFloat() ?: 1F
            }
        }
    }

    private var tempInBuffers: Array<FloatArray> = emptyArray()
    private var tempOutBuffers: Array<FloatArray> = emptyArray()
    private var position = 0L
    private var lastPos = -1L
    private fun fillNextBlock(channels: Int) {
        val tr = timeStretcher ?: return
        val needed = tr.framesNeeded

        val numRead = if (needed >= 0) {
            if (tempInBuffers.size != channels || tempInBuffers[0].size < needed)
                tempInBuffers = Array(target.channels) { FloatArray(needed) }
            val read = target.getSamples(position, needed, tempInBuffers)
            if (read > 0) position += read

            tr.process(tempInBuffers, tempOutBuffers, needed)
        } else {
            tr.flush(tempOutBuffers)
        }

        if (numRead > 0) fifo.push(tempOutBuffers, 0, numRead)
    }
    internal fun processBlock(pos: CurrentPosition, playTime: Long, buffers: Array<FloatArray>) {
        val bufferSize = buffers[0].size
        val tr = timeStretcher
        if (tr == null || tr.isDefaultParams) {
            target.getSamples(playTime, bufferSize, buffers)
            return
        } else if (pos.timeInSamples != lastPos + bufferSize) {
            position = (playTime * tr.speedRatio).roundToLong()
            fifo.clear()
            tr.reset()
        }
        lastPos = pos.timeInSamples
        val channels = buffers.size
        if (tempOutBuffers.size != channels || tempOutBuffers[0].size < bufferSize)
            tempOutBuffers = Array(channels) { FloatArray(bufferSize) }
        while (fifo.available < bufferSize) fillNextBlock(channels)
        fifo.pop(buffers)
    }
}

private val logger = KotlinLogging.logger { }
class AudioClipFactoryImpl: AudioClipFactory {
    override val name = "AudioClip"
    override fun createClip(path: Path) = AudioClipImpl(this, AudioSourceManager.instance.createAudioSource(path))
    override fun createClip() = AudioClipImpl(this).apply {
        logger.info { "Creating clip \"${this.id}\"" }
    }
    override fun createClip(target: AudioSource) = AudioClipImpl(this, target).apply {
        logger.info { "Creating clip \"${this.id}\" with target $target" }
    }
    override fun createClip(path: Path, json: JsonObject) = AudioClipImpl(this).apply {
        logger.info { "Creating clip ${json["id"]} in $path" }
        fromJson(json)
    }
    override fun getEditor(clip: TrackClip<AudioClip>) = AudioClipEditor(clip)

    override fun processBlock(
        clip: TrackClip<AudioClip>, buffers: Array<FloatArray>, position: CurrentPosition,
        midiBuffer: ArrayList<Int>, noteRecorder: MidiNoteTimeRecorder
    ) {
        val c = clip.clip as? AudioClipImpl ?: return
        val clipTime = clip.time - clip.start
        val playTime = position.timeInSamples - position.convertPPQToSamples(clipTime)
        if (playTime < 0) return
        c.processBlock(position, playTime, buffers)
        val volume = clip.clip.volumeEnvelope.getValue(position.timeInPPQ - clipTime, 1F)
        repeat(buffers.size) { i ->
            repeat(buffers[i].size) { j ->
                buffers[i][j] *= volume
            }
        }
    }

    @Composable
    override fun PlaylistContent(
        clip: TrackClip<AudioClip>, track: Track, contentColor: Color,
        noteWidth: MutableState<Dp>, startPPQ: Float, widthPPQ: Float
    ) {
        val isDrawMinAndMax = noteWidth.value.value < LocalDensity.current.density
        Box {
            Waveform(
                clip.clip.thumbnail, EchoInMirror.currentPosition, startPPQ, widthPPQ,
                clip.clip.timeStretcher?.speedRatio ?: 1F,
                clip.clip.volumeEnvelope, contentColor, isDrawMinAndMax
            )
            remember(clip) {
                EnvelopeEditor(clip.clip.volumeEnvelope, VOLUME_RANGE, 1F, true)
            }.Editor(startPPQ, contentColor, noteWidth, false, clipStartTime = clip.start, stroke = 0.5F, drawGradient = false)
        }
    }

    @Composable
    override fun MenuContent(clips: List<TrackClip<*>>, close: () -> Unit) {
        val trackClip = clips.firstOrNull { it.clip is AudioClip } ?: return
        val clip = trackClip.clip as AudioClip
        val snackbarProvider = LocalSnackbarProvider.current

        Divider()
        MenuItem({
            close()
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch(Dispatchers.IO) {
                val bpm = detectBPM(clip.target.copy())
                if (bpm.isEmpty()) {
                    snackbarProvider.enqueueSnackbar("采样时间过短!", SnackbarType.Error)
                    return@launch
                }
                snackbarProvider.enqueueSnackbar {
                    Text("检测到速度: ")
                    val color = LocalContentColor.current.copy(0.5F)
                    bpm.firstOrNull()?.let {
                        Text(it.first.toString())
                        Text("(${(it.second * 100).roundToInt()}%)", color = color)
                    }
                    bpm.subList(1, bpm.size).forEach {
                        Text(
                            ", ${it.first}(${(it.second * 100).roundToInt()}%)",
                            color = color
                        )
                    }
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("检测速度")
        }
    }

    override fun split(clip: TrackClip<AudioClip>, time: Int): ClipSplitResult<AudioClip> {
        return object : ClipSplitResult<AudioClip> {
            override val clip = copy(clip.clip) as AudioClip
            override val start = time
            override fun revert() { }
        }
    }

    override fun save(clip: AudioClip, path: Path) { }

    override fun toString(): String {
        return "AudioClipFactoryImpl"
    }

    override fun copy(clip: AudioClip) = createClip(clip.target.copy()).apply {
        volumeEnvelope.addAll(clip.volumeEnvelope.copy())
    }

    override fun merge(clips: Collection<TrackClip<*>>): List<ClipActionResult<AudioClip>> = emptyList()
    override fun canMerge(clip: TrackClip<*>) = false
}

class AudioFileExtensionHandler : AbstractFileExtensionHandler() {
    override val icon = Icons.Outlined.MusicNote
    // wav, mp3, ogg, flac, aiff, aif
    override val extensions = Regex("\\.(wav|mp3|ogg|flac|aiff|aif)$", RegexOption.IGNORE_CASE)

    override suspend fun createClip(file: Path, data: Any?) = ClipManager.instance.defaultAudioClipFactory.createClip(file)
}

