package com.eimsound.audioprocessor

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

interface Renderable {
    val isRendering: Boolean
    suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Int>)
    fun onRenderStart()
    fun onRenderEnd()
}

enum class RenderFormat(val extend: String, val isLossLess: Boolean = true) {
    WAV("wav"),
    MP3("mp3", false),
    FLAC("flac"),
    OGG("ogg"),
    AU("au"),
    AIFF("aiff")
}

interface Renderer {
    suspend fun start(
        range: IntRange,
        sampleRate: Int,
        ppq: Int,
        bpm: Double,
        file: File,
        format: RenderFormat,
        bits: Int = 16,
        bitRate: Int = 320,
        compressionLevel: Int = 5,
        callback: (Float) -> Unit
    )
}

private val logger = KotlinLogging.logger("RenderPosition")
class RenderPosition(override var ppq: Int, override val sampleRate: Int, range: IntRange) : CurrentPosition {
    override var bpm = 140.0
    override var timeInSeconds = 0.0
    override var ppqPosition = 0.0
    override val bufferSize = 1024
    override var timeSigNumerator = 4
    override var timeSigDenominator = 4
    override var projectRange = range
        set(_) = logger.warn { "Modify projectRange is not supported in RenderPosition" }
    override var loopingRange = range
        set(_) = logger.warn { "Modify loopingRange is not supported in RenderPosition" }
    override var isPlaying = true
        set(_) = logger.warn { "Modify isPlaying is not supported in RenderPosition" }
    override var isLooping = false
    override var isRecording = false
    override val isRealtime = false
    override var isProjectLooping = false
    override var timeInPPQ = range.first
    override var timeInSamples = convertPPQToSamples(timeInPPQ)

    override val ppqCountOfBlock: Int
        get() = (bufferSize / sampleRate / 60.0 * bpm * ppq).toInt()

    override fun setCurrentTime(timeInPPQ: Int) {
        logger.warn { "Call setCurrentTime is not supported in RenderPosition" }
    }

    override fun setPPQPosition(ppqPosition: Double) {
        logger.warn { "Call setPPQPosition is not supported in RenderPosition" }
    }

    override fun update(timeInSamples: Long) {
        logger.warn { "Call update is not supported in RenderPosition" }
    }

    override fun setSampleRateAndBufferSize(sampleRate: Int, bufferSize: Int) {
        logger.warn { "Call setSampleRateAndBufferSize is not supported in RenderPosition" }
    }
}
