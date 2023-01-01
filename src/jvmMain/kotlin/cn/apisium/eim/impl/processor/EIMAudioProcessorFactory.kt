package cn.apisium.eim.impl.processor

import cn.apisium.eim.VERSION
import cn.apisium.eim.api.processor.*
import cn.apisium.eim.processor.synthesizer.KarplusStrongSynthesizer
import cn.apisium.eim.processor.synthesizer.KarplusStrongSynthesizerDescription
import com.fasterxml.jackson.databind.JsonNode

class EIMAudioProcessorDescription(name: String, category: String? = null, isInstrument: Boolean? = null):
    DefaultAudioProcessorDescription(name, category, "Echo In Mirror", VERSION, isInstrument)

class EIMAudioProcessorFactory : AudioProcessorFactory<AudioProcessor> {
    override val name = "EIMAudioProcessorFactory"
    override val descriptions = setOf(KarplusStrongSynthesizerDescription)
    private val audioProcessors = descriptions.associateBy { it.name }

    override suspend fun createAudioProcessor(description: AudioProcessorDescription): AudioProcessor {
        if (description !is EIMAudioProcessorDescription) throw NoSuchAudioProcessorException(description.name, name)
        when(description) {
            KarplusStrongSynthesizerDescription -> return KarplusStrongSynthesizer(description, this)
            else -> throw NoSuchAudioProcessorException(description.name, name)
        }
    }

    override suspend fun createAudioProcessor(path: String, json: JsonNode): AudioProcessor {
        val name = json.get("name")?.asText() ?: "Unknown"
        val disc = audioProcessors[name] ?: throw NoSuchAudioProcessorException(name, this.name)
        return createAudioProcessor(disc).apply { load(path, json) }
    }
}
