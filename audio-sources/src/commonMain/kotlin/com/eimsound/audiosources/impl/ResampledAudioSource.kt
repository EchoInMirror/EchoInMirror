package com.eimsound.audiosources.impl

import be.tarsos.dsp.resample.Resampler
import com.eimsound.audiosources.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.*

class DefaultResampledAudioSource(
    override val factory: ResampledAudioSourceFactory<ResampledAudioSource>,
    override val source: AudioSource, override var resampleFactor: Double = 1.0,
    override var timeStretchFactor: Double = 1.0
): ResampledAudioSource {
    override val channels get() = source.channels
    override val sampleRate get() = (source.sampleRate * resampleFactor).toFloat()
    override val length get() = (source.length * timeStretchFactor / resampleFactor).roundToLong()
    private val resamplers = Array(channels) { Resampler(true, 0.1, 4.0) }
    private var nextStart = 0L
    private var sourceBuffers = Array(channels) { FloatArray(1024) }

    override fun getSamples(start: Long, length: Int, buffers: Array<FloatArray>): Int {
        if (resampleFactor == 1.0 && timeStretchFactor == 1.0) {
            source.getSamples(start, length, buffers)
            return buffers[0].size
        }
        var sourceStart = (start * resampleFactor / timeStretchFactor).roundToLong()
        if (nextStart - 1 == sourceStart) sourceStart = nextStart
        val sourceLength = (buffers[0].size * timeStretchFactor / resampleFactor).roundToInt()
        nextStart = sourceStart + sourceLength
        val ch = channels.coerceAtMost(buffers.size)
        if (sourceBuffers[0].size < sourceLength || sourceBuffers.size < ch)
            sourceBuffers = Array(ch) { FloatArray(sourceLength) }
        source.getSamples(sourceStart, sourceLength, sourceBuffers)
        var consumed = 0
        repeat(ch) {
            consumed = resamplers[it].process(resampleFactor, sourceBuffers[it], 0, sourceLength, false,
                buffers[it], 0, buffers[it].size).outputSamplesGenerated
        }
        return consumed
    }

    override fun toJson() = buildJsonObject {
        put("factory", factory.name)
        put("source", source.toJson())
    }
    override fun fromJson(json: JsonElement) = throw UnsupportedOperationException()
    override fun copy() = DefaultResampledAudioSource(factory, source.copy(), resampleFactor, timeStretchFactor)

    override fun toString() =
        "DefaultResampledAudioSource(source=$source, resampleFactor=$resampleFactor, " +
                "timeStretchFactor=$timeStretchFactor, channels=$channels, sampleRate=$sampleRate, length=$length)"
}

class DefaultResampledAudioSourceFactory: ResampledAudioSourceFactory<ResampledAudioSource> {
    override val name = "Resampled"
    override fun createAudioSource(source: AudioSource?, json: JsonObject?) =
        DefaultResampledAudioSource(this, source!!)
}
