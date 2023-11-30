package com.eimsound.daw.impl.processor

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.eimsound.audioprocessor.*
import com.eimsound.dsp.data.PAN_RANGE
import com.eimsound.dsp.data.VOLUME_RANGE
import com.eimsound.dsp.data.midi.MidiEvent
import com.eimsound.dsp.data.midi.MidiNoteRecorder
import com.eimsound.dsp.data.midi.noteOff
import com.eimsound.audioprocessor.interfaces.calcPanLeftChannel
import com.eimsound.audioprocessor.interfaces.calcPanRightChannel
import com.eimsound.daw.api.*
import com.eimsound.daw.api.processor.*
import com.eimsound.daw.commons.json.*
import com.eimsound.daw.components.utils.randomColor
import com.eimsound.daw.dawutils.buffer.TrackBuffers
import com.eimsound.daw.utils.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.collections.ArrayList
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

private val trackLogger = KotlinLogging.logger("TrackImpl")
open class TrackImpl(description: AudioProcessorDescription, factory: TrackFactory<*>) :
    Track, AbstractAudioProcessor(description, factory) {
    override val storeFileName = "track.json"
    override var name by mutableStateOf("NewTrack")
    override var color by mutableStateOf(randomColor(true))
    override var height by mutableStateOf(0)
    override var collapsed by mutableStateOf(false)

    override val levelMeter = LevelMeterImpl()

    override val internalProcessorsChain = ArrayList<AudioProcessor>()
    override val preProcessorsChain: MutableList<TrackAudioProcessorWrapper> = mutableStateListOf()
    override val postProcessorsChain: MutableList<TrackAudioProcessorWrapper> = mutableStateListOf()
    override val subTracks: MutableList<Track> = mutableStateListOf()

    private val pendingMidiBuffer = Collections.synchronizedList(ArrayList<Int>())
    private val pendingNoteOns = LongArray(128)
    private val noteRecorder = MidiNoteRecorder()
    private var lastUpdateTime = 0L

    private val trackBuffers = TrackBuffers()
    override var isRendering by mutableStateOf(false)
    override var isDisabled by observableMutableStateOf(false, ::stateChange)
    override var isSolo by observableMutableStateOf(false, ::stateChange)

    private val panParameter = audioProcessorParameterOf("pan", "声相", PAN_RANGE, 0F)
    private val volumeParameter = audioProcessorParameterOf("volume", "电平", VOLUME_RANGE, 1F)
    private var asyncJobs: Array<Job?> = emptyArray()
    override var pan by panParameter
    override var volume by volumeParameter
    override val parameters = listOf(panParameter, volumeParameter)

    @Suppress("LeakingThis")
    override val clips = DefaultTrackClipList(this)
    private var lastClipIndex = -1

    override suspend fun processBlock(
        buffers: Array<FloatArray>,
        position: CurrentPosition,
        midiBuffer: ArrayList<Int>
    ) = withContext(Dispatchers.Default) {
        if (isDisabled) return@withContext
        if (pendingMidiBuffer.isNotEmpty()) {
            midiBuffer.addAll(pendingMidiBuffer)
            pendingMidiBuffer.clear()
        }
        if (position.isPlaying) {
            val bufferSize = position.bufferSize.toLong()
            noteRecorder.forEachNotes {
                pendingNoteOns[it] -= bufferSize
                if (pendingNoteOns[it] <= 0) {
                    noteRecorder.unmarkNote(it)
                    midiBuffer.add(noteOff(0, it).rawData)
                    midiBuffer.add(pendingNoteOns[it].toInt().coerceAtLeast(0))
                }
            }
            val startTime = position.timeInPPQ
            val blockEndSample = position.timeInSamples + bufferSize
            if (lastClipIndex == -1) lastClipIndex = clips.lowerBound { it.time < startTime }
            if (lastClipIndex > 0) lastClipIndex--
            for (i in lastClipIndex..clips.lastIndex) {
                val clip = clips[i]
                val startTimeInSamples = position.convertPPQToSamples(clip.time)
                val endTimeInSamples = position.convertPPQToSamples(clip.time + clip.duration)
                if (endTimeInSamples < position.timeInSamples) {
                    lastClipIndex = i + 1
                    continue
                }
                if (clip.isDisabled) continue
                if (startTimeInSamples > blockEndSample) break
                @Suppress("TYPE_MISMATCH")
                clip.clip.factory.processBlock(clip, buffers, position, midiBuffer, noteRecorder, pendingNoteOns)
            }
        }
        var tempLatency = 0
        preProcessorsChain.fastForEach {
            if (!it.processor.isDisabled) {
                it.processor.processBlock(buffers, position, midiBuffer)
                tempLatency += it.processor.latency
            }
        }
        tempLatency += processSubTracks(position, buffers, midiBuffer)

        if (position.isRealtime) {
            internalProcessorsChain.fastForEach {
                it.processBlock(buffers, position, midiBuffer)
                tempLatency += it.latency
            }
        }
        postProcessorsChain.fastForEach {
            if (!it.processor.isDisabled) {
                it.processor.processBlock(buffers, position, midiBuffer)
                tempLatency += it.processor.latency
            }
        }

        if (latency != tempLatency) latency = tempLatency

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

    private suspend fun processSubTracks(
        position: CurrentPosition, buffers: Array<FloatArray>, midiBuffer: ArrayList<Int>
    ) = withContext(Dispatchers.Default) {
        if (subTracks.isEmpty()) return@withContext 0
        trackBuffers.checkSize(subTracks.size, 2, position.bufferSize)

        if (asyncJobs.size != subTracks.size) asyncJobs = arrayOfNulls(subTracks.size)
        repeat(subTracks.size) { index ->
            val it = subTracks[index]
            asyncJobs[index] = if (it.isDisabled || it.isRendering) null
            else launch {
                val trackBuffer = trackBuffers[index]
                trackBuffer.putBuffers(buffers, 2, buffers[0].size)
                it.processBlock(trackBuffer.buffers, position, ArrayList(midiBuffer))
            }
        }

        var maxLatency = 0
        repeat(subTracks.size) { index ->
            asyncJobs[index]?.apply {
                join()
                subTracks[index].latency.let { l -> if (l > maxLatency) maxLatency = l }
            }
        }

        buffers[0].fill(0F)
        buffers[1].fill(0F)

        repeat(subTracks.size) { index ->
            if (asyncJobs[index] == null) return@repeat
//                buffers.mixWith(trackBuffers[index].buffers, 1F)
            trackBuffers[index].popAndMixBuffersTo(buffers, (maxLatency - subTracks[index].latency).coerceAtLeast(0))
        }

        buffers[0].copyInto(buffers[0])
        buffers[1].copyInto(buffers[1])

        maxLatency
    }

    override suspend fun prepareToPlay(sampleRate: Int, bufferSize: Int) {
        preProcessorsChain.fastForEach { it.processor.prepareToPlay(sampleRate, bufferSize) }
        subTracks.fastForEach { it.prepareToPlay(sampleRate, bufferSize) }
        postProcessorsChain.fastForEach { it.processor.prepareToPlay(sampleRate, bufferSize) }
        internalProcessorsChain.fastForEach { it.prepareToPlay(sampleRate, bufferSize) }
    }

    override fun close() {
        if (EchoInMirror.selectedTrack == this) EchoInMirror.selectedTrack = null
        preProcessorsChain.fastForEach { it.close() }
        subTracks.fastForEach(AutoCloseable::close)
        postProcessorsChain.fastForEach { it.close() }
        internalProcessorsChain.fastForEach(AutoCloseable::close)
        clips.fastForEach { (it.clip as? AutoCloseable)?.close() }
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
        preProcessorsChain.fastForEach { it.processor.onSuddenChange() }
        subTracks.fastForEach(Track::onSuddenChange)
        postProcessorsChain.fastForEach { it.processor.onSuddenChange() }
        internalProcessorsChain.fastForEach(AudioProcessor::onSuddenChange)
    }

//    override fun stateChange() {
//        levelMeter.reset()
//        subTracks.fastForEach(Track::stateChange)
//    }

    @Suppress("UNUSED_PARAMETER")
    private fun stateChange(unused: Boolean) { levelMeter.reset() }

    override fun onRenderStart() {
        isRendering = true
    }

    override fun onRenderEnd() {
        isRendering = false
    }

    protected fun JsonObjectBuilder.buildJson() {
        put("factory", factory.name)
        put("id", id)
        put("uuid", uuid.toString())
        putNotDefault("name", name)
        put("color", color.value.toLong())
        putNotDefault("pan", pan)
        putNotDefault("volume", volume)
        putNotDefault("height", height)
        putNotDefault("collapsed", collapsed)
        putNotDefault("isDisabled", isDisabled)
        putNotDefault("isSolo", isSolo)
        putNotDefault("subTracks", subTracks.fastMap { it.id })
        putNotDefault("preProcessorsChain", preProcessorsChain.fastMap { it.processor.id })
        putNotDefault("postProcessorsChain", postProcessorsChain.fastMap { it.processor.id })
        putNotDefault("clips", clips)
    }
    override fun toJson() = buildJsonObject { buildJson() }

    override suspend fun store(path: Path) {
        withContext(Dispatchers.IO) {
            if (!Files.exists(path)) Files.createDirectory(path)
            super.store(path)

            if (clips.isNotEmpty()) {
                val clipsDir = path.resolve("clips")
                if (!Files.exists(clipsDir)) Files.createDirectory(clipsDir)
                clips.fastMap {
                    launch {
                        @Suppress("TYPE_MISMATCH")
                        it.clip.factory.save(it.clip, clipsDir.resolve(it.clip.id))
                    }
                }.joinAll()
            }

            if (subTracks.isNotEmpty()) {
                val tracksDir = path.resolve("tracks")
                if (!Files.exists(tracksDir)) Files.createDirectory(tracksDir)
                subTracks.fastMap { launch { it.store(tracksDir.resolve(it.id)) } }.joinAll()
            }

            if (preProcessorsChain.isNotEmpty() || postProcessorsChain.isNotEmpty()) {
                val processorsDir = path.resolve("processors")
                storeProcessors(preProcessorsChain, processorsDir)
                storeProcessors(postProcessorsChain, processorsDir)
            }

            clean(path)
        }
    }

    private suspend fun storeProcessors(processors: List<TrackAudioProcessorWrapper>, dir: Path) {
        withContext(Dispatchers.IO) {
            processors.fastMap {
                launch {
                    val processDir = dir.resolve(it.processor.id)
                    Files.createDirectories(processDir)
                    it.store(processDir)
                }
            }.joinAll()
        }
    }

    private suspend fun clean(path: Path) {
        val processorsDir = path.resolve("processors")
        val processors = hashSetOf<String>()
        preProcessorsChain.fastForEach { processors.add(it.processor.id) }
        postProcessorsChain.fastForEach { processors.add(it.processor.id) }
        if (Files.exists(processorsDir)) {
            cleanAudioProcessors(processorsDir, processors)
            cleanAudioProcessors(processorsDir, processors)
        }
        val tracksDir = path.resolve("tracks")
        if (Files.exists(tracksDir)) cleanAudioProcessors(tracksDir, subTracks.mapTo(HashSet()) { it.id })
    }

    @OptIn(ExperimentalPathApi::class)
    private suspend fun cleanAudioProcessors(dir: Path, names: Set<String>) {
        withContext(Dispatchers.IO) {
            try {
                launch {
                    Files.list(dir).forEach {
                        if (it.fileName.toString() !in names) {
                            tryOrNull(trackLogger, "Failed to delete audio processor: $it") {
                                it.deleteRecursively()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                trackLogger.error(e) { "Failed to clean track: $id" }
            }
        }
    }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        id = json["id"]!!.asString()
        uuid = UUID.fromString(json["uuid"]!!.asString())
        json["name"]?.asString()?.let { name = it }
        json["color"]?.asLong()?.let { color = Color(it.toULong()) }
        json["height"]?.asInt()?.let { height = it }
        json["collapsed"]?.asBoolean()?.let { collapsed = it }
        json["isDisabled"]?.asBoolean()?.let { isDisabled = it }
        json["isSolo"]?.asBoolean()?.let { isSolo = it }
        json["volume"]?.asFloat()?.let { volume = it }
    }

    override suspend fun restore(path: Path) {
        withContext(Dispatchers.Default) {
            try {
                val json = Json.parseToJsonElement(path.resolve("track.json").toFile().readText()).jsonObject
                fromJson(json)

                json["clips"]?.let { clipIds ->
                    val clipsDir = path.resolve("clips")
                    clips.addAll(clipIds.jsonArray.fastMap {
                        async {
                            tryOrNull(trackLogger, "Failed to load clip: $it") {
                                ClipManager.instance.createTrackClip(clipsDir, it as JsonObject)
                            }
                        }
                    }.awaitAll().filterNotNull())
                }

                json["subTracks"]?.let { tracks ->
                    val tracksDir = path.resolve("tracks")
                    subTracks.addAll(tracks.jsonArray.fastMap {
                        async {
                            tryOrNull(trackLogger, "Failed to load track: $it") {
                                TrackManager.instance.createTrack(tracksDir.resolve(it.asString()))
                            }
                        }
                    }.awaitAll().filterNotNull())
                }
                val processorsDir = path.resolve("processors")
                loadAudioProcessors(preProcessorsChain, json["preProcessorsChain"]?.jsonArray, processorsDir)
                loadAudioProcessors(postProcessorsChain, json["postProcessorsChain"]?.jsonArray, processorsDir)
            } catch (_: FileNotFoundException) { }
        }
    }

    private suspend fun loadAudioProcessors(list: MutableList<TrackAudioProcessorWrapper>, json: JsonArray?, processorsDir: Path) {
        if (json == null) return
        withContext(Dispatchers.IO) {
            list.addAll(json.jsonArray.fastMap {
                val id = it.asString()
                async {
                    tryOrNull(trackLogger, "Failed to load audio processor: $id") {
                        val dir = processorsDir.resolve(id)
                        DefaultTrackAudioProcessorWrapper(
                            AudioProcessorManager.instance.createAudioProcessor(dir)
                        ).apply {
                            restore(dir)
                        }
                    }
                }
            }.awaitAll().filterNotNull())
        }
    }

    override fun toString(): String {
        return "TrackImpl(name='$name', preProcessorsChain=${preProcessorsChain.size}, " +
                "postProcessorsChain=${postProcessorsChain.size}, subTracks=${subTracks.size}, clips=${clips.size}, height=$height)"
    }
}
