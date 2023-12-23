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
        set(value) { field = value.coerceIn(0, length) }

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
        if (isClosed || len < 1 || length < 1 || start >= len) return 0
        val arrLen = buffers.firstOrNull()?.size ?: 0
        val destLen = length.coerceAtMost(arrLen)
        if (offset > arrLen) return 0
        val startIndex = start.coerceAtLeast(0)
        val consumed = destLen.coerceAtMost((len - startIndex).toInt()).coerceAtMost(arrLen - offset)
        if (consumed < 1) return 0
        val end = startIndex + consumed

        repeat(channels.coerceAtMost(buffers.size)) {
            samplesBuffer[it].copyInto(buffers[it], offset, startIndex, end)
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
