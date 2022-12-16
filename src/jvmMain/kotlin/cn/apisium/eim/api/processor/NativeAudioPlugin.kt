package cn.apisium.eim.api.processor

import cn.apisium.eim.utils.DateAsLongSerializer
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class NativeAudioPluginDescription(
    override val name: String,
    val pluginFormatName: String,
    override val category: String,
    override val manufacturerName: String,
    override val version: String,
    val fileOrIdentifier: String,
    override val isInstrument: Boolean,
    @Serializable(DateAsLongSerializer::class)
    val lastFileModTime: Date,
    @Serializable(DateAsLongSerializer::class)
    val lastInfoUpdateTime: Date,
    val hasSharedContainer: Boolean,
    val hasARAExtension: Boolean,
    val deprecatedUid: Int,
    val uniqueId: Int,
    val descriptiveName: String? = null
): AudioProcessorDescription {
    override val identifier get() = fileOrIdentifier
}

class FailedToLoadAudioPluginException(message: String) : RuntimeException(message)

interface NativeAudioPlugin: ProcessAudioProcessor {
    override val description: NativeAudioPluginDescription
}

interface NativeAudioPluginFactory: AudioProcessorFactory<NativeAudioPlugin> {
    override val descriptions: Set<NativeAudioPluginDescription>
    val scanPaths: MutableSet<String>
    val skipList: MutableSet<String>
    val pluginExtensions: Set<String>
    override fun createProcessor(description: AudioProcessorDescription): NativeAudioPlugin
    suspend fun scan()
    fun save()
}
