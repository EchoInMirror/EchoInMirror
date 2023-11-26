package com.eimsound.audiosources

import com.eimsound.daw.commons.json.JsonSerializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject
import java.nio.file.Path

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
    fun copy(): AudioSource
}

/**
 * @see com.eimsound.audiosources.impl.DefaultFileAudioSource
 */
interface FileAudioSource : AudioSource, AutoCloseable {
    val file: Path
    @Transient
    val isRandomAccessible: Boolean
    override val source: Nothing?
    override val factory: FileAudioSourceFactory<*>
    override fun copy(): FileAudioSource
}

/**
 * @see com.eimsound.audiosources.impl.DefaultResampledAudioSource
 */
interface ResampledAudioSource : AudioSource {
    @Transient
    var factor: Double
    override val factory: ResampledAudioSourceFactory<*>
    override fun copy(): ResampledAudioSource
}

/**
 * @see com.eimsound.audiosources.impl.DefaultMemoryAudioSource
 */
interface MemoryAudioSource : AudioSource, AutoCloseable {
    override val factory: MemoryAudioSourceFactory<*>
    override fun copy(): MemoryAudioSource
}

interface AudioSourceFactory <T: AudioSource> {
    val name: String
    fun createAudioSource(source: AudioSource? = null, json: JsonObject? = null): T
}

/**
 * @see com.eimsound.audiosources.impl.DefaultFileAudioSourceFactory
 */
interface FileAudioSourceFactory <T: FileAudioSource> : AudioSourceFactory<T> {
    val supportedFormats: List<String>
    fun createAudioSource(file: Path): T
}

/**
 * @see com.eimsound.audiosources.impl.DefaultResampledAudioSourceFactory
 */
interface ResampledAudioSourceFactory <T: ResampledAudioSource> : AudioSourceFactory<T>

/**
 * @see com.eimsound.audiosources.impl.DefaultMemoryAudioSourceFactory
 */
interface MemoryAudioSourceFactory <T: MemoryAudioSource> : AudioSourceFactory<T>

fun AudioSource.close() {
    var source: AudioSource? = this
    while (source != null) {
        if (source is AutoCloseable) source.close()
        source = source.source
    }
}

val AudioSource.timeInSeconds get() = length / sampleRate
