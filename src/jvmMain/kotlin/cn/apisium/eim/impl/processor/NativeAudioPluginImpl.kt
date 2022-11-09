package cn.apisium.eim.impl.processor

import androidx.compose.runtime.mutableStateMapOf
import cn.apisium.eim.WORKING_PATH
import cn.apisium.eim.api.processor.NativeAudioPlugin
import cn.apisium.eim.api.processor.NativeAudioPluginDescription
import cn.apisium.eim.api.processor.NativeAudioPluginFactory
import cn.apisium.eim.utils.mutableStateSetOf
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.apache.commons.lang3.SystemUtils
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

class NativeAudioPluginImpl(
    override val description: NativeAudioPluginDescription
) : NativeAudioPlugin, ProcessAudioProcessorImpl("D:\\Cpp\\EIMPluginScanner\\build\\EIMHost_artefacts\\Debug\\EIMHost.exe", " -l",
    JsonPrimitive(Json.encodeToString(NativeAudioPluginDescription.serializer(), description)).toString())

private val NATIVE_AUDIO_PLUGIN_CONFIG = WORKING_PATH.resolve("nativeAudioPlugin.json")

@Serializable
class NativeAudioPluginFactoryData {
    val name = "NativeAudioPluginFactory"
    val pluginDescriptions: MutableMap<String, NativeAudioPluginDescription> = mutableStateMapOf()
    val scanPaths: MutableSet<String> = mutableStateSetOf()
    val skipList: MutableSet<String> = mutableStateSetOf()
}

@Serializable
class NativeAudioPluginFactoryImpl: NativeAudioPluginFactory {
    override val name = "NativeAudioPluginFactory"
    override val pluginDescriptions: MutableMap<String, NativeAudioPluginDescription> = mutableStateMapOf()
    override val scanPaths: MutableSet<String> = mutableStateSetOf()
    override val skipList: MutableSet<String> = mutableStateSetOf()

    init {
        if (NATIVE_AUDIO_PLUGIN_CONFIG.toFile().exists()) {
            val json = NATIVE_AUDIO_PLUGIN_CONFIG.toFile().readText()
            val data = Json.decodeFromString(NativeAudioPluginFactoryData.serializer(), json)
            pluginDescriptions.putAll(data.pluginDescriptions)
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
    }

    @OptIn(ExperimentalPathApi::class)
    override suspend fun scan()  {
        val pluginList : MutableList<String> = mutableListOf()
        val scanVisitor = fileVisitor {
            onPreVisitDirectory { directory, _ ->
                // 跳过隐藏文件夹
                if (directory.name.startsWith(".")) {
                    FileVisitResult.SKIP_SUBTREE
                } else {
                    FileVisitResult.CONTINUE
                }
            }
            onVisitFile { file, _ ->
                if (file.extension == "dll" || file.extension == "vst3") {
                    pluginList.add(file.toAbsolutePath().toString())
                }
                FileVisitResult.CONTINUE
            }
        }

        scanPaths.forEach {
            val path = Path(it)
            if(!path.exists()) return@forEach
            Files.walkFileTree(path, scanVisitor)
        }

        val processNum = 10
        val scanSemaphore = Semaphore(processNum)

        coroutineScope {
            pluginList.map {
                async {
                    scanSemaphore.withPermit {
                        val process = Runtime.getRuntime().exec((listOf("./EIMHost.exe") + "-s" + it).toTypedArray())
                        val result = try {
                            val stdout = async {
                                runInterruptible {
                                    String(process.inputStream.readAllBytes(), UTF_8)
                                }
                            }
                            stdout.await()
                        } finally {
                            process.destroy()
                        }
                        if (result.isEmpty()) return@withPermit
                        Json.decodeFromString<List<NativeAudioPluginDescription>>(result).forEach {
                            pluginDescriptions[it.name] = it
                        }
                        save()
                    }
                }
            }.awaitAll()
        }
    }

    override fun createProcessor(identifier: String?, file: Path?): NativeAudioPlugin {
        return NativeAudioPluginImpl(pluginDescriptions[identifier]!!)
    }

    private fun save() {
        NATIVE_AUDIO_PLUGIN_CONFIG.toFile().writeText(Json.encodeToString(serializer(), this))
    }
}
