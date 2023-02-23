package com.eimsound.audioprocessor

import com.eimsound.daw.utils.JsonSerializable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class NativeAudioPluginDescription(
    override val name: String,
    val pluginFormatName: String,
    override val category: String,
    override val manufacturerName: String,
    override val version: String,
    override val identifier: String,
    val fileOrIdentifier: String,
    override val isInstrument: Boolean,
    val lastFileModTime: Long,
    val lastInfoUpdateTime: Long,
    val hasSharedContainer: Boolean,
    val hasARAExtension: Boolean,
    val deprecatedUid: Int,
    val uniqueId: Int,
    override val descriptiveName: String?,
    var isX86: Boolean = false
): AudioProcessorDescription {
    @Transient
    override val isDeprecated = isX86 || pluginFormatName == "VST"
    @Transient
    override val displayName = name + (if (pluginFormatName == "VST") " (VST2)" else "") + (if (isX86) " (32位)" else "")
}

class FailedToLoadAudioPluginException(message: String) : RuntimeException(message)

/**
 * @see com.eimsound.dsp.native.processors.NativeAudioPluginImpl
 */
interface NativeAudioPlugin: ProcessAudioProcessor {
    override val description: NativeAudioPluginDescription
}

/**
 * @see com.eimsound.dsp.native.processors.NativeAudioPluginFactoryImpl
 */
interface NativeAudioPluginFactory: AudioProcessorFactory<NativeAudioPlugin>, JsonSerializable {
    override val descriptions: Set<NativeAudioPluginDescription>
    val scanPaths: MutableSet<String>
    val skipList: MutableSet<String>
    @Transient
    val pluginExtensions: Set<String>
    @Transient
    val pluginIsFile: Boolean
    suspend fun scan()
    suspend fun save()
}
