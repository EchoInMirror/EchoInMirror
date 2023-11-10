package com.eimsound.daw.api.controllers

import com.eimsound.audioprocessor.AudioProcessorParameter
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.commons.json.asString
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.*

private val logger = KotlinLogging.logger { }
internal class AudioProcessorParameterController(
    parameter: AudioProcessorParameter? = null,
    audioProcessor: UUID? = null
) : ParameterController {
    override lateinit var parameter: AudioProcessorParameter
    private lateinit var audioProcessor: UUID

    init {
        if (parameter != null) this.parameter = parameter
        if (audioProcessor != null) this.audioProcessor = audioProcessor
    }

    override fun toJson() = buildJsonObject {
        put("factory", "AudioProcessorParameterController")
        put("parameter", parameter.id)
        put("uuid", audioProcessor.toString())
    }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        val uuid = json["uuid"]?.asString() ?: return
        audioProcessor = UUID.fromString(uuid)
        val p = EchoInMirror.bus?.findProcessor(audioProcessor)

        if (p == null) {
            logger.warn { "Audio processor with UUID $uuid not found." }
            return
        }

        val parameterId = json["parameter"]?.asString()
        val pa = p.processor.parameters.firstOrNull { it.id == parameterId }
        if (pa == null) {
            logger.warn { "Parameter with id $parameterId not found." }
            return
        }
    }

    override fun hashCode() = parameter.hashCode() + audioProcessor.hashCode() * 31

    override fun equals(other: Any?): Boolean {
        if (other !is AudioProcessorParameterController) return false
        return parameter == other.parameter && audioProcessor == other.audioProcessor
    }

    override fun toString() = "AudioProcessorParameterController(parameter=$parameter, audioProcessor=$audioProcessor)"
}
