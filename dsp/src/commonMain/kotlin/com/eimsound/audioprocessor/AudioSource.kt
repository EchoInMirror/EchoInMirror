package com.eimsound.audioprocessor

import com.eimsound.daw.utils.JsonMutableObjectSerializable
import com.eimsound.daw.utils.NoSuchFactoryException
import com.eimsound.daw.utils.Reloadable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.file.Path
import java.util.*
import kotlin.io.path.pathString

interface AudioSource : JsonMutableObjectSerializable {
    val source: AudioSource?
    @Transient
    val sampleRate: Float
    @Transient
    val channels: Int
    @Transient
    val length: Long // timeInSamples
    val factory: AudioSourceFactory<*>
    fun getSamples(start: Long, buffers: Array<FloatArray>): Int

    override fun toJson(): MutableMap<String, Any> = hashMapOf<String, Any>(
        "factory" to factory.name,
    ).apply {
        if (source != null) put("source", source!!.toJson())
    }
}

/**
 * @see com.eimsound.audiosources.DefaultFileAudioSource
 */
interface FileAudioSource : AudioSource, AutoCloseable {
    val file: Path
    @Transient
    val isRandomAccessible: Boolean
    override val factory: FileAudioSourceFactory<*>

    override fun toJson() = super.toJson().apply {
        put("file", file.pathString)
    }
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

@JsonSerialize(using = AudioSourceFactoryNameSerializer::class)
interface AudioSourceFactory <T: AudioSource> {
    val name: String
    fun createAudioSource(source: AudioSource? = null, json: JsonNode? = null): T
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
    fun createAudioSource(json: JsonNode): AudioSource
    fun createAudioSource(file: Path, factory: String? = null): FileAudioSource
    fun createAutoWrappedAudioSource(file: Path): AudioSource
    fun createMemorySource(source: AudioSource, factory: String? = null): MemoryAudioSource
    fun createResampledSource(source: AudioSource, factory: String? = null): ResampledAudioSource
}

object AudioSourceFactoryNameSerializer : KSerializer<AudioSourceFactory<*>> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor
    override fun deserialize(decoder: Decoder) {
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
