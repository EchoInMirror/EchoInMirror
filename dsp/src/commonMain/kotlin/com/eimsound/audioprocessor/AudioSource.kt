package com.eimsound.audioprocessor

import com.eimsound.daw.commons.*
import com.eimsound.daw.commons.json.JsonSerializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import java.nio.file.Path
import java.util.*

interface AudioSource : JsonSerializable {
    val source: AudioSource?
    @Transient
    val sampleRate: Float
    @Transient
    val channels: Int
    @Transient
    val length: Long // timeInSamples
    val factory: AudioSourceFactory<*>
    fun getSamples(start: Long, length: Int, buffers: Array<FloatArray>): Int
}

/**
 * @see com.eimsound.audiosources.DefaultFileAudioSource
 */
interface FileAudioSource : AudioSource, AutoCloseable {
    val file: Path
    @Transient
    val isRandomAccessible: Boolean
    override val factory: FileAudioSourceFactory<*>
}

/**
 * @see com.eimsound.audiosources.DefaultResampledAudioSource
 */
interface ResampledAudioSource : AudioSource {
    @Transient
    var factor: Double
    override val factory: ResampledAudioSourceFactory<*>
}

/**
 * @see com.eimsound.audiosources.DefaultMemoryAudioSource
 */
interface MemoryAudioSource : AudioSource, AutoCloseable

@Serializable(with = AudioSourceFactoryNameSerializer::class)
interface AudioSourceFactory <T: AudioSource> {
    val name: String
    fun createAudioSource(source: AudioSource? = null, json: JsonObject? = null): T
}

/**
 * @see com.eimsound.audiosources.DefaultFileAudioSourceFactory
 */
interface FileAudioSourceFactory <T: FileAudioSource> : AudioSourceFactory<T> {
    val supportedFormats: List<String>
    fun createAudioSource(file: Path): T
}

/**
 * @see com.eimsound.audiosources.DefaultResampledAudioSourceFactory
 */
interface ResampledAudioSourceFactory <T: ResampledAudioSource> : AudioSourceFactory<T>

/**
 * @see com.eimsound.audiosources.DefaultMemoryAudioSourceFactory
 */
interface MemoryAudioSourceFactory <T: MemoryAudioSource> : AudioSourceFactory<T>

/**
 * @see com.eimsound.audioprocessor.impl.AudioSourceManagerImpl
 */
interface AudioSourceManager : Reloadable {
    companion object {
        val instance by lazy { ServiceLoader.load(AudioSourceManager::class.java).first()!! }
    }
    val factories: Map<String, AudioSourceFactory<*>>
    val supportedFormats: Set<String>
    fun createAudioSource(factory: String, source: AudioSource? = null): AudioSource
    fun createAudioSource(json: JsonObject): AudioSource
    fun createAudioSource(file: Path, factory: String? = null): FileAudioSource
    fun createAutoWrappedAudioSource(file: Path): AudioSource
    fun createMemorySource(source: AudioSource, factory: String? = null): MemoryAudioSource
    fun createResampledSource(source: AudioSource, factory: String? = null): ResampledAudioSource
}

object AudioSourceFactoryNameSerializer : KSerializer<AudioSourceFactory<*>> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor
    override fun deserialize(decoder: Decoder): AudioSourceFactory<*> {
        val name = decoder.decodeString()
        return AudioSourceManager.instance.factories[name] ?: throw NoSuchFactoryException(name)
    }
    override fun serialize(encoder: Encoder, value: AudioSourceFactory<*>) { encoder.encodeString(value.name) }
}

fun AudioSource.close() {
    var source: AudioSource? = source
    while (source != null) {
        if (source is AutoCloseable) source.close()
        source = source.source
    }
}

val AudioSource.timeInSeconds get() = length / sampleRate
