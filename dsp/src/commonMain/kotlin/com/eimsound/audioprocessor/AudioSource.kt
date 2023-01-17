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
    val isRandomAccessable: Boolean
}

interface ResampledAudioSource : AudioSource {
    var factor: Double
}

//interface AudioSourceManager {
//    fun registerAudioSource(name: String, source: AudioSource)
//    fun createAudioSource(name: String): AudioSource
//    fun createAudioSource(file: File): AudioSource
//}
