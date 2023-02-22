package com.eimsound.dsp.native.processors

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.audioprocessor.*
import com.eimsound.daw.utils.mutableStateSetOf
import com.eimsound.dsp.native.isX86PEFile
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
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
        withContext(Dispatchers.IO) {
            ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(
                File("$path.json"), mapOf(
                    "factory" to factory.name,
                    "name" to name,
                    "id" to id,
                    "identifier" to description.identifier
                )
            )
        }
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

@Serializable
data class NativeAudioPluginFactoryData(
    val descriptions: Set<NativeAudioPluginDescription>,
    val scanPaths: Set<String>,
    val skipList: Set<String>,
)

@OptIn(ExperimentalSerializationApi::class)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE
)
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
                    Json.decodeFromStream<NativeAudioPluginFactoryData>(configFile.inputStream()).apply {
                        println(descriptions)
                    }
//                    descriptions.addAll(data.descriptions)
//                    scanPaths.addAll(data.scanPaths)
//                    skipList.addAll(data.skipList)
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
                        val pb = ProcessBuilder(getNativeHostPath(it), " -S ", jacksonObjectMapper().writeValueAsString(it))
                        pb.redirectError()
                        val process = pb.start()
                        scanningPlugins[it] = process
                        var result = ""
                        try {
                            result = process.inputStream.readAllBytes().decodeToString()
                                .substringAfter("\$EIMHostScanner{{").substringBeforeLast("}}EIMHostScanner\$")
                            jacksonObjectMapper().readValue<List<NativeAudioPluginDescription>>(result).forEach(descriptions::add)
                            descriptions.distinctBy { it.identifier }
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
            launch(getNativeHostPath(description))
        }
    }

    override suspend fun createAudioProcessor(path: String, json: JsonNode): NativeAudioPlugin {
        val description = descriptions.find { it.identifier == json["identifier"].asText() }
            ?: throw NoSuchAudioProcessorException(path, name)
        return NativeAudioPluginImpl(description, this).apply {
            jacksonObjectMapper().run { launch(getNativeHostPath(description), "-P",
                writeValueAsString("$path/${json["id"]!!.asText()}.bin"), "-L",
                writeValueAsString(writeValueAsString(description))) }
        }
    }

    override fun save() {
        jacksonObjectMapper().writeValue(configFile.toFile(), this)
    }

    private fun getNativeHostPath(isX86: Boolean) = Paths.get(System.getProperty("eim.dsp.nativeaudioplugins.host" +
            (if (isX86) ".x86" else ""))).absolutePathString()
    private fun getNativeHostPath(description: NativeAudioPluginDescription) =
        getNativeHostPath(SystemUtils.IS_OS_WINDOWS && description.isX86)
    private fun getNativeHostPath(path: String) = getNativeHostPath(SystemUtils.IS_OS_WINDOWS && File(path).isX86PEFile())
}
