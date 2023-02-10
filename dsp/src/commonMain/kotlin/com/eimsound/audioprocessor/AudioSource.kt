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
import java.io.File
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

interface FileAudioSource : AudioSource, AutoCloseable {
    @get:JsonProperty
    val file: Path
    val isRandomAccessible: Boolean
    override val factory: FileAudioSourceFactory<*>
}

interface ResampledAudioSource : AudioSource {
    var factor: Double
    override val factory: ResampledSourceFactory<*>
}

@JsonSerialize(using = AudioSourceFactoryNameSerializer::class)
interface AudioSourceFactory <T: AudioSource> {
    val name: String
    fun createAudioSource(source: AudioSource? = null, json: JsonNode? = null): T
}

interface FileAudioSourceFactory <T: FileAudioSource> : AudioSourceFactory<T> {
    val supportedFormats: List<String>
    fun createAudioSource(file: File): T
}

interface ResampledSourceFactory <T: ResampledAudioSource> : AudioSourceFactory<T> {
    fun createAudioSource(source: AudioSource): T
}

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
    fun createAudioSource(file: File, factory: String? = null): FileAudioSource
    fun createResampledSource(source: AudioSource, factory: String? = null): ResampledAudioSource
}

class AudioSourceFactoryNameSerializer @JvmOverloads constructor(t: Class<AudioSourceFactory<*>>? = null) :
    StdSerializer<AudioSourceFactory<*>>(t) {
    override fun serialize(value: AudioSourceFactory<*>, jgen: JsonGenerator, provider: SerializerProvider?) {
        jgen.writeString(value.name)
    }
}
