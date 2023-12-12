package com.eimsound.audiosources.impl

import com.eimsound.audiosources.AudioSource
import com.eimsound.audiosources.MemoryAudioSource
import com.eimsound.audiosources.MemoryAudioSourceFactory

class DefaultMemoryAudioSource(
    private var samplesBuffer: Array<FloatArray>, override val sampleRate: Float
): MemoryAudioSource {
    override var isClosed = false
        private set
    override val length get() = samplesBuffer.firstOrNull()?.size?.toLong() ?: 0L
    override val isRandomAccessible = true
    override val channels get() = samplesBuffer.size
    override var position = 0L
        set(value) { field = value.coerceIn(0, length - 1) }

    constructor(source: AudioSource):
            this(source.run {
                val len = source.length.toInt()
                val sampleBuffer = Array(source.channels) { FloatArray(len) }
                source.nextBlock(sampleBuffer, len)
                source.close()
                sampleBuffer
            }, source.sampleRate)

    override fun getSamples(buffers: Array<FloatArray>, start: Int, length: Int, offset: Int): Int {
        val len = this.length
        if (isClosed || len < 1 || length < 1 || start > len - 1) return 0
        var consumed = 0
        val s = start.coerceAtLeast(0)

        for (i in 0 until channels.coerceAtMost(buffers.size)) {
            consumed = length.coerceAtMost((len - s).toInt())
            if (consumed < 1) break
            samplesBuffer[i].copyInto(buffers[i], offset, s, s + consumed)
        }
        return consumed
    }

    override fun close() {
        samplesBuffer = emptyArray()
        isClosed = true
    }

    override fun copy() = DefaultMemoryAudioSource(samplesBuffer, sampleRate)

    override fun toString(): String {
        return "DefaultMemoryAudioSource(length=$length, channels=$channels, sampleRate=$sampleRate)"
    }
}

class DefaultMemoryAudioSourceFactory: MemoryAudioSourceFactory {
    override val name = "Memory"

    override fun createAudioSource(buffers: Array<FloatArray>, sampleRate: Float) =
        DefaultMemoryAudioSource(buffers, sampleRate)
    override fun createAudioSource(source: AudioSource) = DefaultMemoryAudioSource(source)
}
