package com.eimsound.daw.impl.processor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.eimsound.audioprocessor.AudioProcessorDescription
import com.eimsound.audioprocessor.PlayPosition
import com.eimsound.daw.Configuration
import com.eimsound.daw.api.ProjectInformation
import com.eimsound.daw.api.processor.*
import com.eimsound.daw.commons.json.asInt
import com.eimsound.daw.commons.json.putNotDefault
import com.eimsound.daw.utils.tryOrNull
import com.eimsound.daw.window.panels.fileBrowserPreviewer
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.pathString
import kotlin.system.measureTimeMillis

private fun findProcessor(track: Track, uuid: UUID): TrackAudioProcessorWrapper? {
    var p = track.preProcessorsChain.find { it.processor.uuid == uuid }
        ?: track.postProcessorsChain.find { it.processor.uuid == uuid }
    if (p != null) return p

    for (sub in track.subTracks) {
        p = findProcessor(sub, uuid)
        if (p != null) return p
    }

    return null
}

private val backupFileTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

private val busLogger = KotlinLogging.logger("BusImpl")
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

    private var loaded = false
    private val loadedCallbacks = mutableListOf<() -> Unit>()

    @OptIn(DelicateCoroutinesApi::class)
    private val autoSaveJob = GlobalScope.launch {
        while (true) {
            delay(10 * 60 * 1000L) // 10 minutes
            save()
        }
    }

    init {
        internalProcessorsChain.add(fileBrowserPreviewer)
    }

    override fun toJson() = buildJsonObject {
        buildJson()
        putNotDefault("channelType", channelType.ordinal)
    }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        super.fromJson(json)
        json["channelType"]?.asInt()?.let { channelType = ChannelType.entries[it] }
    }

    @OptIn(ExperimentalPathApi::class)
    private suspend fun backup() {
        withContext(Dispatchers.IO) {
            val backupDir = project.root.resolve("backup")
            if (!Files.exists(backupDir)) Files.createDirectory(backupDir)

            Files.list(backupDir).sorted(Comparator.reverseOrder()).skip(50).forEach {
                tryOrNull(busLogger, "Failed to delete backup: $it") { it.deleteRecursively() }
            }

            var backupRoot: Path
            var i = 0
            do {
                val time = LocalDateTime.now().format(backupFileTimeFormatter)
                backupRoot = backupDir.resolve(if (i == 0) time else "$time-$i")
                i++
            } while (Files.exists(backupRoot))
            Files.createDirectory(backupRoot)
            for (it in Files.walk(project.root)) {
                val relativePath = project.root.relativize(it)
                if (it == project.root || relativePath.startsWith("backup")) continue
                val target = backupRoot.resolve(relativePath)
                launch {
                    withContext(Dispatchers.IO) {
                        if (Files.isDirectory(it)) Files.createDirectories(target)
                        else {
                            Files.createDirectories(target.parent)
                            Files.copy(it, target)
                        }
                    }
                }
            }
        }
    }

    override suspend fun save() {
        busLogger.info { "Saving project: $project to ${project.root.pathString}" }
        val cost = measureTimeMillis {
            val time = System.currentTimeMillis()
            project.timeCost += (time - lastSaveTime).toInt()
            lastSaveTime = time
            project.save()
            super.store(project.root)
            backup()
        }
        busLogger.info { "Saved project by cost ${cost}ms" }
    }

    override suspend fun store(path: Path) {
        busLogger.info { "Saving project: $project to $path" }
        val cost = measureTimeMillis {
            val time = System.currentTimeMillis()
            project.timeCost += (time - lastSaveTime).toInt()
            lastSaveTime = time
            project.save(path)
            super.store(path)
        }
        busLogger.info { "Saved project by cost ${cost}ms" }
    }

    override suspend fun processBlock(
        buffers: Array<FloatArray>,
        position: PlayPosition,
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

    override suspend fun restore(path: Path) {
        super.restore(path)
        loaded = true
        loadedCallbacks.forEach { it() }
        loadedCallbacks.clear()
    }

    override fun close() {
        autoSaveJob.cancel()
        super.close()
    }

    override fun findProcessor(uuid: UUID) = findProcessor(this, uuid)

    override fun onLoaded(callback: () -> Unit) {
        if (loaded) callback()
        else loadedCallbacks.add(callback)
    }
}
