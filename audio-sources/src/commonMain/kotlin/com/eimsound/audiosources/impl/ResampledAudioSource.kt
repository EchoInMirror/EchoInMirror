package com.eimsound.audiosources.impl

import be.tarsos.dsp.resample.Resampler
import com.eimsound.audiosources.*
import kotlin.math.*

class DefaultResampledAudioSource(val source: AudioSource, override var factor: Double = 1.0): ResampledAudioSource {
    override val channels = source.channels
    override val sampleRate get() = (source.sampleRate * factor).toFloat()
    override val isClosed get() = source.isClosed
    override val length get() = (source.length / factor).roundToLong()
    override val isRandomAccessible = source.isRandomAccessible
    override var position
        get() = (source.position / factor).roundToLong()
        set(value) {
            if (value < 0 || !isRandomAccessible) return
            source.position = (value * factor).roundToLong()
        }
    private val resamplers = Array(channels) { Resampler(true, 0.1, 4.0) }
    private var sourceBuffers = Array(channels) { FloatArray(1024) }

    override fun nextBlock(buffers: Array<FloatArray>, length: Int, offset: Int): Int {
        var len = length.coerceAtMost(buffers.firstOrNull()?.size ?: 0)
        if (factor == 1.0) return source.nextBlock(buffers, len, offset)
        val sourceLength = (len / factor).roundToInt()
        val ch = channels.coerceAtMost(buffers.size)
        if (sourceBuffers[0].size < sourceLength || sourceBuffers.size < ch)
            sourceBuffers = Array(ch) { FloatArray(sourceLength) }
        len = source.nextBlock(sourceBuffers, sourceLength)
        var consumed = 0
        val isLast = source.position + sourceLength >= source.length
        repeat(ch) {
            consumed = resamplers[it].process(
                factor, sourceBuffers[it], 0, sourceLength,
                isLast, buffers[it], offset, len
            ).outputSamplesGenerated
        }
        return consumed
    }

    override fun copy() = DefaultResampledAudioSource(source.copy(), factor)
    override fun close() { source.close() }

    override fun toString() =
        "DefaultResampledAudioSource(source=$source, factor=$factor, channels=$channels, sampleRate=$sampleRate, length=$length)"
}

class DefaultResampledAudioSourceFactory: ResampledAudioSourceFactory {
    override val name = "Resampled"
    override fun createAudioSource(source: AudioSource) = DefaultResampledAudioSource(source)
}
