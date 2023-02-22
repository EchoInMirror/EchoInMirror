package com.eimsound.audioprocessor

import com.eimsound.daw.utils.IDisplayName
import com.eimsound.daw.utils.NoSuchFactoryException
import com.eimsound.daw.utils.Reloadable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
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
    suspend fun createAudioProcessor(path: String, json: JsonObject): AudioProcessor
}

@Serializable(with = AudioProcessorFactoryNameSerializer::class)
interface AudioProcessorFactory<T: AudioProcessor> : IDisplayName {
    val name: String
    val descriptions: Set<AudioProcessorDescription>
    suspend fun createAudioProcessor(description: AudioProcessorDescription): T
    suspend fun createAudioProcessor(path: String, json: JsonObject): T
}

object AudioProcessorFactoryNameSerializer : KSerializer<AudioProcessorFactory<*>> {
    override val descriptor = String.serializer().descriptor

    override fun serialize(encoder: Encoder, value: AudioProcessorFactory<*>) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): AudioProcessorFactory<*> {
        val name = decoder.decodeString()
        return AudioProcessorManager.instance.factories[name] ?: throw NoSuchFactoryException(name)
    }
}
