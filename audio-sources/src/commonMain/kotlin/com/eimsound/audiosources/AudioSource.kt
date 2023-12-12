package com.eimsound.audiosources

import java.nio.file.Path

interface AudioSource : AutoCloseable {
    val sampleRate: Float
    val channels: Int
    val length: Long // timeInSamples
    val isRandomAccessible: Boolean
    val isClosed: Boolean
    var position: Long

    fun nextBlock(buffers: Array<FloatArray>, length: Int = buffers.firstOrNull()?.size ?: 0, offset: Int = 0): Int
    fun copy(): AudioSource
}

val AudioSource.fileSize get() = length * channels * 4

/**
 * @see com.eimsound.audiosources.impl.DefaultFileAudioSource
 */
interface FileAudioSource : AudioSource, AutoCloseable {
    val file: Path
    override fun copy(): FileAudioSource
}

/**
 * @see com.eimsound.audiosources.impl.DefaultResampledAudioSource
 */
interface ResampledAudioSource : AudioSource {
    var factor: Double
    override fun copy(): ResampledAudioSource
}

/**
 * @see com.eimsound.audiosources.impl.DefaultMemoryAudioSource
 */
interface MemoryAudioSource : AudioSource, AutoCloseable {
    override fun copy(): MemoryAudioSource

    override fun nextBlock(buffers: Array<FloatArray>, length: Int, offset: Int) =
        getSamples(buffers, position.toInt(), length, offset).also { position += it }
    fun getSamples(
        buffers: Array<FloatArray>, start: Int, length: Int = buffers.firstOrNull()?.size ?: 0, offset: Int = 0
    ): Int
}

interface AudioSourceFactory { val name: String }

/**
 * @see com.eimsound.audiosources.impl.DefaultFileAudioSourceFactory
 */
interface FileAudioSourceFactory : AudioSourceFactory {
    val supportedFormats: List<String>
    fun createAudioSource(file: Path): FileAudioSource
}

/**
 * @see com.eimsound.audiosources.impl.DefaultResampledAudioSourceFactory
 */
interface ResampledAudioSourceFactory : AudioSourceFactory {
    fun createAudioSource(source: AudioSource): ResampledAudioSource
}

/**
 * @see com.eimsound.audiosources.impl.DefaultMemoryAudioSourceFactory
 */
interface MemoryAudioSourceFactory : AudioSourceFactory {
    fun createAudioSource(source: AudioSource): MemoryAudioSource
    fun createAudioSource(buffers: Array<FloatArray>, sampleRate: Float): MemoryAudioSource
}

val AudioSource.timeInSeconds get() = length / sampleRate
