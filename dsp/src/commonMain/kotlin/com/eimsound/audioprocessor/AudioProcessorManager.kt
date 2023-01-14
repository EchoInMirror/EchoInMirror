package com.eimsound.audioprocessor

import com.eimsound.daw.utils.NoSuchFactoryException
import com.fasterxml.jackson.databind.JsonNode

class NoSuchAudioProcessorException(name: String, factory: String): Exception("No such audio processor: $name of $factory")

interface AudioProcessorManager {
    val factories: Map<String, AudioProcessorFactory<*>>

    fun registerFactory(factory: AudioProcessorFactory<*>)

    @Throws(NoSuchFactoryException::class)
    suspend fun createAudioProcessor(factory: String, description: AudioProcessorDescription): AudioProcessor
    @Throws(NoSuchFactoryException::class)
    suspend fun createAudioProcessor(path: String, id: String): AudioProcessor
    @Throws(NoSuchFactoryException::class)
    suspend fun createAudioProcessor(path: String, json: JsonNode): AudioProcessor
}
