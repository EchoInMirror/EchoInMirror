package cn.apisium.eim.impl.processor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.apisium.eim.Configuration
import cn.apisium.eim.ROOT_PATH
import cn.apisium.eim.api.processor.AudioProcessorDescription
import cn.apisium.eim.api.processor.NativeAudioPlugin
import cn.apisium.eim.api.processor.NativeAudioPluginDescription
import cn.apisium.eim.api.processor.NativeAudioPluginFactory
import cn.apisium.eim.utils.OBJECT_MAPPER
import cn.apisium.eim.utils.mutableStateSetOf
import com.fasterxml.jackson.databind.ObjectMapper
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
    override val description: NativeAudioPluginDescription
) : NativeAudioPlugin, ProcessAudioProcessorImpl(
    Configuration.nativeHostPath, " -L",
    OBJECT_MAPPER.run { writeValueAsString(writeValueAsString(description)) }
) {
    override var name = description.name
}

private val NATIVE_AUDIO_PLUGIN_CONFIG = ROOT_PATH.resolve("nativeAudioPlugin.json")

private data class NativeAudioPluginFactoryData(
    val descriptions: Set<NativeAudioPluginDescription>,
    val scanPaths: Set<String>,
    val skipList: Set<String>
)

class NativeAudioPluginFactoryImpl: NativeAudioPluginFactory {
    private val logger = LoggerFactory.getLogger(NativeAudioPluginFactoryImpl::class.java)
    override val name = "NativeAudioPluginFactory"

    override val descriptions = mutableSetOf<NativeAudioPluginDescription>()
    override val scanPaths = mutableStateSetOf<String>()
    override val skipList = mutableStateSetOf<String>()
    override lateinit var pluginExtensions: Set<String>
    var scannedCount by mutableStateOf(0)
    var allScanCount by mutableStateOf(0)
    val scanningPlugins = mutableStateMapOf<String, Process>()

    init {
        if (NATIVE_AUDIO_PLUGIN_CONFIG.toFile().exists()) {
            val data = OBJECT_MAPPER.readValue<NativeAudioPluginFactoryData>(NATIVE_AUDIO_PLUGIN_CONFIG.toFile())
            descriptions.addAll(data.descriptions)
            scanPaths.addAll(data.scanPaths)
            skipList.addAll(data.skipList)
        } else {
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
        descriptions.forEach { scanned.add(it.identifier) }
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
                        val process = Runtime.getRuntime()
                            .exec(arrayOf("./EIMHost.exe",  " -S ", ObjectMapper().writeValueAsString(it)))
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

    override fun createProcessor(description: AudioProcessorDescription): NativeAudioPlugin {
        if (description !is NativeAudioPluginDescription) throw IllegalArgumentException("description is not NativeAudioPluginDescription")
        return NativeAudioPluginImpl(description)
    }

     override fun save() {
         OBJECT_MAPPER.writeValue(NATIVE_AUDIO_PLUGIN_CONFIG.toFile(),
             NativeAudioPluginFactoryData(descriptions, scanPaths, skipList))
    }
}
