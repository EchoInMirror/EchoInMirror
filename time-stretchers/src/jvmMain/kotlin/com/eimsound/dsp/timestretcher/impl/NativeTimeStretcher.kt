package com.eimsound.dsp.timestretcher.impl

import com.eimsound.dsp.timestretcher.TimeStretcher
import com.eimsound.dsp.timestretcher.TimeStretcherFactory
import com.eimsound.dsp.timestretchers.NativeTimeStretcher

class EIMNativeTimeStretcher private constructor(private val instance: NativeTimeStretcher) : TimeStretcher {
    override val name by instance::name
    override var speedRatio by instance::speedRatio
    override var semitones by instance::semitones
    override val maxFramesNeeded by instance::maxFramesNeeded
    override val framesNeeded by instance::framesNeeded
    override val isInitialised get() = instance.isInitialised
    override val numChannels by instance::numChannels
    override val samplesPerBlock by instance::samplesPerBlock
    override val sourceSampleRate by instance::sourceSampleRate
    override val isRealtime get() = instance.isRealtime

    constructor(name: String) : this(NativeTimeStretcher(name))

    override fun initialise(sourceSampleRate: Float, samplesPerBlock: Int, numChannels: Int, isRealtime: Boolean) {
        instance.initialise(sourceSampleRate, samplesPerBlock, numChannels, isRealtime)
    }
    override fun process(input: Array<FloatArray>, output: Array<FloatArray>, numSamples: Int) =
        instance.process(input, output, numSamples)
    override fun flush(output: Array<FloatArray>) = instance.flush(output)
    override fun reset() { instance.reset() }
    override fun copy() = EIMNativeTimeStretcher(instance.copy())
    override fun close() {
        try {
            instance.close()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

class NativeTimeStretcherFactory : TimeStretcherFactory {
    override val timeStretchers: List<String> = try {
        NativeTimeStretcher.getAllTimeStretcherNames().toList()
    } catch (e: Throwable) {
        e.printStackTrace()
        emptyList()
    }

    override fun createTimeStretcher(name: String): TimeStretcher? =
        if (name in timeStretchers) try { EIMNativeTimeStretcher(name) } catch (e: Throwable) { null } else null
}
