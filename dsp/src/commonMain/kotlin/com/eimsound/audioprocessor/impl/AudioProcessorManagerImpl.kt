package com.eimsound.audioprocessor.impl

import androidx.compose.runtime.mutableStateMapOf
import com.eimsound.audioprocessor.*
import com.eimsound.daw.utils.NoSuchFactoryException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.util.*

val AudioProcessorManager.nativeAudioPluginManager
    get() = factories["NativeAudioPluginFactory"] as NativeAudioPluginFactory

class AudioProcessorManagerImpl: AudioProcessorManager {
    override val factories = mutableStateMapOf<String, AudioProcessorFactory<*>>()

    init { reload() }

    override fun reload() {
        factories.clear()
        ServiceLoader.load(AudioProcessorFactory::class.java).forEach { factories[it.name] = it }
    }

    override suspend fun createAudioProcessor(factory: String, description: AudioProcessorDescription) =
        factories[factory]?.createAudioProcessor(description) ?: throw NoSuchFactoryException(factory)

    override suspend fun createAudioProcessor(path: String, id: String) =
        createAudioProcessor(path, ObjectMapper().readTree(File(path, "$id.json")))

    override suspend fun createAudioProcessor(path: String, json: JsonNode): AudioProcessor {
        val factory = json["factory"]?.asText()
        return factories[factory]?.createAudioProcessor(path, json)
            ?: throw NoSuchFactoryException(factory ?: "Null")
    }
}
