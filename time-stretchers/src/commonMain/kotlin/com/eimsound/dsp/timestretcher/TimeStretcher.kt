package com.eimsound.dsp.timestretcher

import kotlin.math.pow

interface TimeStretcher : AutoCloseable {
    var speedRatio: Double
    var semitones: Double
    val maxFramesNeeded: Int
    val framesNeeded: Int

    fun initialise(sourceSampleRate: Double, samplesPerBlock: Int, numChannels: Int, isRealtime: Boolean = true)
    fun process(input: Array<FloatArray>, output: Array<FloatArray>): Int
    fun reset()
    fun flush(output: Array<FloatArray>)
}

abstract class AbstractTimeStretcher : TimeStretcher {
    override fun flush(output: Array<FloatArray>) {
        repeat(output.size) { i ->
            output[i].fill(0F)
        }
    }

    override fun reset() { }
}

fun semitonesToRatio(semitones: Double) = 2.0.pow(semitones / 12.0)
