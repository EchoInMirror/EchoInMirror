package com.eimsound.audioprocessor

import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * @see com.eimsound.daw.impl.CurrentPositionImpl
 * @see com.eimsound.audioprocessor.RenderPosition
 */
interface CurrentPosition {
    var bpm: Double
    val timeInSamples: Long
    val timeInSeconds: Double
    val ppq: Int
    val timeInPPQ: Int
    val ppqPosition: Double
    var isPlaying: Boolean
    var isLooping: Boolean
    var isProjectLooping: Boolean
    var isRecording: Boolean
    val isRealtime: Boolean
    val bufferSize: Int
    val sampleRate: Int
    val timeSigNumerator: Int
    val timeSigDenominator: Int
    val projectRange: IntRange
    val loopingRange: IntRange
    val ppqCountOfBlock: Int
}

interface MutableCurrentPosition : CurrentPosition {
    override var timeInSamples: Long
    override var timeInSeconds: Double
    override var ppq: Int
    override var timeInPPQ: Int
    override var ppqPosition: Double
    override var bufferSize: Int
    override var sampleRate: Int
    override var timeSigNumerator: Int
    override var timeSigDenominator: Int
    override var projectRange: IntRange
    override var loopingRange: IntRange
}

fun CurrentPosition.convertPPQToSamples(ppq: Int) = (ppq.toDouble() / this.ppq / bpm * 60.0 * sampleRate).roundToLong()
fun CurrentPosition.convertSamplesToPPQ(samples: Long) = (samples.toDouble() / sampleRate * bpm / 60.0 * this.ppq).roundToInt()
fun CurrentPosition.convertPPQToSeconds(ppq: Float) = ppq.toDouble() / this.ppq / bpm * 60.0
fun CurrentPosition.convertSecondsToPPQ(seconds: Float) = seconds * bpm * this.ppq / 60.0
val CurrentPosition.projectDisplayPPQ get() = projectRange.last.coerceAtLeast(timeSigDenominator * ppq * 96) +
        timeSigDenominator * ppq * 32
val CurrentPosition.oneBarPPQ get() = timeSigDenominator * ppq
