package com.eimsound.audioprocessor

import com.eimsound.daw.commons.IDisplayName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


/**
 * @see com.eimsound.daw.impl.processor.EIMAudioProcessorDescription
 * @see com.eimsound.audioprocessor.NativeAudioPluginDescription
 */
@Serializable
sealed interface AudioProcessorDescription : Comparable<AudioProcessorDescription>, IDisplayName {
    val name: String
    val category: String?
    val manufacturerName: String?
    val version: String?
    val isInstrument: Boolean
    val identifier: String
    val descriptiveName: String?
    @Transient
    val isDeprecated: Boolean?
    @Transient
    override val displayName: String
    override fun compareTo(other: AudioProcessorDescription) =
        if (other.isDeprecated != isDeprecated) {
            if (isDeprecated == true) 1 else -1
        } else {
            val n = name.compareTo(other.name)
            if (n == 0) version?.compareTo(other.version ?: "") ?: 0 else n
        }
}

interface AudioProcessorListener
interface SuddenChangeListener {
    fun onSuddenChange()
}

open class DefaultAudioProcessorDescription(
    override val name: String,
    override val identifier: String,
    override val category: String? = null,
    override val manufacturerName: String? = null,
    override val version: String? = null,
    override val isInstrument: Boolean = false,
    override val descriptiveName: String? = null,
    override val isDeprecated: Boolean? = false,
): AudioProcessorDescription {
    override val displayName get() = name

    override fun toString(): String {
        return "DefaultAudioProcessorDescription(name='$name', category=$category, manufacturerName=$manufacturerName, " +
                "version=$version, isInstrument=$isInstrument, identifier='$identifier', " +
                "descriptiveName=$descriptiveName, isDeprecated=$isDeprecated)"
    }
}
