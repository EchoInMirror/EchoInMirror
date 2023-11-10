package com.eimsound.daw.api.controllers

import com.eimsound.audioprocessor.IAudioProcessorParameter
import com.eimsound.daw.commons.json.JsonSerializable
import com.eimsound.daw.commons.json.asString
import kotlinx.serialization.json.JsonObject
interface ParameterController : JsonSerializable {
    val parameter: IAudioProcessorParameter
}

interface ParameterControllerFactory {
    fun createController(json: JsonObject): ParameterController
}

object DefaultParameterControllerFactory : ParameterControllerFactory {
    override fun createController(json: JsonObject) = when (json["factory"]?.asString()) {
        "AudioProcessorParameterController" -> AudioProcessorParameterController().apply { fromJson(json) }
        else -> throw IllegalArgumentException("Unknown controller factory: ${json["factory"]}")
    }
}
