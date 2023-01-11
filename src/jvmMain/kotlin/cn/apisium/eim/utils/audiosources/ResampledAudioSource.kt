package cn.apisium.eim.utils.audiosources

import be.tarsos.dsp.resample.Resampler
import kotlin.math.roundToInt

interface ResampledAudioSource : AudioSource {
    val source: AudioSource
    var factor: Double
}

//class DefaultResampledAudioSource(override val source: AudioSource, override var factor: Double = 1.0): ResampledAudioSource {
//    override val channels get() = source.channels
//    override val sampleRate get() = (source.sampleRate * factor).toFloat()
//    override val length get() = (source.length * factor).toLong()
//    private val resampler = Resampler(false, 0.1, 4.0)
//
//    override fun getSamples(start: Int, buffers: Array<FloatArray>) {
//        if (factor == 1.0) {
//            source.getSamples(start, buffers)
//            return
//        }
//        val sourceStart = (start / factor).roundToInt()
//        val sourceLength = (buffers[0].size / factor).roundToInt()
//        val sourceBuffers = Array(channels) { FloatArray(sourceLength) }
//        source.getSamples(sourceStart, sourceBuffers)
//        for (i in 0 until channels.coerceAtMost(sourceBuffers.size)) {
//            resampler.process(factor, sourceBuffers[i], 0, sourceLength, false, buffers[i], 0, buffers[i].size)
//        }
//    }
//}

class DefaultResampledAudioSource(override val source: AudioSource, override var factor: Double = 1.0): ResampledAudioSource {
    override val channels get() = source.channels
    override val sampleRate get() = (source.sampleRate * factor).toFloat()
    override val length get() = (source.length * factor).toLong()
    private val resamplers = Array(channels) { Resampler(false, 0.1, 4.0) }

    override fun getSamples(start: Int, buffers: Array<FloatArray>) {
        if (factor == 1.0) {
            source.getSamples(start, buffers)
            return
        }
        val sourceStart = (start / factor).roundToInt()
        val sourceLength = (buffers[0].size / factor).roundToInt()
        val sourceBuffers = Array(channels) { FloatArray(sourceLength) }
        source.getSamples(sourceStart, sourceBuffers)
        for (i in 0 until channels.coerceAtMost(sourceBuffers.size)) {
            resamplers[i].process(factor, sourceBuffers[i], 0, sourceLength, false, buffers[i], 0, buffers[i].size)
        }
    }
}
