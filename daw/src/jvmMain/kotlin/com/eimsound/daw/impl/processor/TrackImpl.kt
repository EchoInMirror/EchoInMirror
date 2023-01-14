package com.eimsound.daw.impl.processor

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.eimsound.audioprocessor.*
import com.eimsound.audioprocessor.data.midi.MidiEvent
import com.eimsound.audioprocessor.data.midi.noteOff
import com.eimsound.audioprocessor.dsp.calcPanLeftChannel
import com.eimsound.audioprocessor.dsp.calcPanRightChannel
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.api.DefaultTrackClipList
import com.eimsound.daw.api.ProjectInformation
import com.eimsound.daw.api.processor.*
import com.eimsound.daw.data.midi.*
import com.eimsound.daw.utils.LevelMeterImpl
import com.eimsound.daw.utils.randomColor
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.*
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.absolutePathString
import kotlin.io.path.pathString

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE
)
open class TrackImpl(description: AudioProcessorDescription, factory: TrackFactory<*>) :
    Track, AbstractAudioProcessor(description, factory) {
    @get:JsonProperty
    override var name by mutableStateOf("NewTrack")
    @get:JsonProperty
    @set:JsonProperty("color")
    override var color by mutableStateOf(randomColor(true))
    @get:JsonProperty
    override var pan by mutableStateOf(0F)
    @get:JsonProperty
    override var volume by mutableStateOf(1F)
    @get:JsonProperty
    @get:JsonInclude(JsonInclude.Include.NON_DEFAULT)
    override var height by mutableStateOf(0)

    override val levelMeter = LevelMeterImpl()

    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonSerialize(using = AudioProcessorCollectionIDSerializer::class)
    override val preProcessorsChain: MutableList<AudioProcessor> = mutableStateListOf()
    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonSerialize(using = AudioProcessorCollectionIDSerializer::class)
    override val postProcessorsChain: MutableList<AudioProcessor> = mutableStateListOf()
    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonSerialize(using = AudioProcessorCollectionIDSerializer::class)
    override val subTracks: MutableList<Track> = mutableStateListOf()

    private val pendingMidiBuffer = Collections.synchronizedList(ArrayList<Int>())
    private val pendingNoteOns = LongArray(128)
    private val noteRecorder = MidiNoteRecorder()
    private var lastUpdateTime = 0L

    private var _isMute by mutableStateOf(false)
    private var _isSolo by mutableStateOf(false)
    private var _isDisabled by mutableStateOf(false)
    private var tempBuffer = arrayOf(FloatArray(1024), FloatArray(1024))
    private var tempBuffer2 = arrayOf(FloatArray(1024), FloatArray(1024))
    override var isRendering: Boolean by mutableStateOf(false)
    @get:JsonProperty
    override var isMute
        get() = _isMute
        set(value) {
            if (_isMute == value) return
            _isMute = value
            stateChange()
        }
    @get:JsonProperty
    override var isSolo
        get() = _isSolo
        set(value) {
            if (_isSolo == value) return
            _isSolo = value
            stateChange()
        }
    @get:JsonProperty
    override var isDisabled
        get() = _isDisabled
        set(value) {
            if (_isDisabled == value) return
            _isDisabled = value
            stateChange()
        }

    @Suppress("LeakingThis")
    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    override val clips = DefaultTrackClipList(this)
    private var lastClipIndex = -1

    override suspend fun processBlock(
        buffers: Array<FloatArray>,
        position: CurrentPosition,
        midiBuffer: ArrayList<Int>
    ) {
        if (_isMute || _isDisabled) return
        if (pendingMidiBuffer.isNotEmpty()) {
            midiBuffer.addAll(pendingMidiBuffer)
            pendingMidiBuffer.clear()
        }
        if (position.isPlaying) {
            noteRecorder.forEachNotes {
                pendingNoteOns[it] -= position.bufferSize.toLong()
                if (pendingNoteOns[it] <= 0) {
                    noteRecorder.unmarkNote(it)
                    midiBuffer.add(noteOff(0, it).rawData)
                    midiBuffer.add(pendingNoteOns[it].toInt().coerceAtLeast(0))
                }
            }
            val startTime = position.timeInPPQ
            val blockEndSample = position.timeInSamples + position.bufferSize
            if (lastClipIndex == -1) {
                var l = 0
                var r = clips.size - 1
                while (l < r) {
                    val mid = (l + r) ushr 1
                    if (clips[mid].time > startTime) r = mid
                    else l = mid + 1
                }
                lastClipIndex = l
            }
            for (i in lastClipIndex..clips.lastIndex) {
                val clip = clips[i]
                val startTimeInSamples = position.convertPPQToSamples(clip.time)
                val endTimeInSamples = position.convertPPQToSamples(clip.time + clip.duration)
                if (endTimeInSamples < position.timeInSamples) {
                    lastClipIndex = i + 1
                    continue
                }
                if (startTimeInSamples > blockEndSample) break
                @Suppress("TYPE_MISMATCH")
                clip.clip.factory.processBlock(clip, buffers, position, midiBuffer, noteRecorder, pendingNoteOns)
            }
        }
        preProcessorsChain.forEach { it.processBlock(buffers, position, midiBuffer) }
        if (subTracks.isNotEmpty()) {
            tempBuffer[0].fill(0F)
            tempBuffer[1].fill(0F)
            runBlocking {
                val bus = EchoInMirror.bus
                subTracks.forEach {
                    if (it.isMute || it.isDisabled || it.isRendering) return@forEach
                    launch {
                        val buffer = if (it is TrackImpl) it.tempBuffer2.apply {
                            this[0].fill(0F)
                            this[1].fill(0F)
                        } else arrayOf(FloatArray(buffers[0].size), FloatArray(buffers[1].size))
                        it.processBlock(buffer, position, ArrayList(midiBuffer))
                        if (bus != null) for (i in 0 until position.bufferSize) {
                            tempBuffer[0][i] += buffer[0][i]
                            tempBuffer[1][i] += buffer[1][i]
                        }
                    }
                }
            }
            tempBuffer[0].copyInto(buffers[0])
            tempBuffer[1].copyInto(buffers[1])
        }

        postProcessorsChain.forEach { it.processBlock(buffers, position, midiBuffer) }

        var leftPeak = 0F
        var rightPeak = 0F
        val leftFactor = calcPanLeftChannel() * volume
        val rightFactor = calcPanRightChannel() * volume
        for (i in buffers[0].indices) {
            buffers[0][i] *= leftFactor
            val tmp = buffers[0][i]
            if (tmp > leftPeak) leftPeak = tmp
        }
        for (i in buffers[1].indices) {
            buffers[1][i] *= rightFactor
            val tmp = buffers[1][i]
            if (tmp > rightPeak) rightPeak = tmp
        }
        levelMeter.left = levelMeter.left.update(leftPeak)
        levelMeter.right = levelMeter.right.update(rightPeak)
        lastUpdateTime += (1000.0 * position.bufferSize / position.sampleRate).toLong()
        if (lastUpdateTime > 300) {
            levelMeter.cachedMaxLevelString = levelMeter.maxLevel.toString()
            lastUpdateTime = 0
        }
    }

    override fun prepareToPlay(sampleRate: Int, bufferSize: Int) {
        tempBuffer = arrayOf(FloatArray(bufferSize), FloatArray(bufferSize))
        tempBuffer2 = arrayOf(FloatArray(bufferSize), FloatArray(bufferSize))
        preProcessorsChain.forEach { it.prepareToPlay(sampleRate, bufferSize) }
        subTracks.forEach { it.prepareToPlay(sampleRate, bufferSize) }
        postProcessorsChain.forEach { it.prepareToPlay(sampleRate, bufferSize) }
    }

    override fun close() {
        EchoInMirror.windowManager.clearTrackUIState(this)
        preProcessorsChain.forEach { it.close() }
        preProcessorsChain.clear()
        subTracks.forEach { it.close() }
        subTracks.clear()
        postProcessorsChain.forEach { it.close() }
        postProcessorsChain.clear()
    }

    override fun playMidiEvent(midiEvent: MidiEvent, time: Int) {
        pendingMidiBuffer.add(midiEvent.rawData)
        pendingMidiBuffer.add(time)
    }

    override fun onSuddenChange() {
        stopAllNotes()
        lastClipIndex = -1
        pendingNoteOns.clone()
        noteRecorder.reset()
        clips.forEach { it.reset() }
        preProcessorsChain.forEach(AudioProcessor::onSuddenChange)
        subTracks.forEach(Track::onSuddenChange)
        postProcessorsChain.forEach(AudioProcessor::onSuddenChange)
    }

    override fun stateChange() {
        levelMeter.reset()
        subTracks.forEach(Track::stateChange)
    }

    override fun onRenderStart() {
        isRendering = true
    }

    override fun onRenderEnd() {
        isRendering = false
    }

    override suspend fun save(path: String) {
        withContext(Dispatchers.IO) {
            val dir = Paths.get(path)
            if (!Files.exists(dir)) Files.createDirectory(dir)
            val trackFile = dir.resolve("track.json").toFile()
            jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValue(trackFile, this@TrackImpl)

            if (clips.isNotEmpty()) {
                val clipsDir = dir.resolve("clips")
                if (!Files.exists(clipsDir)) Files.createDirectory(clipsDir)
                clips.forEach {
                    launch {
                        @Suppress("TYPE_MISMATCH")
                        it.clip.factory.save(it.clip, clipsDir.resolve(it.clip.id).absolutePathString())
                    }
                }
            }

            if (subTracks.isNotEmpty()) {
                val tracksDir = dir.resolve("tracks")
                if (!Files.exists(tracksDir)) Files.createDirectory(tracksDir)
                subTracks.forEach { launch { it.save(tracksDir.resolve(it.id).absolutePathString()) } }
            }

            if (preProcessorsChain.isNotEmpty() || postProcessorsChain.isNotEmpty()) {
                val processorsDir = dir.resolve("processors")
                if (!Files.exists(processorsDir)) Files.createDirectory(processorsDir)
                preProcessorsChain.forEach { launch { it.save(processorsDir.resolve(it.id).absolutePathString()) } }
                postProcessorsChain.forEach { launch { it.save(processorsDir.resolve(it.id).absolutePathString()) } }
            }
        }
    }

    override suspend fun load(path: String, json: JsonNode) {
        withContext(Dispatchers.IO) {
            try {
                val dir = Paths.get(path)
                val reader = jacksonObjectMapper().readerForUpdating(this@TrackImpl)
                reader.readValue<TrackImpl>(json)

                val clipsDir = dir.resolve("clips").absolutePathString()
                clips.addAll(json.get("clips").map {
                    async { EchoInMirror.clipManager.createTrackClip(clipsDir, it) }
                }.awaitAll())

                val tracksDir = dir.resolve("tracks")
                subTracks.addAll(json.get("subTracks").map {
                    async {
                        val trackID = it.asText()
                        val trackPath = tracksDir.resolve(trackID)
                        EchoInMirror.trackManager.createTrack(trackPath.absolutePathString(), trackID)
                    }
                }.awaitAll())
                val processorsDir = dir.resolve("processors").absolutePathString()
                preProcessorsChain.addAll(json.get("preProcessorsChain").map {
                    async { EchoInMirror.audioProcessorManager.createAudioProcessor(processorsDir, it.asText()) }
                }.awaitAll())
                postProcessorsChain.addAll(json.get("postProcessorsChain").map {
                    async { EchoInMirror.audioProcessorManager.createAudioProcessor(processorsDir, it.asText()) }
                }.awaitAll())
            } catch (_: FileNotFoundException) { }
        }
    }

    override fun toString(): String {
        return "TrackImpl(name='$name', preProcessorsChain=${preProcessorsChain.size}, " +
        "postProcessorsChain=${postProcessorsChain.size}, subTracks=${subTracks.size}, clips=${clips.size})"
    }
}

class BusImpl(
    override val project: ProjectInformation,
    description: AudioProcessorDescription, factory: TrackFactory<Track>
) : TrackImpl(description, factory), Bus {
    override var name = "Bus"
    override var lastSaveTime by mutableStateOf(System.currentTimeMillis())
    @get:JsonProperty
    override var channelType by mutableStateOf(ChannelType.STEREO)
    override var color
        get() = Color.Transparent
        set(_) { }

    override suspend fun save() {
        val time = System.currentTimeMillis()
        project.timeCost += (time - lastSaveTime).toInt()
        lastSaveTime = time
        project.save()
        save(project.root.pathString)
    }

    override suspend fun processBlock(
        buffers: Array<FloatArray>,
        position: CurrentPosition,
        midiBuffer: ArrayList<Int>
    ) {
        super.processBlock(buffers, position, midiBuffer)

        when (channelType) {
            ChannelType.LEFT -> buffers[0].copyInto(buffers[1])
            ChannelType.RIGHT -> buffers[1].copyInto(buffers[0])
            ChannelType.MONO -> {
                for (i in 0 until position.bufferSize) {
                    buffers[0][i] = (buffers[0][i] + buffers[1][i]) / 2
                    buffers[1][i] = buffers[0][i]
                }
            }
            ChannelType.SIDE -> {
                for (i in 0 until position.bufferSize) {
                    val mid = (buffers[0][i] + buffers[1][i]) / 2
                    buffers[0][i] -= mid
                    buffers[1][i] -= mid
                }
            }
            else -> {}
        }
    }
}
