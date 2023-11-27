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
import com.eimsound.dsp.data.midi.MidiNoteRecorder
import com.eimsound.daw.api.*
import com.eimsound.daw.api.processor.Track
import com.eimsound.audiosources.*
import com.eimsound.daw.components.*
import com.eimsound.dsp.data.*
import com.eimsound.dsp.detectBPM
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.math.roundToInt

class AudioClipImpl(
    factory: ClipFactory<AudioClip>, target: AudioSource? = null
): AbstractClip<AudioClip>(factory), AudioClip {
    private var _target: AudioSource? = null
    override var target: AudioSource
        get() = _target ?: throw IllegalStateException("Target is not set")
        set(value) {
            if (_target == value) return
            close()
            _target = value
            resampledAudioSource = AudioSourceManager.instance.createResampledSource(value).apply {
                factor = EchoInMirror.currentPosition.sampleRate.toDouble() / value.sampleRate
            }
            _thumbnail = AudioThumbnail(audioSource)
        }
    private var resampledAudioSource: ResampledAudioSource? = null
    override val audioSource get() = resampledAudioSource ?: throw IllegalStateException("Audio source is not set")
    override val defaultDuration get() = EchoInMirror.currentPosition.convertSamplesToPPQ(audioSource.length)
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

    override fun close() { _target?.close() }

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
        json["volumeEnvelope"]?.let { volumeEnvelope.fromJson(it) }
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
        midiBuffer: ArrayList<Int>, noteRecorder: MidiNoteRecorder, pendingNoteOns: LongArray
    ) {
        clip.clip.audioSource.factor = position.sampleRate.toDouble() / clip.clip.audioSource.source!!.sampleRate
        val clipTime = clip.time - clip.start
        clip.clip.audioSource.getSamples(
            position.timeInSamples - position.convertPPQToSamples(clipTime),
            buffers[0].size,
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
    override fun PlaylistContent(
        clip: TrackClip<AudioClip>, track: Track, contentColor: Color,
        noteWidth: MutableState<Dp>, startPPQ: Float, widthPPQ: Float
    ) {
        val isDrawMinAndMax = noteWidth.value.value < LocalDensity.current.density
        Box {
            Waveform(clip.clip.thumbnail, EchoInMirror.currentPosition, startPPQ, widthPPQ, clip.clip.volumeEnvelope, contentColor, isDrawMinAndMax)
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
                val bpm = detectBPM(clip.audioSource.copy())
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
        val oldEnvelope = clip.clip.volumeEnvelope.copy()
        return object : ClipSplitResult<AudioClip> {
            override val clip = copy(clip.clip) as AudioClip
            override val start = time
            override fun revert() {
                clip.clip.volumeEnvelope.clear()
                clip.clip.volumeEnvelope.addAll(oldEnvelope)
                clip.clip.volumeEnvelope.update()
            }
        }
    }

    override fun save(clip: AudioClip, path: Path) { }

    override fun toString(): String {
        return "AudioClipFactoryImpl"
    }

    override fun copy(clip: AudioClip) = createClip(clip.target.copy()).apply {
        volumeEnvelope.addAll(clip.volumeEnvelope.copy())
    }

    override fun merge(clip: TrackClip<AudioClip>, other: TrackClip<AudioClip>) {
        clip.duration += other.duration
    }
}

class AudioFileExtensionHandler : AbstractFileExtensionHandler() {
    override val icon = Icons.Outlined.MusicNote
    // wav, mp3, ogg, flac, aiff, aif
    override val extensions = Regex("\\.(wav|mp3|ogg|flac|aiff|aif)$", RegexOption.IGNORE_CASE)

    override suspend fun createClip(file: Path, data: Any?) = ClipManager.instance.defaultAudioClipFactory.createClip(file)
}

