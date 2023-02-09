package com.eimsound.audiosources

import com.eimsound.audioprocessor.AudioSource
import com.eimsound.audioprocessor.AudioSourceFactory
import com.fasterxml.jackson.databind.JsonNode

class MemoryAudioSource(override val factory: AudioSourceFactory<MemoryAudioSource>, override val source: AudioSource): AudioSource {
    override val length get() = source.length
    override val channels get() = source.channels
    override val sampleRate get() = source.sampleRate
    private var sampleBuffer: Array<FloatArray> = Array(channels) { FloatArray(length.toInt()) }

    init {
        source.getSamples(0, sampleBuffer)
        if (source is AutoCloseable) source.close()
    }

    override fun getSamples(start: Long, buffers: Array<FloatArray>): Int {
        val len = length
        if (start > len) return 0
        var consumed = 0
        for (i in 0 until channels.coerceAtMost(buffers.size)) {
            val buf = sampleBuffer[i]
            consumed = buffers[i].size.coerceAtMost((len - start).toInt())
            System.arraycopy(buf, start.toInt(), buffers[i], 0, consumed)
        }
        return consumed
    }
}

class MemoryAudioSourceFactory: AudioSourceFactory<MemoryAudioSource> {
    override val name = "Memory"
    override fun createAudioSource(source: AudioSource?, json: JsonNode?) = MemoryAudioSource(this, source!!)
}
