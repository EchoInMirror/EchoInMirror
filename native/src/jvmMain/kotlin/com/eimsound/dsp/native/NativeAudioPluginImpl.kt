package com.eimsound.dsp.native

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.audioprocessor.*
import com.eimsound.daw.utils.mutableStateSetOf
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import org.apache.commons.lang3.SystemUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

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
                ))
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

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE
)
class NativeAudioPluginFactoryImpl(private val configFile: Path, private val nativeHostPath: Path): NativeAudioPluginFactory {
    private val logger = LoggerFactory.getLogger(NativeAudioPluginFactoryImpl::class.java)
    override val name = "NativeAudioPluginFactory"
    override val pluginIsFile = SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_LINUX

    @JsonProperty
    @JsonMerge
    override val descriptions: MutableSet<NativeAudioPluginDescription> = mutableStateSetOf()
    @JsonProperty
    @JsonMerge
    override val scanPaths: MutableSet<String> = mutableStateSetOf()
    @JsonProperty
    @JsonMerge
    override val skipList: MutableSet<String> = mutableStateSetOf()
    override lateinit var pluginExtensions: Set<String>

    var scannedCount by mutableStateOf(0)
    var allScanCount by mutableStateOf(0)
    val scanningPlugins = mutableStateMapOf<String, Process>()

    init {
        var read = false
        if (configFile.toFile().exists()) {
            try {
                jacksonObjectMapper().readerForUpdating(this)
                    .readValue<NativeAudioPluginFactoryImpl>(configFile.toFile())
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
                        val pb = ProcessBuilder(nativeHostPath.absolutePathString(), " -S ", jacksonObjectMapper().writeValueAsString(it))
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
            throw NoSuchAudioProcessorException(description.identifier ?: "Unknown", name)
        return NativeAudioPluginImpl(description, this).apply {
            launch(nativeHostPath.absolutePathString(), "-L",
                jacksonObjectMapper().run { writeValueAsString(writeValueAsString(description)) })
        }
    }

    override suspend fun createAudioProcessor(path: String, json: JsonNode): NativeAudioPlugin {
        val description = descriptions.find { it.identifier == json["identifier"].asText() }
            ?: throw NoSuchAudioProcessorException(path, name)
        return NativeAudioPluginImpl(description, this).apply {
            jacksonObjectMapper().run { launch(nativeHostPath.absolutePathString(), "-P",
                writeValueAsString("$path/${json["id"]!!.asText()}.bin"), "-L",
                writeValueAsString(writeValueAsString(description))) }
        }
    }

    override fun save() {
        jacksonObjectMapper().writeValue(configFile.toFile(), this)
    }
}
