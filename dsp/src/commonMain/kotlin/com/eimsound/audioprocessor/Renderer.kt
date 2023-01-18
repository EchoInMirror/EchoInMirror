package com.eimsound.audioprocessor

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

class RenderPosition(override var ppq: Int, override val sampleRate: Int, range: IntRange) : CurrentPosition {
    override var bpm = 140.0
    override var timeInSeconds = 0.0
    override var ppqPosition = 0.0
    override val bufferSize = 1024
    override var timeSigNumerator = 4
    override var timeSigDenominator = 4
    override var projectRange = range
        set(_) {
            throw UnsupportedOperationException()
        }
    override var loopingRange = range
        set(_) {
            throw UnsupportedOperationException()
        }
    override var isPlaying = true
        set(_) {
            throw UnsupportedOperationException()
        }
    override var isLooping = false
    override var isRecording = false
    override val isRealtime = false
    override var timeInPPQ = range.first
    override var timeInSamples = convertPPQToSamples(timeInPPQ)

    override val ppqCountOfBlock: Int
        get() = (bufferSize / sampleRate / 60.0 * bpm * ppq).toInt()

    override fun setCurrentTime(timeInPPQ: Int) {
        throw UnsupportedOperationException()
    }

    override fun setPPQPosition(ppqPosition: Double) {
        throw UnsupportedOperationException()
    }

    override fun update(timeInSamples: Long) {
        throw UnsupportedOperationException()
    }

    override fun setSampleRateAndBufferSize(sampleRate: Int, bufferSize: Int) {
        throw UnsupportedOperationException()
    }
}
