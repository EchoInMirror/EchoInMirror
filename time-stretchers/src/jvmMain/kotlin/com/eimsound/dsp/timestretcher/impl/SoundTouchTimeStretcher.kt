package com.eimsound.dsp.timestretcher.impl

import com.eimsound.dsp.timestretcher.TimeStretcher
import com.tianscar.soundtouch.SoundTouch
import kotlin.math.exp
import kotlin.math.roundToInt

class SoundTouchTimeStretcher : TimeStretcher {
    private val soundTouch = SoundTouch()
    private var numChannels = 0
    private var samplesPerBlock = 0
    private val inputOutputSampleRatio get(): Double {
//        val virtualPitch = exp(0.69314718056 * (semitones / 12.0))
//        return 1.0 / (((1 / speedRatio) / virtualPitch) * virtualPitch)
        return speedRatio
    }
    override var speedRatio: Double = 1.0
        set(value) {
            field = value
            soundTouch.setTempo(1 / value.toFloat())
        }
    override var semitones: Double = 0.0
        set(value) {
            field = value
            soundTouch.setPitchSemiTones(value.toFloat())
        }
    override val maxFramesNeeded = 8192
    override val framesNeeded: Int
        get() {
            val numAvailable = soundTouch.numSamples()
            val numRequiredForOneBlock = (samplesPerBlock * inputOutputSampleRatio)

            return (numRequiredForOneBlock - numAvailable).roundToInt().coerceAtLeast(0)
        }

    private var tempBuffer = FloatArray(0)

    override fun initialise(sourceSampleRate: Double, samplesPerBlock: Int, numChannels: Int, isRealtime: Boolean) {
        this.numChannels = numChannels
        this.samplesPerBlock = samplesPerBlock
        soundTouch.setSampleRate(sourceSampleRate.toLong())
        soundTouch.setChannels(numChannels.toLong())
        if (tempBuffer.size != samplesPerBlock * numChannels) tempBuffer = FloatArray(samplesPerBlock * numChannels)
    }

    override fun process(input: Array<FloatArray>, output: Array<FloatArray>): Int {
        val size = input[0].size
        repeat(numChannels) { i ->
            val buf = input[i]
            buf.copyInto(tempBuffer, i * size, 0, size)
        }
        soundTouch.putSamples(tempBuffer, size)
        val numAvailable = soundTouch.numSamples().toInt()
        val numToRead = samplesPerBlock.coerceAtMost(numAvailable)
        return if (numToRead > 0) soundTouch.receiveSamples(tempBuffer, numToRead)
        else 0
    }

    override fun reset() {
        soundTouch.clear()
    }

    override fun flush(output: Array<FloatArray>) {
        soundTouch.flush()
    }

    override fun close() {
        soundTouch.dispose()
    }
}