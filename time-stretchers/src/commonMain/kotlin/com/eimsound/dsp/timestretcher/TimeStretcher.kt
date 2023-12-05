package com.eimsound.dsp.timestretcher

import kotlin.math.pow

interface TimeStretcher : AutoCloseable {
    val name: String
    var speedRatio: Float
    var semitones: Float
    val maxFramesNeeded: Int
    val framesNeeded: Int
    val isInitialised: Boolean
    val numChannels: Int
    val samplesPerBlock: Int

    val isDefaultParams get() = speedRatio == 1F && semitones == 0F

    fun initialise(sourceSampleRate: Float, samplesPerBlock: Int, numChannels: Int, isRealtime: Boolean = true)
    fun process(input: Array<FloatArray>, output: Array<FloatArray>, numSamples: Int): Int
    fun flush(output: Array<FloatArray>): Int
    fun reset()
}

abstract class AbstractTimeStretcher(override val name: String) : TimeStretcher {
    final override var isInitialised = false
        private set
    final override var numChannels = 0
        private set
    final override var samplesPerBlock = 0
        private set

    override fun initialise(sourceSampleRate: Float, samplesPerBlock: Int, numChannels: Int, isRealtime: Boolean) {
        this.numChannels = numChannels
        this.samplesPerBlock = samplesPerBlock
        isInitialised = true
    }
}

fun semitonesToRatio(semitones: Float) = 2F.pow(semitones / 12F)
