package com.eimsound.audiosources.impl

import com.eimsound.audiosources.AudioSource
import com.eimsound.audiosources.MemoryAudioSource
import com.eimsound.audiosources.MemoryAudioSourceFactory
import com.eimsound.audiosources.close
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class DefaultMemoryAudioSource(
    override val factory: MemoryAudioSourceFactory<MemoryAudioSource>,
    private var samplesBuffer: Array<FloatArray>,
    override val sampleRate: Float
): MemoryAudioSource {
    override val source: AudioSource? = null
    override val length get() = samplesBuffer.firstOrNull()?.size?.toLong() ?: 0
    override val channels get() = samplesBuffer.size

    constructor(factory: MemoryAudioSourceFactory<MemoryAudioSource>, source: AudioSource):
            this(factory, source.run {
                val len = source.length.toInt()
                val sampleBuffer = Array(source.channels) { FloatArray(len) }
                source.getSamples(0, 0, len, sampleBuffer)
                source.close()
                sampleBuffer
            }, source.sampleRate)

    override fun getSamples(start: Long, offset: Int, length: Int, buffers: Array<FloatArray>): Int {
        val len = this.length
        if (start > len || start < 0) return 0
        var consumed = 0
        for (i in 0 until channels.coerceAtMost(buffers.size)) {
            consumed = length.coerceAtMost((len - start).toInt())
            samplesBuffer[i].copyInto(buffers[i], 0, start.toInt(), (start + consumed).toInt())
        }
        return consumed
    }

    override fun close() { samplesBuffer = emptyArray() }

    override fun copy() = DefaultMemoryAudioSource(factory, samplesBuffer, sampleRate)

    override fun toString(): String {
        return "DefaultMemoryAudioSource(source=$source, length=$length, channels=$channels, sampleRate=$sampleRate)"
    }

    override fun toJson() = source?.toJson() ?: throw IllegalStateException("No source")

    override fun fromJson(json: JsonElement) = throw UnsupportedOperationException()
}

class DefaultMemoryAudioSourceFactory: MemoryAudioSourceFactory<MemoryAudioSource> {
    override val name = "Memory"

    override fun createAudioSource(source: AudioSource?, json: JsonObject?) = DefaultMemoryAudioSource(this, source!!)
}
