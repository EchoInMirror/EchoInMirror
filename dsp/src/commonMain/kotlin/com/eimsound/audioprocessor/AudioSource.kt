package com.eimsound.audioprocessor

import java.nio.file.Path

interface AudioSource {
    val name: String
    val source: AudioSource?
    val sampleRate: Float
    val channels: Int
    val length: Long // timeInSamples
    fun getSamples(start: Long, buffers: Array<FloatArray>): Int
}

interface FileAudioSource : AudioSource, AutoCloseable {
    val file: Path
    val isRandomAccessible: Boolean
}

interface ResampledAudioSource : AudioSource {
    var factor: Double
}

class EmptyAudioSource : AudioSource {
    override val name: String = "Empty"
    override val source: AudioSource? = null
    override val sampleRate = 44100F
    override val channels = 2
    override val length = 0L
    override fun getSamples(start: Long, buffers: Array<FloatArray>) = 0
}

//interface AudioSourceManager {
//    fun registerAudioSource(name: String, source: AudioSource)
//    fun createAudioSource(name: String): AudioSource
//    fun createAudioSource(file: File): AudioSource
//}
