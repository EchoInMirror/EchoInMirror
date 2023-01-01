package cn.apisium.eim.impl.processor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.apisium.eim.Configuration
import cn.apisium.eim.ROOT_PATH
import cn.apisium.eim.api.processor.*
import cn.apisium.eim.utils.OBJECT_MAPPER
import cn.apisium.eim.utils.mutableStateSetOf
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.apache.commons.lang3.SystemUtils
import org.slf4j.LoggerFactory
import java.nio.file.FileVisitResult
import java.nio.file.Files
import kotlin.io.path.*

class NativeAudioPluginImpl(
    override val description: NativeAudioPluginDescription,
    factory: AudioProcessorFactory<*>,
) : NativeAudioPlugin, ProcessAudioProcessorImpl(description, factory) {
    override var name = description.name
}

private val NATIVE_AUDIO_PLUGIN_CONFIG = ROOT_PATH.resolve("nativeAudioPlugin.json")

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE
)
class NativeAudioPluginFactoryImpl: NativeAudioPluginFactory {
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
        if (NATIVE_AUDIO_PLUGIN_CONFIG.toFile().exists()) {
            try {
                jacksonObjectMapper().readerForUpdating(this)
                    .readValue<NativeAudioPluginFactoryImpl>(NATIVE_AUDIO_PLUGIN_CONFIG.toFile())
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
                        val pb = ProcessBuilder(Configuration.nativeHostPath,  " -S ", ObjectMapper().writeValueAsString(it))
                        pb.redirectError()
                        val process = pb.start()
                        scanningPlugins[it] = process
                        var result = ""
                        try {
                            result = process.inputStream.readAllBytes().decodeToString()
                                .substringAfter("\$EIMHostScanner{{").substringBeforeLast("}}EIMHostScanner\$")
                            OBJECT_MAPPER.readValue<List<NativeAudioPluginDescription>>(result)
                                .forEach((descriptions::add))
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
            launch(Configuration.nativeHostPath, " -L",
                OBJECT_MAPPER.run { writeValueAsString(writeValueAsString(description)) })
        }
    }

    override suspend fun createAudioProcessor(path: String, json: JsonNode): NativeAudioPlugin {
        throw NoSuchAudioProcessorException("Unknown", name)
    }

    override fun save() {
         OBJECT_MAPPER.writeValue(NATIVE_AUDIO_PLUGIN_CONFIG.toFile(), this)
    }
}
