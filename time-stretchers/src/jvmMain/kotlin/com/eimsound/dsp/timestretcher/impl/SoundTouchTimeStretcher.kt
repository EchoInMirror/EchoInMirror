package com.eimsound.dsp.timestretcher.impl

import com.eimsound.dsp.timestretcher.AbstractTimeStretcher
import com.tianscar.soundtouch.SoundTouch
//import kotlin.math.exp
import kotlin.math.roundToInt

class SoundTouchTimeStretcher : AbstractTimeStretcher() {
    private val soundTouch = SoundTouch()
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

    override fun initialise(sourceSampleRate: Float, samplesPerBlock: Int, numChannels: Int, isRealtime: Boolean) {
        soundTouch.setSampleRate(sourceSampleRate.toLong())
        soundTouch.setChannels(numChannels.toLong())
        if (tempBuffer.size != samplesPerBlock * numChannels) tempBuffer = FloatArray(samplesPerBlock * numChannels)
        super.initialise(sourceSampleRate, samplesPerBlock, numChannels, isRealtime)
    }

    override fun process(input: Array<FloatArray>, output: Array<FloatArray>): Int {
        if (!isInitialised) return 0
        val size = input[0].size
        repeat(numChannels) { i ->
            val buf = input[i]
            buf.copyInto(tempBuffer, i * size, 0, size)
        }
        soundTouch.putSamples(tempBuffer, size)
        return readOutput(output)
    }

    private fun readOutput(output: Array<FloatArray>): Int {
        val numToRead = samplesPerBlock.coerceAtMost(soundTouch.numSamples().toInt()).coerceAtMost(output[0].size)
        var numRead = 0
        if (numToRead > 0) {
            println(numToRead)
            numRead = soundTouch.receiveSamples(FloatArray(numToRead * 20), 10)
            repeat(numChannels) { i ->
                tempBuffer.copyInto(output[i], 0, i * numRead, (i + 1) * numRead)
            }
        }
        return numRead
    }

    override fun reset() {
        soundTouch.clear()
    }

    override fun flush(output: Array<FloatArray>): Int {
        soundTouch.flush()
        return readOutput(output)
    }

    override fun close() {
        soundTouch.dispose()
    }
}