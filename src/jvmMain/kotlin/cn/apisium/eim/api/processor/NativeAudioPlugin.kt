package cn.apisium.eim.api.processor

import com.fasterxml.jackson.annotation.JsonFormat
import java.util.Date

data class NativeAudioPluginDescription(
    override val name: String,
    val pluginFormatName: String,
    override val category: String,
    override val manufacturerName: String,
    override val version: String,
    override val identifier: String,
    val fileOrIdentifier: String,
    override val isInstrument: Boolean,
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    val lastFileModTime: Date,
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    val lastInfoUpdateTime: Date,
    val hasSharedContainer: Boolean,
    val hasARAExtension: Boolean,
    val deprecatedUid: Int,
    val uniqueId: Int,
    val descriptiveName: String? = null,
): AudioProcessorDescription

class FailedToLoadAudioPluginException(message: String) : RuntimeException(message)

interface NativeAudioPlugin: ProcessAudioProcessor {
    override val description: NativeAudioPluginDescription
}

interface NativeAudioPluginFactory: AudioProcessorFactory<NativeAudioPlugin> {
    override val descriptions: Set<NativeAudioPluginDescription>
    val pluginIsFile: Boolean
    val scanPaths: MutableSet<String>
    val skipList: MutableSet<String>
    val pluginExtensions: Set<String>
    suspend fun scan()
    fun save()
}
