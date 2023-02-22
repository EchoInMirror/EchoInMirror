package com.eimsound.dsp.native.processors

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.audioprocessor.*
import com.eimsound.daw.utils.*
import com.eimsound.dsp.native.isX86PEFile
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.apache.commons.lang3.SystemUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.*
import kotlin.system.measureTimeMillis

class NativeAudioPluginImpl(
    override val description: NativeAudioPluginDescription,
    factory: AudioProcessorFactory<*>,
) : NativeAudioPlugin, ProcessAudioProcessorImpl(description, factory) {
    override var name = description.name

    override suspend fun save(path: String) {
        super.save(path)
        if (!isLaunched) return
        mutex.withLock {
            outputStream?.apply {
                write(3)
                writeString("$path.bin")
                flush()
            }
        }
    }
}

class NativeAudioPluginFactoryImpl: NativeAudioPluginFactory {
    private val logger = LoggerFactory.getLogger(NativeAudioPluginFactoryImpl::class.java)
    private val configFile get() = Paths.get(System.getProperty("eim.dsp.nativeaudioplugins.list", "nativeAudioPlugins.json"))
    override val name = "NativeAudioPluginFactory"
    override val displayName = "原生"

    override val pluginIsFile = SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_LINUX

    override val descriptions: MutableSet<NativeAudioPluginDescription> = mutableStateSetOf()
    override val scanPaths: MutableSet<String> = mutableStateSetOf()
    override val skipList: MutableSet<String> = mutableStateSetOf()
    override lateinit var pluginExtensions: Set<String>

    var scannedCount by mutableStateOf(0)
    var allScanCount by mutableStateOf(0)
    val scanningPlugins = mutableStateMapOf<String, Process>()

    init {
        var read = false
        println(measureTimeMillis {
            if (configFile.toFile().exists()) {
                try {
                    runBlocking { fromJsonFile(configFile.toFile()) }
                    read = true
                } catch (e: Throwable) {
                    e.printStackTrace()
                    // Files.delete(NATIVE_AUDIO_PLUGIN_CONFIG)
                }
            }
            if (!read) {
                if (SystemUtils.IS_OS_WINDOWS) {
                    scanPaths.add("C:\\Program Files\\Common Files\\VST3")
                    scanPaths.add("C:\\Program Files\\Steinberg\\VSTPlugins")
                    scanPaths.add("C:\\Program Files\\VstPlugins")
                    scanPaths.add("C:\\Program Files\\Native Instruments\\VSTPlugins 64 bit")
                } else if (SystemUtils.IS_OS_LINUX) {
                    scanPaths.addAll(System.getenv("LADSPA_PATH")
                        .ifEmpty { "/usr/lib/ladspa;/usr/local/lib/ladspa;~/.ladspa" }
                        .replace(":", ";").split(";"))
                }
            }
            pluginExtensions = if (SystemUtils.IS_OS_WINDOWS) setOf("dll", "vst", "vst3")
            else if (SystemUtils.IS_OS_LINUX) setOf("so")
            else setOf("dylib")
        })
    }

    @OptIn(ExperimentalPathApi::class)
    override suspend fun scan()  {
        scanningPlugins.clear()
        val pluginList : MutableList<String> = mutableListOf()
        val scanned = hashSetOf<String>()
        scanned.addAll(skipList)
        descriptions.forEach { scanned.add(it.fileOrIdentifier) }
        val scanVisitor = fileVisitor {
            onPreVisitDirectory { directory, _ ->
                if (directory.name.startsWith(".")) {
                    FileVisitResult.SKIP_SUBTREE
                } else {
                    FileVisitResult.CONTINUE
                }
            }
            onVisitFile { file, _ ->
                if (pluginExtensions.contains(file.extension) && !scanned.contains(file.absolutePathString())) {
                    pluginList.add(file.absolutePathString())
                }
                FileVisitResult.CONTINUE
            }
        }

        scanPaths.forEach {
            val path = Path(it)
            if(!path.exists()) return@forEach
            Files.walkFileTree(path, scanVisitor)
        }
        allScanCount = pluginList.size

        val scanSemaphore = Semaphore(10)

        scannedCount = 0

        coroutineScope {
            pluginList.map {
                async {
                    scanSemaphore.withPermit {
                        logger.info("Scanning native audio plugin: {}", it)
                        val pb = ProcessBuilder(getNativeHostPath(it), "-S", "#")
                        pb.redirectError()
                        val process = pb.start()
                        scanningPlugins[it] = process
                        process.outputStream.write(it.encodeToByteArray())
                        process.outputStream.write(0)
                        process.outputStream.flush()
                        var result = ""
                        try {
                            result = process.inputStream.readAllBytes().decodeToString()
                                .substringAfter("\$EIMHostScanner{{").substringBeforeLast("}}EIMHostScanner\$")
                            descriptions.addAll(Json.decodeFromString<List<NativeAudioPluginDescription>>(result))
                        } catch (e: Throwable) {
                            logger.error("Failed to scan native audio plugin: $it, data: $result", e)
                            skipList.add(it)
                        } finally {
                            scannedCount++
                            scanningPlugins.remove(it)
                            process.destroy()
                            save()
                        }
                    }
                }
            }.awaitAll()
        }
        scanningPlugins.clear()
    }

    override suspend fun createAudioProcessor(description: AudioProcessorDescription): NativeAudioPlugin {
        if (description !is NativeAudioPluginDescription)
            throw NoSuchAudioProcessorException(description.identifier, name)
        return NativeAudioPluginImpl(description, this).apply {
            launch(getNativeHostPath(description), null)
        }
    }

    override suspend fun createAudioProcessor(path: String, json: JsonObject): NativeAudioPlugin {
        val description = descriptions.find { it.identifier == json["identifier"].asString() }
            ?: throw NoSuchAudioProcessorException(path, name)
        return NativeAudioPluginImpl(description, this).apply {
            launch(getNativeHostPath(description), "$path/${json["id"].asString()}.bin")
        }
    }

    override suspend fun save() { encodeJsonFile(configFile.toFile()) }

    override fun toJson() = mapOf(
        "descriptions" to descriptions,
        "scanPaths" to scanPaths,
        "skipList" to skipList
    )

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        descriptions.clear()
        scanPaths.clear()
        skipList.clear()
        descriptions.addAll((json["descriptions"] as JsonArray).map(Json::decodeFromJsonElement))
        scanPaths.addAll((json["scanPaths"] as JsonArray).map(JsonElement::asString))
        skipList.addAll((json["skipList"] as JsonArray).map(JsonElement::asString))
    }

    private fun getNativeHostPath(isX86: Boolean) = Paths.get(System.getProperty("eim.dsp.nativeaudioplugins.host" +
            (if (isX86) ".x86" else ""))).absolutePathString()
    private fun getNativeHostPath(description: NativeAudioPluginDescription) =
        getNativeHostPath(SystemUtils.IS_OS_WINDOWS && description.isX86)
    private fun getNativeHostPath(path: String) = getNativeHostPath(SystemUtils.IS_OS_WINDOWS && File(path).isX86PEFile())
}
