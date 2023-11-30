package com.eimsound.dsp.native.processors

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.audioprocessor.*
import com.eimsound.daw.commons.json.*
import com.eimsound.daw.utils.mutableStateSetOf
import com.eimsound.dsp.native.isX86PEFile
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.*
import org.apache.commons.lang3.SystemUtils
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

val AudioProcessorManager.nativeAudioPluginFactory: NativeAudioPluginFactory
    get() = factories.values.firstOrNull { it is NativeAudioPluginFactory } as NativeAudioPluginFactory

private val logger = KotlinLogging.logger {  }
class NativeAudioPluginFactoryImpl: NativeAudioPluginFactory {
    private val configFile get() = Paths.get(System.getProperty("eim.dsp.nativeaudioplugins.list", "nativeAudioPlugins.json"))
    override val name = "NativeAudioPluginFactory"
    override val displayName = "原生"

    override val pluginIsFile = SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_LINUX
    override val pluginExtensions = if (SystemUtils.IS_OS_WINDOWS) setOf("dll", "vst", "vst3")
    else if (SystemUtils.IS_OS_LINUX) setOf("so")
    else setOf("dylib")

    private var loaded = false
    private val loadMutex = Mutex()
    private val _descriptions: MutableSet<NativeAudioPluginDescription> = mutableStateSetOf()
    private val _scanPaths: MutableSet<String> = mutableStateSetOf()
    private val _skipList: MutableSet<String> = mutableStateSetOf()
    private val pluginLoadingMutexes = mutableStateMapOf<String, Mutex>()

    var scannedCount by mutableStateOf(0)
    var allScanCount by mutableStateOf(0)
    val scanningPlugins = mutableStateMapOf<String, Process>()

    override val descriptions: MutableSet<NativeAudioPluginDescription>
        get() {
            checkLoad()
            return _descriptions
        }
    override val scanPaths: MutableSet<String>
        get() {
            checkLoad()
            return _scanPaths
        }
    override val skipList: MutableSet<String>
        get() {
            checkLoad()
            return _skipList
        }

    companion object {
        var instance: NativeAudioPluginFactoryImpl? = null
    }

    init {
        instance = this
    }

    @OptIn(ExperimentalPathApi::class)
    override suspend fun scan()  {
        scanningPlugins.clear()
        val pluginList : MutableList<String> = mutableListOf()
        val scanned = hashSetOf<String>()
        scanned.addAll(skipList)
        descriptions.forEach { scanned.add(it.fileOrIdentifier) }

        if (SystemUtils.IS_OS_MAC) {
            pluginList.addAll(getAudioUnitsForMacOS())
        } else {
            val scanVisitor = fileVisitor {
                onPreVisitDirectory { directory, _ ->
                    if (directory.name.startsWith(".")) {
                        FileVisitResult.SKIP_SUBTREE
                    } else {
                        if(directory.name.endsWith(".vst3")) {
                            pluginList.add(directory.absolutePathString())
                            FileVisitResult.SKIP_SUBTREE
                        }
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
        }
        allScanCount = pluginList.size

        val scanSemaphore = Semaphore(10)

        scannedCount = 0

        coroutineScope {
            pluginList.map {
                async {
                    scanSemaphore.withPermit {
                        logger.info { "Scanning native audio plugin: $it" }
                        val pb = ProcessBuilder(getNativeHostPath(it), "-S", "#")
                        pb.redirectError()
                        val process = pb.start()
                        scanningPlugins[it] = process
                        process.outputStream.write(it.encodeToByteArray())
                        process.outputStream.flush()
                        process.outputStream.close()
                        var result = ""
                        try {
                            result = process.inputStream.readAllBytes().decodeToString()
                                .substringAfter("\$EIMHostScanner{{").substringBeforeLast("}}EIMHostScanner\$")
                            descriptions.addAll(Json.decodeFromString<List<NativeAudioPluginDescription>>(result))
                        } catch (e: Throwable) {
                            logger.error(e) { "Failed to scan native audio plugin: $it, data: $result" }
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
        loadMutex.withLock {
            pluginLoadingMutexes.getOrPut(description.fileOrIdentifier) { Mutex() }
        }.withLock {
            logger.info { "Creating native audio plugin: $description" }
            return NativeAudioPluginImpl(description, this).apply {
                launch(getNativeHostPath(description), null)
                delay(20)
            }
        }
    }

    override suspend fun createAudioProcessor(path: Path): NativeAudioPlugin {
        val json = path.resolve("processor.json").toJsonElement() as JsonObject
        val description = descriptions.find { it.identifier == json["identifier"]!!.asString() }
            ?: throw NoSuchAudioProcessorException(path.toString(), name)
        loadMutex.withLock {
            pluginLoadingMutexes.getOrPut(description.fileOrIdentifier) { Mutex() }
        }.withLock {
            logger.info { "Creating native audio plugin: $description in $path" }
            return NativeAudioPluginImpl(description, this).apply {
                restore(path)
                delay(20)
            }
        }
    }

    override suspend fun save() { encodeJsonFile(configFile) }

    override fun toJson() = buildJsonObject {
        put("descriptions", Json.encodeToJsonElement(descriptions))
        put("scanPaths", Json.encodeToJsonElement(scanPaths))
        put("skipList", Json.encodeToJsonElement(skipList))
    }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        descriptions.clear()
        scanPaths.clear()
        skipList.clear()
        descriptions.addAll((json["descriptions"] as JsonArray).map(Json::decodeFromJsonElement))
        scanPaths.addAll((json["scanPaths"] as JsonArray).map(JsonElement::asString))
        skipList.addAll((json["skipList"] as JsonArray).map(JsonElement::asString))
    }

    private fun checkLoad() {
        if (loaded) return
        runBlocking {
            loadMutex.withLock {
                if (loaded) return@runBlocking
                loaded = true
                if (configFile.exists()) {
                    try {
                        fromJsonFile(configFile)
                        return@withLock
                    } catch (e: Throwable) {
                        logger.error(e) { "Failed to load native audio plugin config file: $configFile" }
                        // Files.delete(NATIVE_AUDIO_PLUGIN_CONFIG)
                    }
                }
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
        }
    }

    private suspend fun getAudioUnitsForMacOS(): List<String> {
        val pb = ProcessBuilder(getNativeHostPath(), "-S")
        pb.redirectError()
        return withContext(Dispatchers.IO) {
            Json.decodeFromString<List<String>>(pb.start().inputStream.readAllBytes().decodeToString())
        }
    }

    private fun getNativeHostPath(isX86: Boolean = false) = Paths.get(System.getProperty("eim.dsp.nativeaudioplugins.host" +
            (if (isX86) ".x86" else ""))).absolutePathString()
    fun getNativeHostPath(description: NativeAudioPluginDescription) =
        getNativeHostPath(SystemUtils.IS_OS_WINDOWS && description.isX86)
    private fun getNativeHostPath(path: String) = getNativeHostPath(SystemUtils.IS_OS_WINDOWS && File(path).isX86PEFile())
}

class NativeAudioPluginImpl(
    override val description: NativeAudioPluginDescription,
    override val factory: NativeAudioPluginFactoryImpl,
) : NativeAudioPlugin, ProcessAudioProcessorImpl(description, factory) {
    override var name = description.name
}
