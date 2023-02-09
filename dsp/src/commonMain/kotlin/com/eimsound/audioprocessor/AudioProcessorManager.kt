package com.eimsound.audioprocessor

import com.eimsound.daw.utils.NoSuchFactoryException
import com.eimsound.daw.utils.Reloadable
import com.fasterxml.jackson.databind.JsonNode
import java.util.*

class NoSuchAudioProcessorException(name: String, factory: String): Exception("No such audio processor: $name of $factory")

/**
 * @see com.eimsound.audioprocessor.impl.AudioProcessorManagerImpl
 */
interface AudioProcessorManager : Reloadable {
    companion object {
        val instance by lazy { ServiceLoader.load(AudioProcessorManager::class.java).first()!! }
    }

    val factories: Map<String, AudioProcessorFactory<*>>

    @Throws(NoSuchFactoryException::class)
    suspend fun createAudioProcessor(factory: String, description: AudioProcessorDescription): AudioProcessor
    @Throws(NoSuchFactoryException::class)
    suspend fun createAudioProcessor(path: String, id: String): AudioProcessor
    @Throws(NoSuchFactoryException::class)
    suspend fun createAudioProcessor(path: String, json: JsonNode): AudioProcessor
}
