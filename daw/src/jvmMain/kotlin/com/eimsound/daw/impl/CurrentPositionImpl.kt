package com.eimsound.daw.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.audioprocessor.MutableCurrentPosition
import com.eimsound.audioprocessor.SuddenChangeListener
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.utils.observableMutableStateOf

class CurrentPositionImpl(
    private val suddenChangeListener: SuddenChangeListener? = null,
    private val isMainPosition: Boolean = false
): MutableCurrentPosition {
    override var bpm by mutableStateOf(140.0)
    override var timeInSeconds by mutableStateOf(0.0)
    override var ppq by mutableStateOf(96)
    override var bufferSize by mutableStateOf(1024)
    override var sampleRate by mutableStateOf(44100)
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

    private var _timeInSamples = 0L
    override var timeInSamples
        get() = _timeInSamples
        set(value) {
            _timeInSamples = value.coerceAtLeast(0)
            timeInSeconds = _timeInSamples.toDouble() / sampleRate
            _ppqPosition = timeInSeconds / 60.0 * bpm
            _timeInPPQ = (ppqPosition * ppq).toInt()
            checkTimeInPPQ()
        }
    private var _timeInPPQ by mutableStateOf(0)
    override var timeInPPQ
        get() = _timeInPPQ
        set(value) {
            if (value == _timeInPPQ) return
            _timeInPPQ = value.coerceIn(projectRange).coerceAtLeast(0)
            _ppqPosition = _timeInPPQ.toDouble() / ppq
            timeInSeconds = ppqPosition / bpm * 60.0
            _timeInSamples = (timeInSeconds * sampleRate).toLong()
            (if (isMainPosition) EchoInMirror.bus else suddenChangeListener)?.onSuddenChange()
        }
    private var _ppqPosition by mutableStateOf(0.0)
    override var ppqPosition
        get() = _ppqPosition
        set(value) {
            if (value == _ppqPosition) return
            _ppqPosition = value.coerceAtLeast(0.0)
            timeInSeconds = _ppqPosition / bpm * 60.0
            _timeInSamples = (timeInSeconds * sampleRate).toLong()
            _timeInPPQ = (_ppqPosition * ppq * timeSigNumerator).toInt()
            checkTimeInPPQ()
            (if (isMainPosition) EchoInMirror.bus else suddenChangeListener)?.onSuddenChange()
        }

    private fun checkTimeInPPQ() {
        if (_timeInPPQ !in projectRange) if (isProjectLooping) {
            _timeInPPQ = projectRange.first
        } else {
            isPlaying = false
            _timeInPPQ = projectRange.last
        }
    }
}