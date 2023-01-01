package cn.apisium.eim.impl.processor

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.api.ProjectInformation
import cn.apisium.eim.api.convertPPQToSamples
import cn.apisium.eim.api.processor.*
import cn.apisium.eim.api.processor.dsp.calcPanLeftChannel
import cn.apisium.eim.api.processor.dsp.calcPanRightChannel
import cn.apisium.eim.data.midi.*
import cn.apisium.eim.utils.randomColor
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
    override var name by mutableStateOf("")
    @get:JsonProperty
    @set:JsonProperty("color")
    override var color by mutableStateOf(randomColor(true))
    @get:JsonProperty
    override var pan by mutableStateOf(0F)
    @get:JsonProperty
    override var volume by mutableStateOf(1F)

    override val levelMeter = LevelMeterImpl()
    override val notes = NoteMessageListImpl()

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
    private var currentPlayedIndex = 0
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
            val blockEndSample = position.timeInSamples + position.bufferSize
            noteRecorder.forEachNotes {
                pendingNoteOns[it] -= position.bufferSize.toLong()
                if (pendingNoteOns[it] <= 0) {
                    noteRecorder.unmarkNote(it)
                    midiBuffer.add(noteOff(0, it).rawData)
                    midiBuffer.add(pendingNoteOns[it].toInt().coerceAtLeast(0))
                }
            }
            for (i in currentPlayedIndex until notes.size) {
                val note = notes[i]
                val startTimeInSamples = position.convertPPQToSamples(note.time)
                val endTimeInSamples = position.convertPPQToSamples(note.time + note.duration)
                if (startTimeInSamples < position.timeInSamples) continue
                if (startTimeInSamples > blockEndSample) break
                currentPlayedIndex = i + 1
                val noteOnTime = (startTimeInSamples - position.timeInSamples).toInt().coerceAtLeast(0)
                if (noteRecorder.isMarked(note.note)) {
                    noteRecorder.unmarkNote(note.note)
                    midiBuffer.add(note.toNoteOffRawData())
                    midiBuffer.add(noteOnTime)
                }
                midiBuffer.add(note.toNoteOnRawData())
                midiBuffer.add(noteOnTime)
                val endTime = endTimeInSamples - position.timeInSamples
                if (endTimeInSamples > blockEndSample) {
                    pendingNoteOns[note.note] = endTime
                    noteRecorder.markNote(note.note)
                } else {
                    midiBuffer.add(note.toNoteOffRawData())
                    midiBuffer.add((endTimeInSamples - position.timeInSamples).toInt().coerceAtLeast(0))
                }
            }
        }
        preProcessorsChain.forEach { it.processBlock(buffers, position, midiBuffer) }
        if (subTracks.size == 1) {
            val track = subTracks.first()
            if (!track.isMute && !track.isDisabled) track.processBlock(buffers, position, ArrayList(midiBuffer))
        } else if (subTracks.isNotEmpty()) {
            tempBuffer[0].fill(0F)
            tempBuffer[1].fill(0F)
            runBlocking {
                subTracks.forEach {
                    if (it.isMute || it.isDisabled || it.isRendering) return@forEach
                    launch {
                        val buffer = if (it is TrackImpl) it.tempBuffer2.apply {
                            buffers[0].copyInto(this[0])
                            buffers[1].copyInto(this[1])
                        } else arrayOf(buffers[0].clone(), buffers[1].clone())
                        it.processBlock(buffer, position, ArrayList(midiBuffer))
                        for (i in 0 until position.bufferSize) {
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
        currentPlayedIndex = 0
        stopAllNotes()
        pendingNoteOns.clone()
        noteRecorder.reset()
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
                val tracksDir = dir.resolve("tracks")
                json.get("subTracks").forEach {
                    launch {
                        val trackID = it.asText()
                        val trackPath = tracksDir.resolve(trackID)
                        subTracks.add(EchoInMirror.audioProcessorManager.createTrack(trackPath.absolutePathString(), trackID))
                    }
                }
                val processorsDir = dir.resolve("processors").absolutePathString()
                json.get("preProcessorsChain").forEach {
                    launch {
                        preProcessorsChain.add(EchoInMirror.audioProcessorManager
                            .createAudioProcessor(processorsDir, it.asText()))
                    }
                }
                json.get("postProcessorsChain").forEach {
                    launch {
                        postProcessorsChain.add(EchoInMirror.audioProcessorManager
                            .createAudioProcessor(processorsDir, it.asText()))
                    }
                }
            } catch (_: FileNotFoundException) { }
        }
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
