package com.eimsound.daw.api.controllers

import com.eimsound.audioprocessor.AudioProcessorParameter
import com.eimsound.daw.commons.json.JsonSerializable
import com.eimsound.daw.commons.json.asString
import kotlinx.serialization.json.JsonObject
import java.util.UUID

interface ParameterController : JsonSerializable {
    val parameter: AudioProcessorParameter
}

interface ParameterControllerFactory {
    fun createController(json: JsonObject): ParameterController
    fun createAudioProcessorParameterController(parameter: AudioProcessorParameter, audioProcessor: UUID): ParameterController
}

object DefaultParameterControllerFactory : ParameterControllerFactory {
    override fun createController(json: JsonObject): ParameterController = when (json["factory"]?.asString()) {
        "AudioProcessorParameterController" -> AudioProcessorParameterController().apply { fromJson(json) }
        else -> throw IllegalArgumentException("Unknown controller factory: ${json["factory"]}")
    }

    override fun createAudioProcessorParameterController(
        parameter: AudioProcessorParameter,
        audioProcessor: UUID
    ): ParameterController = AudioProcessorParameterController(parameter, audioProcessor)
}
