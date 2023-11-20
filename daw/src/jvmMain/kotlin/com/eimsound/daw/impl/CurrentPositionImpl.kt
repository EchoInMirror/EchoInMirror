package com.eimsound.daw.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.audioprocessor.CurrentPosition
import com.eimsound.audioprocessor.SuddenChangeListener
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.utils.observableMutableStateOf

class CurrentPositionImpl(
    private val suddenChangeListener: SuddenChangeListener? = null,
    private val isMainPosition: Boolean = false
): CurrentPosition {
    override var bpm by mutableStateOf(140.0)
    override var timeInSamples = 0L
    override var timeInSeconds by mutableStateOf(0.0)
    override var ppq by mutableStateOf(96)
    override var timeInPPQ by mutableStateOf(0)
    override var ppqPosition by mutableStateOf(0.0)
    override var bufferSize by mutableStateOf(1024)
    override var sampleRate by mutableStateOf(48000)
    override var timeSigNumerator by mutableStateOf(4)
    override var timeSigDenominator by mutableStateOf(4)
    override var loopingRange by mutableStateOf(0..0)
    override var projectRange by mutableStateOf(0..ppq * timeSigNumerator * 96)
    override val ppqCountOfBlock get() = (bufferSize / sampleRate / 60.0 * bpm * ppq).toInt()

    override var isLooping by mutableStateOf(true)
    override var isRecording by mutableStateOf(false)
    override val isRealtime = true
    override var isProjectLooping by mutableStateOf(true)

    override var isPlaying by observableMutableStateOf(false) {
        (if (isMainPosition) EchoInMirror.bus else suddenChangeListener)?.onSuddenChange()
    }

    override fun update(timeInSamples: Long) {
        this.timeInSamples = timeInSamples.coerceAtLeast(0)
        timeInSeconds = this.timeInSamples.toDouble() / sampleRate
        ppqPosition = timeInSeconds / 60.0 * bpm
        timeInPPQ = (ppqPosition * ppq).toInt()
        checkTimeInPPQ()
    }

    override fun setPPQPosition(ppqPosition: Double) {
        if (this.ppqPosition == ppqPosition) return
        this.ppqPosition = ppqPosition.coerceAtLeast(0.0)
        timeInSeconds = this.ppqPosition / bpm * 60.0
        timeInSamples = (timeInSeconds * sampleRate).toLong()
        timeInPPQ = (this.ppqPosition * ppq * timeSigNumerator).toInt()
        checkTimeInPPQ()
        (if (isMainPosition) EchoInMirror.bus else suddenChangeListener)?.onSuddenChange()
    }

    override fun setCurrentTime(timeInPPQ: Int) {
        if (this.timeInPPQ == timeInPPQ) return
        this.timeInPPQ = timeInPPQ.coerceIn(projectRange).coerceAtLeast(0)
        ppqPosition = this.timeInPPQ.toDouble() / ppq
        timeInSeconds = ppqPosition / bpm * 60.0
        timeInSamples = (timeInSeconds * sampleRate).toLong()
        (if (isMainPosition) EchoInMirror.bus else suddenChangeListener)?.onSuddenChange()
    }

    override fun setSampleRateAndBufferSize(sampleRate: Int, bufferSize: Int) {
        this.sampleRate = sampleRate
        this.bufferSize = bufferSize
    }

    private fun checkTimeInPPQ() {
        if (timeInPPQ !in projectRange) if (isProjectLooping) {
            setCurrentTime(projectRange.first)
        } else {
            isPlaying = false
            setCurrentTime(projectRange.last)
        }
    }
}