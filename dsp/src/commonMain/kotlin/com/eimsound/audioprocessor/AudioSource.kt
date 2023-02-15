package com.eimsound.audioprocessor

import com.eimsound.daw.utils.Reloadable
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.nio.file.Path
import java.util.*

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE
)
interface AudioSource {
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    val source: AudioSource?
    val sampleRate: Float
    val channels: Int
    val length: Long // timeInSamples
    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    val factory: AudioSourceFactory<*>
    fun getSamples(start: Long, buffers: Array<FloatArray>): Int
}

/**
 * @see com.eimsound.audiosources.DefaultFileAudioSource
 */
interface FileAudioSource : AudioSource, AutoCloseable {
    @get:JsonProperty
    val file: Path
    val isRandomAccessible: Boolean
    override val factory: FileAudioSourceFactory<*>
}

/**
 * @see com.eimsound.audiosources.DefaultResampledAudioSource
 */
interface ResampledAudioSource : AudioSource {
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

class AudioSourceFactoryNameSerializer @JvmOverloads constructor(t: Class<AudioSourceFactory<*>>? = null) :
    StdSerializer<AudioSourceFactory<*>>(t) {
    override fun serialize(value: AudioSourceFactory<*>, jgen: JsonGenerator, provider: SerializerProvider?) {
        jgen.writeString(value.name)
    }
}

fun AudioSource.close() {
    var source: AudioSource? = source
    while (source != null) {
        if (source is AutoCloseable) source.close()
        source = source.source
    }
}
