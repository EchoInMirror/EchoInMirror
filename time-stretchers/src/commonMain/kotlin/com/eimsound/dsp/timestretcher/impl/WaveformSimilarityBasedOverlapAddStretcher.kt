package com.eimsound.dsp.timestretcher.impl

import com.eimsound.dsp.timestretcher.AbstractTimeStretcher
import be.tarsos.dsp.resample.Resampler
import com.eimsound.dsp.timestretcher.dsp.FIFOAudioBuffer
import com.eimsound.dsp.timestretcher.dsp.WaveformSimilarityBasedOverlapAdd
import com.eimsound.dsp.timestretcher.semitonesToRatio
import kotlin.math.ceil

@Suppress("unused")
class WaveformSimilarityBasedOverlapAddStretcher : AbstractTimeStretcher() {
    private var channels = 0
    private var timeStretchers: Array<WaveformSimilarityBasedOverlapAdd> = emptyArray()
    private var resamplers: Array<Resampler> = emptyArray()
    private var pitchRatio = 1.0
    private var queues: Array<FIFOAudioBuffer> = emptyArray()
    private var tempBuffer = FloatArray(0)
    private var tempOutputBuffer = FloatArray(0)

    override var speedRatio: Double = 1.0
        set(value) = timeStretchers.forEach { it.setSpeedRatio(value) }
    override var semitones: Double = 0.0
        set(value) {
            field = value
            pitchRatio = semitonesToRatio(value)
        }

    override val maxFramesNeeded = 40960
    override val framesNeeded get() = timeStretchers[0].run {
        if (inputBufferSize != tempBuffer.size) tempBuffer = FloatArray(inputBufferSize)
//        var outputSize = samplesPerBlock
        if (outputBufferSize != tempOutputBuffer.size) tempOutputBuffer = FloatArray(outputBufferSize)
        inputBufferSize * ceil(samplesPerBlock / (outputBufferSize / pitchRatio)).toInt()
    }

    override fun initialise(sourceSampleRate: Float, samplesPerBlock: Int, numChannels: Int, isRealtime: Boolean) {
        super.initialise(sourceSampleRate, samplesPerBlock, numChannels, isRealtime)
        if (channels != numChannels) {
            channels = numChannels
            timeStretchers = Array(numChannels) { WaveformSimilarityBasedOverlapAdd() }
            resamplers = Array(numChannels) { Resampler(true, 0.1, 4.0) }
            queues = Array(numChannels) { FIFOAudioBuffer(maxFramesNeeded) }
        }
        timeStretchers.forEach { it.applyNewParameters(sourceSampleRate) }
    }

    override fun process(input: Array<FloatArray>, output: Array<FloatArray>): Int {
//        val framesNeeded = framesNeeded

        val timeStretcher = timeStretchers[0]
        val times = ceil(samplesPerBlock / (timeStretcher.outputBufferSize / pitchRatio)).toInt()
        repeat(channels) {
//            val resampler = resamplers[it]
            val inputBuffer = input[it]
            val outputBuffer = output[it]
            val queue = queues[it]
//            queue.push(timeStretcher.process(inputBuffer))
//            resampler.process(pitchRatio, outputBuffer, framesNeeded)
            repeat(times) { time ->
                inputBuffer.copyInto(tempBuffer, 0, time * timeStretcher.outputBufferSize, (time + 1) * timeStretcher.outputBufferSize)
                queue.push(timeStretcher.process(tempBuffer))
            }
            queue.pop(outputBuffer)
//            resampler.process(pitchRatio, outputBuffer, framesNeeded)
        }
        return 0
    }

    override fun flush(output: Array<FloatArray>) = 0

    override fun reset() {
    }

    override fun close() {
        timeStretchers = emptyArray()
        resamplers = emptyArray()
        queues = emptyArray()
    }
}