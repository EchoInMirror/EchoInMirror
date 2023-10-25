package com.eimsound.daw.impl.processor

import com.eimsound.audioprocessor.*
import com.eimsound.daw.VERSION
import com.eimsound.daw.processor.synthesizer.KarplusStrongSynthesizer
import com.eimsound.daw.processor.synthesizer.KarplusStrongSynthesizerDescription
import com.eimsound.daw.processor.synthesizer.SineWaveSynthesizer
import com.eimsound.daw.processor.synthesizer.SineWaveSynthesizerDescription
import com.eimsound.daw.utils.asString
import com.eimsound.daw.utils.toJsonElement
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.JsonObject
import java.io.File

class EIMAudioProcessorDescription(name: String, category: String? = null, isInstrument: Boolean = false):
    DefaultAudioProcessorDescription(name, name, category, "EIMSound", VERSION, isInstrument)

const val EIMAudioProcessorFactoryName = "EIMAudioProcessorFactory"
val AudioProcessorManager.eimAudioProcessorFactory get() = factories[EIMAudioProcessorFactoryName] as EIMAudioProcessorFactory

private val logger = KotlinLogging.logger { }
class EIMAudioProcessorFactory : AudioProcessorFactory<AudioProcessor> {
    override val name = EIMAudioProcessorFactoryName
    override val displayName = "内置"
    override val descriptions = setOf(KarplusStrongSynthesizerDescription)
    private val audioProcessors = descriptions.associateBy { it.name }

    override suspend fun createAudioProcessor(description: AudioProcessorDescription): AudioProcessor {
        logger.info { "Creating audio processor \"${description.name}\"" }
        return when(description) {
            KarplusStrongSynthesizerDescription -> KarplusStrongSynthesizer(this)
            SineWaveSynthesizerDescription -> SineWaveSynthesizer(this)
            else -> throw NoSuchAudioProcessorException(description.name, name)
        }
    }

    override suspend fun createAudioProcessor(path: String): AudioProcessor {
        val json = File(path, "processor.json").toJsonElement() as JsonObject
        val name = json["name"]?.asString() ?: "Unknown"
        logger.info { "Creating audio processor \"$name\" in \"$path\"" }
        val desc = audioProcessors[name] ?: throw NoSuchAudioProcessorException(name, this.name)
        return createAudioProcessor(desc).apply { restore(path) }
    }
}
