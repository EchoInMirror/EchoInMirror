package com.eimsound.audioprocessor

import com.eimsound.daw.utils.NoSuchFactoryException
import com.eimsound.daw.utils.Reloadable
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
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

@JsonSerialize(using = AudioProcessorFactoryNameSerializer::class)
interface AudioProcessorFactory<T: AudioProcessor> {
    val name: String
    val descriptions: Set<AudioProcessorDescription>
    val displayName: String
    suspend fun createAudioProcessor(description: AudioProcessorDescription): T
    suspend fun createAudioProcessor(path: String, json: JsonNode): T
}

class AudioProcessorFactoryNameSerializer @JvmOverloads constructor(t: Class<AudioProcessorFactory<*>>? = null) :
    StdSerializer<AudioProcessorFactory<*>>(t) {
    override fun serialize(value: AudioProcessorFactory<*>, jgen: JsonGenerator, provider: SerializerProvider?) {
        jgen.writeString(value.name)
    }
}
