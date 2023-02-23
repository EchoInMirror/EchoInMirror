package com.eimsound.audiosources

import com.eimsound.audioprocessor.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DefaultMemoryAudioSource(override val factory: AudioSourceFactory<MemoryAudioSource>, override val source: AudioSource):
    MemoryAudioSource {
    override val length get() = source.length
    override val channels get() = source.channels
    override val sampleRate get() = source.sampleRate
    private var sampleBuffer = Array(channels) { FloatArray(length.toInt()) }

    init {
        source.getSamples(0, sampleBuffer)
        source.close()
    }

    override fun getSamples(start: Long, buffers: Array<FloatArray>): Int {
        val len = length
        if (start > len || start < 0) return 0
        var consumed = 0
        for (i in 0 until channels.coerceAtMost(buffers.size)) {
            val buf = sampleBuffer[i]
            consumed = buffers[i].size.coerceAtMost((len - start).toInt())
            System.arraycopy(buf, start.toInt(), buffers[i], 0, consumed)
        }
        return consumed
    }

    override fun close() { sampleBuffer = emptyArray() }

    override fun toString(): String {
        return "DefaultMemoryAudioSource(source=$source, length=$length, channels=$channels, sampleRate=$sampleRate)"
    }

    override fun toJson() = buildJsonObject {
        put("factory", factory.name)
        put("source", source.toJson())
    }

    override fun fromJson(json: JsonElement) = throw UnsupportedOperationException()
}

class DefaultMemoryAudioSourceFactory: MemoryAudioSourceFactory<MemoryAudioSource> {
    override val name = "Memory"

    override fun createAudioSource(source: AudioSource?, json: JsonObject?) = DefaultMemoryAudioSource(this, source!!)
}
