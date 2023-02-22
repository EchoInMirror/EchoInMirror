package com.eimsound.daw.impl.processor

import com.eimsound.audioprocessor.*
import com.eimsound.daw.VERSION
import com.eimsound.daw.processor.synthesizer.KarplusStrongSynthesizer
import com.eimsound.daw.processor.synthesizer.KarplusStrongSynthesizerDescription
import com.eimsound.daw.processor.synthesizer.SineWaveSynthesizer
import com.eimsound.daw.processor.synthesizer.SineWaveSynthesizerDescription
import com.eimsound.daw.utils.asString
import kotlinx.serialization.json.JsonObject

class EIMAudioProcessorDescription(name: String, category: String? = null, isInstrument: Boolean = false):
    DefaultAudioProcessorDescription(name, name, category, "EIMSound", VERSION, isInstrument)

const val EIMAudioProcessorFactoryName = "EIMAudioProcessorFactory"
val AudioProcessorManager.eimAudioProcessorFactory get() = factories[EIMAudioProcessorFactoryName] as EIMAudioProcessorFactory

class EIMAudioProcessorFactory : AudioProcessorFactory<AudioProcessor> {
    override val name = EIMAudioProcessorFactoryName
    override val displayName = "内置"
    override val descriptions = setOf(KarplusStrongSynthesizerDescription)
    private val audioProcessors = descriptions.associateBy { it.name }

    override suspend fun createAudioProcessor(description: AudioProcessorDescription) = when(description) {
        KarplusStrongSynthesizerDescription -> KarplusStrongSynthesizer(this)
        SineWaveSynthesizerDescription -> SineWaveSynthesizer(this)
        else -> throw NoSuchAudioProcessorException(description.name, name)
    }

    override suspend fun createAudioProcessor(path: String, json: JsonObject): AudioProcessor {
        val name = json["name"]?.asString() ?: "Unknown"
        val disc = audioProcessors[name] ?: throw NoSuchAudioProcessorException(name, this.name)
        return createAudioProcessor(disc).apply { load(path, json) }
    }
}
