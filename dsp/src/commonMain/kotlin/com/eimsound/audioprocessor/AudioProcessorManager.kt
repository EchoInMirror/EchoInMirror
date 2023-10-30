package com.eimsound.audioprocessor

import com.eimsound.daw.utils.IDisplayName
import com.eimsound.daw.utils.NoSuchFactoryException
import com.eimsound.daw.utils.Reloadable
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.file.Path
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
    suspend fun createAudioProcessor(path: Path): AudioProcessor
}

private val audioProcessorFactoryLogger = KotlinLogging.logger("AudioProcessorFactory")

/**
 * @see com.eimsound.daw.impl.processor.EIMAudioProcessorFactory
 * @see com.eimsound.dsp.native.processors.NativeAudioPluginFactoryImpl
 */
@Serializable(with = AudioProcessorFactoryNameSerializer::class)
interface AudioProcessorFactory<T: AudioProcessor> : IDisplayName {
    val name: String
    val descriptions: Set<AudioProcessorDescription>
    suspend fun createAudioProcessor(description: AudioProcessorDescription): T
    suspend fun createAudioProcessor(path: Path): T
}

suspend fun <T: AudioProcessor> AudioProcessorFactory<T>.createAudioProcessorOrNull(
    description: AudioProcessorDescription): Pair<T?, Throwable?> = try {
    createAudioProcessor(description) to null
} catch (e: Throwable) {
    audioProcessorFactoryLogger.error(e) { "Failed to create audio processor ($description)" }
    null to e
}

data class AudioProcessorDescriptionAndFactory(val description: AudioProcessorDescription, val factory: AudioProcessorFactory<*>)

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
