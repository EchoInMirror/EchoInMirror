package com.eimsound.audioprocessor

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class NativeAudioPluginDescription(
    override val name: String,
    val pluginFormatName: String,
    override val category: String,
    override val manufacturerName: String,
    override val version: String,
    override val identifier: String,
    val fileOrIdentifier: String,
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    override val isInstrument: Boolean,
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    val lastFileModTime: Long,
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    val lastInfoUpdateTime: Long,
    val hasSharedContainer: Boolean,
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    val hasARAExtension: Boolean,
    val deprecatedUid: Int,
    val uniqueId: Int,
    override val descriptiveName: String?,
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    var isX86: Boolean = false
): AudioProcessorDescription {
    override val isDeprecated = isX86 || pluginFormatName == "VST"
    override val displayName = name + (if (pluginFormatName == "VST") " (VST2)" else "") + (if (isX86) " (32位)" else "")
}

class FailedToLoadAudioPluginException(message: String) : RuntimeException(message)

interface NativeAudioPlugin: ProcessAudioProcessor {
    override val description: NativeAudioPluginDescription
}

interface NativeAudioPluginFactory: AudioProcessorFactory<NativeAudioPlugin> {
    override val descriptions: Set<NativeAudioPluginDescription>
    val scanPaths: MutableSet<String>
    val skipList: MutableSet<String>
    @get:JsonIgnore
    val pluginExtensions: Set<String>
    @get:JsonIgnore
    val pluginIsFile: Boolean
    suspend fun scan()
    fun save()
}
