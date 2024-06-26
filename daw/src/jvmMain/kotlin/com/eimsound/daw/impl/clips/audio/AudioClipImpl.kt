package com.eimsound.daw.impl.clips.audio

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.eimsound.daw.language.langs
import com.eimsound.daw.utils.observableMutableStateOf
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
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class AudioClipImpl(
    factory: ClipFactory<AudioClip>, target: FileAudioSource? = null,
    audioThumbnail: AudioThumbnail? = null
): AbstractClip<AudioClip>(factory), AudioClip {
    private lateinit var fifo: AudioBufferQueue
    override var target: FileAudioSource? = null
        set(value) {
            if (field == value) return
            close()
            field = value
            if (value != null) {
                fifo = AudioBufferQueue(value.channels, 20480)
                stretcher?.initialise(value.sampleRate, EchoInMirror.currentPosition.bufferSize, value.channels)
            }
        }
    private var stretcher: TimeStretcher? = null
        set(value) {
            if (field == value) return
            field?.close()
            if (value == null) {
                field = null
                return
            }
            target?.apply {
                value.initialise(sampleRate, EchoInMirror.currentPosition.bufferSize, channels)
                value.semitones = semitones
                value.speedRatio = speedRatio
            }
            field = value
        }
    override val timeInSeconds: Float
        get() = target?.let { it.timeInSeconds * (stretcher?.speedRatio ?: 1F) } ?: 0F
    override var speedRatio by observableMutableStateOf(1F) {
        stretcher?.speedRatio = it
    }
    override var semitones by observableMutableStateOf(0F) {
        stretcher?.semitones = it
    }
    private var timeStretcherName by mutableStateOf("")
    override var timeStretcher
        get() = timeStretcherName
        set(value) {
            value.ifEmpty {
                stretcher?.close()
                stretcher = null
                timeStretcherName = ""
                return
            }
            try {
                stretcher = TimeStretcherManager.createTimeStretcher(value) ?: return
                timeStretcherName = value
            } catch (e: Exception) {
                logger.warn(e) { "Failed to create time stretcher \"$value\"" }
            }
        }
    override var bpm = 0F
    override val defaultDuration get() = target?.let {
        EchoInMirror.currentPosition.convertSamplesToPPQ((it.length * (stretcher?.speedRatio ?: 1F)).roundToLong())
    } ?: 0
    override val maxDuration get() = defaultDuration
    override var thumbnail by mutableStateOf(audioThumbnail)
        private set
    override val volumeEnvelope = DefaultEnvelopePointList()
    override fun copy() = AudioClipImpl(factory, target?.copy(), thumbnail).also {
        it.volumeEnvelope.addAll(volumeEnvelope.copy())
        it.bpm = bpm
        it.speedRatio = speedRatio
        it.semitones = semitones
        it.timeStretcher = timeStretcher
    }

    override val icon = Icons.Outlined.GraphicEq
    override val name get() = target?.file?.name ?: langs.audioClipLangs.noAudio

    init {
        if (target != null) {
            this.target = target
            if (audioThumbnail == null) EchoInMirror.audioThumbnailCache.get(target.file) { it, _ -> thumbnail = it }
        }
    }

    override fun close() {
        target?.close()
        stretcher?.close()
        stretcher = null
    }

    override fun toJson() = buildJsonObject {
        put("id", id)
        put("factory", factory.name)
        putNotDefault("file", target?.file?.toString())
        putNotDefault("timeStretcher", timeStretcher)
        putNotDefault("speedRatio", speedRatio, 1F)
        putNotDefault("semitones", semitones)
        putNotDefault("volumeEnvelope", volumeEnvelope)
        putNotDefault("bpm", bpm)
    }

    override fun fromJson(json: JsonElement) {
        super.fromJson(json)
        json as JsonObject
        json["file"]?.let {
            val target = AudioSourceManager.createCachedFileSource(Path(it.asString()))
            this.target = target
            EchoInMirror.audioThumbnailCache.get(target.file) { t, _ -> thumbnail = t }
        }
        json["volumeEnvelope"]?.let { volumeEnvelope.fromJson(it) }
        json["bpm"]?.let { bpm = it.asFloat() }
        json["timeStretcher"]?.let {
            val name = it.asString()
            val timeStretcher = TimeStretcherManager.createTimeStretcher(name)
            if (timeStretcher == null) logger.warn { "Time stretcher \"$name\" not found" }
            else {
                this.timeStretcher = name
                stretcher?.close()
                stretcher = timeStretcher
            }
        }
        semitones = json["semitones"]?.asFloat() ?: 0F
        speedRatio = json["speedRatio"]?.asFloat() ?: 1F
    }

    private var tempInBuffers: Array<FloatArray> = emptyArray()
    private var tempOutBuffers: Array<FloatArray> = emptyArray()
    private var lastPos = -1L
    private fun fillNextBlock(channels: Int): Boolean {
        val target = this.target ?: return true
        val tr = stretcher ?: return true
        val needed = tr.framesNeeded
        var read = 0

        val numRead = if (needed >= 0) {
            if (tempInBuffers.size != channels || tempInBuffers[0].size < needed)
                tempInBuffers = Array(target.channels) { FloatArray(needed) }
            read = target.nextBlock(tempInBuffers, needed)

            tr.process(tempInBuffers, tempOutBuffers, needed)
        } else {
            tr.flush(tempOutBuffers)
        }

        if (numRead > 0) fifo.push(tempOutBuffers, 0, numRead)
        return numRead < 1 && read < 1
    }

    private val pauseProcessor = PauseProcessor()
    internal fun processBlock(pos: PlayPosition, playTime: Long, len: Int, buffers: Array<FloatArray>) {
        val target = this.target ?: return
        var bufferSize = (buffers[0].size).coerceAtMost(len)
        var offset = 0
        val tr = stretcher
        if (playTime < 0) {
            target.position = 0
            offset = (-playTime).toInt().coerceIn(0, bufferSize)
        } else if (playTime == 0L || pos.timeInSamples != lastPos + bufferSize) {
            target.position = if (tr == null || tr.isDefaultParams) playTime
            else {
                fifo.clear()
                tr.reset()
                (playTime * tr.speedRatio).roundToLong()
            }
        }
        lastPos = pos.timeInSamples

        if (tr == null || tr.isDefaultParams) {
            target.nextBlock(buffers, bufferSize.coerceAtMost(bufferSize - offset), offset)
            pauseProcessor.processPause(buffers, offset, bufferSize, pos.timeToPause)
            return
        }
        val channels = buffers.size
        if (tempOutBuffers.size != channels || tempOutBuffers[0].size < bufferSize)
            tempOutBuffers = Array(channels) { FloatArray(bufferSize) }
        if (playTime < 0) bufferSize = (-playTime).toInt().coerceAtMost(bufferSize)
        if (bufferSize == 0) return
        while (fifo.available < bufferSize) if (fillNextBlock(channels)) break
        fifo.pop(buffers, offset, bufferSize)
        pauseProcessor.processPause(buffers, offset, bufferSize, pos.timeToPause)
    }

    suspend fun detectBPM(snackbarProvider: SnackbarProvider? = null): Int = withContext(Dispatchers.IO) {
        val bpm = detectBPM((target ?: return@withContext -1).copy())
        if (bpm.isEmpty()) {
            snackbarProvider?.enqueueSnackbar(langs.audioClipLangs.sampleTimeTooShort, SnackbarType.Error)
            return@withContext -1
        }
        snackbarProvider?.enqueueSnackbar {
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
        bpm.firstOrNull()?.first ?: -1
    }
}

private val logger = KotlinLogging.logger { }
class AudioClipFactoryImpl: AudioClipFactory {
    override val name = "AudioClip"
    override fun createClip(path: Path) = AudioClipImpl(this, AudioSourceManager.createCachedFileSource(path))
    override fun createClip() = AudioClipImpl(this).apply {
        logger.info { "Creating clip \"${this.id}\"" }
    }
    override fun createClip(target: FileAudioSource) = AudioClipImpl(this, target).apply {
        logger.info { "Creating clip \"${this.id}\" with target $target" }
    }
    override fun createClip(path: Path, json: JsonObject) = AudioClipImpl(this).apply {
        logger.info { "Creating clip ${json["id"]} in $path" }
        fromJson(json)
    }
    override fun getEditor(clip: TrackClip<AudioClip>) = AudioClipEditor(clip)

    override fun processBlock(
        clip: TrackClip<AudioClip>, buffers: Array<FloatArray>, position: PlayPosition,
        midiBuffer: ArrayList<Int>, noteRecorder: MidiNoteTimeRecorder
    ) {
        val c = clip.clip as? AudioClipImpl ?: return
        val clipTime = clip.time - clip.start
        c.processBlock(
            position,
            position.timeInSamples - position.convertPPQToSamples(clipTime),
            position.bufferSize, buffers
        )
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
            clip.clip.thumbnail?.let {
                Waveform(
                    it, EchoInMirror.currentPosition, startPPQ, widthPPQ,
                    clip.clip.speedRatio,
                    clip.clip.volumeEnvelope, contentColor, isDrawMinAndMax
                )
            }
            remember(clip) {
                EnvelopeEditor(clip.clip.volumeEnvelope, VOLUME_RANGE, 1F, true)
            }.Editor(startPPQ, contentColor, noteWidth, false, clipStartTime = clip.start, stroke = 0.5F, drawGradient = false)
        }
    }

    @Composable
    override fun MenuContent(clips: List<TrackClip<*>>, close: () -> Unit) {
        val trackClip = clips.firstOrNull { it.clip is AudioClip } ?: return

        @Suppress("UNCHECKED_CAST")
        EditorControls(trackClip as TrackClip<AudioClip>)
    }

    override fun split(clip: TrackClip<AudioClip>, time: Int): ClipSplitResult<AudioClip> {
        return object : ClipSplitResult<AudioClip> {
            override val clip = clip.clip.copy()
            override val start = time
            override fun revert() { }
        }
    }

    override fun save(clip: AudioClip, path: Path) { }

    override fun toString(): String {
        return "AudioClipFactoryImpl"
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

