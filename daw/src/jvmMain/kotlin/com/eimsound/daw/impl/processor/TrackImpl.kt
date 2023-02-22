package com.eimsound.daw.impl.processor

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastForEach
import com.eimsound.audioprocessor.*
import com.eimsound.audioprocessor.data.midi.MidiEvent
import com.eimsound.audioprocessor.data.midi.MidiNoteRecorder
import com.eimsound.audioprocessor.data.midi.noteOff
import com.eimsound.audioprocessor.dsp.calcPanLeftChannel
import com.eimsound.audioprocessor.dsp.calcPanRightChannel
import com.eimsound.daw.Configuration
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.api.ClipManager
import com.eimsound.daw.api.DefaultTrackClipList
import com.eimsound.daw.api.ProjectInformation
import com.eimsound.daw.api.processor.*
import com.eimsound.daw.components.utils.randomColor
import com.eimsound.daw.utils.*
import com.eimsound.daw.window.panels.fileBrowserPreviewer
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.absolutePathString
import kotlin.io.path.pathString

open class TrackImpl(description: AudioProcessorDescription, factory: TrackFactory<*>) :
    Track, AbstractAudioProcessor(description, factory) {
    override var name by mutableStateOf("NewTrack")
    override var color by mutableStateOf(randomColor(true))
    override var pan by mutableStateOf(0F)
    override var volume by mutableStateOf(1F)
    override var height by mutableStateOf(0)

    override val levelMeter = LevelMeterImpl()

    override val internalProcessorsChain = ArrayList<AudioProcessor>()
    override val preProcessorsChain: MutableList<AudioProcessor> = mutableStateListOf()
    override val postProcessorsChain: MutableList<AudioProcessor> = mutableStateListOf()
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
    override var isMute
        get() = _isMute
        set(value) {
            if (_isMute == value) return
            _isMute = value
            stateChange()
        }
    override var isSolo
        get() = _isSolo
        set(value) {
            if (_isSolo == value) return
            _isSolo = value
            stateChange()
        }
    override var isDisabled
        get() = _isDisabled
        set(value) {
            if (_isDisabled == value) return
            _isDisabled = value
            stateChange()
        }

    @Suppress("LeakingThis")
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
            if (lastClipIndex == -1) lastClipIndex = clips.binarySearch { it.time < startTime }
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
        preProcessorsChain.fastForEach { it.processBlock(buffers, position, midiBuffer) }
        if (subTracks.isNotEmpty()) {
            tempBuffer[0].fill(0F)
            tempBuffer[1].fill(0F)
            runBlocking {
                val bus = EchoInMirror.bus
                subTracks.fastForEach {
                    if (it.isMute || it.isDisabled || it.isRendering) return@fastForEach
                    launch {
                        val buffer = if (it is TrackImpl) it.tempBuffer2.apply {
                            this[0].fill(0F)
                            this[1].fill(0F)
                        } else arrayOf(FloatArray(buffers[0].size), FloatArray(buffers[1].size))
                        buffers[0].copyInto(buffer[0])
                        buffers[1].copyInto(buffer[1])
                        it.processBlock(buffer, position, ArrayList(midiBuffer))
                        if (bus != null) tempBuffer.mixWith(buffer)
                    }
                }
            }
            tempBuffer[0].copyInto(buffers[0])
            tempBuffer[1].copyInto(buffers[1])
        }

        if (position.isRealtime) internalProcessorsChain.fastForEach { it.processBlock(buffers, position, midiBuffer) }
        postProcessorsChain.fastForEach { it.processBlock(buffers, position, midiBuffer) }

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
        preProcessorsChain.fastForEach { it.prepareToPlay(sampleRate, bufferSize) }
        subTracks.fastForEach { it.prepareToPlay(sampleRate, bufferSize) }
        postProcessorsChain.fastForEach { it.prepareToPlay(sampleRate, bufferSize) }
        internalProcessorsChain.fastForEach { it.prepareToPlay(sampleRate, bufferSize) }
    }

    override fun close() {
        if (EchoInMirror.selectedTrack == this) EchoInMirror.selectedTrack = null
        preProcessorsChain.fastForEach { it.close() }
        preProcessorsChain.clear()
        subTracks.fastForEach { it.close() }
        subTracks.clear()
        postProcessorsChain.fastForEach { it.close() }
        postProcessorsChain.clear()
        internalProcessorsChain.fastForEach { it.close() }
        internalProcessorsChain.clear()
        clips.fastForEach { (it.clip as? AutoCloseable)?.close() }
        clips.clear()
    }

    override fun playMidiEvent(midiEvent: MidiEvent, time: Int) {
        pendingMidiBuffer.add(midiEvent.rawData)
        pendingMidiBuffer.add(time)
    }

    override fun onSuddenChange() {
        stopAllNotes()
        lastClipIndex = -1
        pendingNoteOns.fill(0L)
        noteRecorder.reset()
        clips.fastForEach { it.reset() }
        preProcessorsChain.fastForEach(AudioProcessor::onSuddenChange)
        subTracks.fastForEach(Track::onSuddenChange)
        postProcessorsChain.fastForEach(AudioProcessor::onSuddenChange)
        internalProcessorsChain.fastForEach(AudioProcessor::onSuddenChange)
    }

    override fun stateChange() {
        levelMeter.reset()
        subTracks.fastForEach(Track::stateChange)
    }

    override fun onRenderStart() {
        isRendering = true
    }

    override fun onRenderEnd() {
        isRendering = false
    }

    override fun toJson() = hashMapOf(
        "factory" to factory.name,
        "id" to id,
        "name" to name,
        "color" to color,
        "pan" to pan,
        "volume" to volume,
        "height" to height,
        "isMute" to isMute,
        "isSolo" to isSolo,
        "isDisabled" to isDisabled,
        "subTracks" to subTracks.map { it.id },
        "preProcessorsChain" to preProcessorsChain.map { it.id },
        "postProcessorsChain" to postProcessorsChain.map { it.id },
        "clips" to clips,
    )

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun save(path: String) {
        withContext(Dispatchers.IO) {
            val dir = Paths.get(path)
            if (!Files.exists(dir)) Files.createDirectory(dir)
            val trackFile = dir.resolve("track.json").toFile()
            JsonPrettier.encodeToStream(toJson(), trackFile.outputStream())

            if (clips.isNotEmpty()) {
                val clipsDir = dir.resolve("clips")
                if (!Files.exists(clipsDir)) Files.createDirectory(clipsDir)
                clips.fastForEach {
                    launch {
                        @Suppress("TYPE_MISMATCH")
                        it.clip.factory.save(it.clip, clipsDir.resolve(it.clip.id).absolutePathString())
                    }
                }
            }

            if (subTracks.isNotEmpty()) {
                val tracksDir = dir.resolve("tracks")
                if (!Files.exists(tracksDir)) Files.createDirectory(tracksDir)
                subTracks.fastForEach { launch { it.save(tracksDir.resolve(it.id).absolutePathString()) } }
            }

            if (preProcessorsChain.isNotEmpty() || postProcessorsChain.isNotEmpty()) {
                val processorsDir = dir.resolve("processors")
                if (!Files.exists(processorsDir)) Files.createDirectory(processorsDir)
                preProcessorsChain.fastForEach { launch { it.save(processorsDir.resolve(it.id).absolutePathString()) } }
                postProcessorsChain.fastForEach { launch { it.save(processorsDir.resolve(it.id).absolutePathString()) } }
            }
        }
    }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        id = json["id"].asString()
        json["name"]?.asString()?.let { name = it }
        json["color"]?.jsonPrimitive?.long?.let { color = Color(it) }
        json["pan"]?.jsonPrimitive?.float?.let { pan = it }
        json["volume"]?.jsonPrimitive?.float?.let { volume = it }
        json["height"]?.jsonPrimitive?.int?.let { height = it }
        json["isMute"]?.jsonPrimitive?.boolean?.let { isMute = it }
        json["isSolo"]?.jsonPrimitive?.boolean?.let { isSolo = it }
        json["isDisabled"]?.jsonPrimitive?.boolean?.let { isDisabled = it }
    }

    override suspend fun load(path: String, json: JsonObject) {
        withContext(Dispatchers.IO) {
            try {
                val dir = Paths.get(path)
                id = json["id"].asString()
                name = json["name"].asString()
                color = Color(json["color"]!!.jsonPrimitive.long)

                val clipsDir = dir.resolve("clips").absolutePathString()
                clips.addAll(json["clips"]!!.jsonArray.map {
                    async { ClipManager.instance.createTrackClip(clipsDir, it as JsonObject) }
                }.awaitAll())

                val tracksDir = dir.resolve("tracks")
                subTracks.addAll(json["subTracks"]!!.jsonArray.map {
                    async {
                        val trackID = it.asString()
                        val trackPath = tracksDir.resolve(trackID)
                        TrackManager.instance.createTrack(trackPath.absolutePathString(), trackID)
                    }
                }.awaitAll())
                val processorsDir = dir.resolve("processors").absolutePathString()
                preProcessorsChain.addAll(json["preProcessorsChain"]!!.jsonArray.map {
                    async { AudioProcessorManager.instance.createAudioProcessor(processorsDir, it.asString()) }
                }.awaitAll())
                postProcessorsChain.addAll(json["postProcessorsChain"]!!.jsonArray.map {
                    async { AudioProcessorManager.instance.createAudioProcessor(processorsDir, it.asString()) }
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

    override var channelType by mutableStateOf(ChannelType.STEREO)
    override var color
        get() = Color.Transparent
        set(_) { }

    init {
        internalProcessorsChain.add(fileBrowserPreviewer)
    }

    override fun toJson() = super.toJson().apply {
        put("channelType", channelType.ordinal)
    }

    override suspend fun save() {
        val time = System.currentTimeMillis()
        project.timeCost += (time - lastSaveTime).toInt()
        lastSaveTime = time
        project.save()
        save(project.root.pathString)
    }

    override suspend fun load(path: String, json: JsonObject) {
        super.load(path, json)
        channelType = ChannelType.values()[json["channelType"].asInt()]
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

        if (position.isRealtime && Configuration.autoCutOver0db) {
            repeat(buffers.size) { ch ->
                repeat(buffers[ch].size) {
                    if (buffers[ch][it] > 1f) buffers[ch][it] = 1f
                    else if (buffers[ch][it] < -1f) buffers[ch][it] = -1f
                }
            }
        }
    }
}
