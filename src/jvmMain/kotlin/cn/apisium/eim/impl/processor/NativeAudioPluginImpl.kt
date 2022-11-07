package cn.apisium.eim.impl.processor

import androidx.compose.runtime.mutableStateMapOf
import cn.apisium.eim.WORKING_PATH
import cn.apisium.eim.api.processor.NativeAudioPlugin
import cn.apisium.eim.api.processor.NativeAudioPluginDescription
import cn.apisium.eim.api.processor.NativeAudioPluginFactory
import cn.apisium.eim.utils.mutableStateSetOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.apache.commons.lang3.SystemUtils
import java.nio.file.Path

class NativeAudioPluginImpl(
    override val description: NativeAudioPluginDescription
) : NativeAudioPlugin, ProcessAudioProcessorImpl("D:\\Cpp\\EIMPluginScanner\\build\\EIMHost_artefacts\\Debug\\EIMHost.exe", " -l",
    JsonPrimitive(Json.encodeToString(NativeAudioPluginDescription.serializer(), description)).toString())

private val NATIVE_AUDIO_PLUGIN_CONFIG = WORKING_PATH.resolve("nativeAudioPlugin.json")

@Serializable
class NativeAudioPluginFactoryImpl: NativeAudioPluginFactory {
    override val name = "NativeAudioPluginFactory"
    override val pluginDescriptions: MutableMap<String, NativeAudioPluginDescription> = mutableStateMapOf()
    override val scanPaths: MutableSet<String> = mutableStateSetOf()
    override val skipList: MutableSet<String> = mutableStateSetOf()

    init {
        if (NATIVE_AUDIO_PLUGIN_CONFIG.toFile().exists()) {
            val json = NATIVE_AUDIO_PLUGIN_CONFIG.toFile().readText()
            val obj = Json.decodeFromString(serializer(), json)
            pluginDescriptions.putAll(obj.pluginDescriptions)
            scanPaths.addAll(obj.scanPaths)
            skipList.addAll(obj.skipList)
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

    override suspend fun scan() {
        TODO("Not yet implemented")
    }

    override fun createProcessor(identifier: String?, file: Path?): NativeAudioPlugin {
        return NativeAudioPluginImpl(pluginDescriptions[identifier]!!)
    }

    private fun save() {
        NATIVE_AUDIO_PLUGIN_CONFIG.toFile().writeText(Json.encodeToString(serializer(), this))
    }
}
