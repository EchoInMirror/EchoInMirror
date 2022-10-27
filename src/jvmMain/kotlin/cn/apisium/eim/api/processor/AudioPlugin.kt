package cn.apisium.eim.api.processor

import cn.apisium.eim.utils.DateAsLongSerializer
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class PluginDescription(
    val name: String,
    val pluginFormatName: String,
    val category: String,
    val manufacturerName: String,
    val version: String,
    val fileOrIdentifier: String,
    val isInstrument: Boolean,
    @Serializable(DateAsLongSerializer::class)
    val lastFileModTime: Date,
    @Serializable(DateAsLongSerializer::class)
    val lastInfoUpdateTime: Date,
    val hasSharedContainer: Boolean,
    val hasARAExtension: Boolean,
    val deprecatedUid: Int,
    val uniqueId: Int,
    val descriptiveName: String? = null
)

class FailedToLoadAudioPluginException(message: String) : RuntimeException(message)

interface AudioPlugin: AudioProcessor {
    val description: PluginDescription
    val isLaunched: Boolean
    suspend fun launch(): Boolean
}
