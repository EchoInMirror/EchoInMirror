package com.eimsound.audiosources

import be.tarsos.dsp.resample.Resampler
import com.eimsound.audioprocessor.AudioSource
import com.eimsound.audioprocessor.ResampledAudioSource
import com.eimsound.audioprocessor.ResampledAudioSourceFactory
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class DefaultResampledAudioSource(
    override val factory: ResampledAudioSourceFactory<ResampledAudioSource>,
    override val source: AudioSource, override var factor: Double = 1.0
): ResampledAudioSource {
    override val channels get() = source.channels
    override val sampleRate get() = (source.sampleRate * factor).toFloat()
    override val length get() = (source.length * factor).toLong()
    private val resamplers = Array(channels) { Resampler(false, 0.1, 4.0) }
    private var nextStart = 0L
    private var sourceBuffers = Array(channels) { FloatArray(1024) }

    override fun getSamples(start: Long, length: Int, buffers: Array<FloatArray>): Int {
        if (factor == 1.0) {
            source.getSamples(start, length, buffers)
            return buffers[0].size
        }
        var sourceStart = (start / factor).roundToLong()
        if (nextStart - 1 == sourceStart) sourceStart = nextStart
        val sourceLength = (buffers[0].size / factor).roundToInt()
        nextStart = sourceStart + sourceLength
        val ch = channels.coerceAtMost(buffers.size)
        if (sourceBuffers[0].size < sourceLength || sourceBuffers.size < ch)
            sourceBuffers = Array(ch) { FloatArray(sourceLength) }
        source.getSamples(sourceStart, sourceLength, sourceBuffers)
        var consumed = 0
        repeat(ch) {
            consumed = resamplers[it].process(factor, sourceBuffers[it], 0, sourceLength, false,
                buffers[it], 0, buffers[it].size).outputSamplesGenerated
        }
        return consumed
    }

    override fun toJson() = buildJsonObject {
        put("factory", factory.name)
        put("source", source.toJson())
    }
    override fun fromJson(json: JsonElement) = throw UnsupportedOperationException()

    override fun toString(): String {
        return "DefaultResampledAudioSource(source=$source, factor=$factor, channels=$channels, sampleRate=$sampleRate, length=$length)"
    }
}

class DefaultResampledAudioSourceFactory: ResampledAudioSourceFactory<ResampledAudioSource> {
    override val name = "Resampled"
    override fun createAudioSource(source: AudioSource?, json: JsonObject?) =
        DefaultResampledAudioSource(this, source!!)
}
