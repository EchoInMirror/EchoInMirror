package com.eimsound.daw.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.audioprocessor.MutablePlayPosition
import com.eimsound.audioprocessor.SuddenChangeListener
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.utils.observableMutableStateOf

class PlayPositionImpl(
    private val suddenChangeListener: SuddenChangeListener? = null,
    private val isMainPosition: Boolean = false
): MutablePlayPosition {
    override var bpm by mutableStateOf(140.0)
    override var timeInSeconds by mutableStateOf(0.0)
    override val ppq = 96
    override var bufferSize = 1024
    override var sampleRate = 44100
    override var timeToPause = 0
    override var timeSigNumerator by mutableStateOf(4)
    override var timeSigDenominator by mutableStateOf(4)
    override var loopingRange by mutableStateOf(0..0)
    override var projectRange by mutableStateOf(0..ppq * timeSigNumerator * 96)
    override val ppqCountOfBlock get() = (bufferSize / sampleRate / 60.0 * bpm * ppq).toInt()

    override var isLooping by mutableStateOf(true)
    override var isRecording by mutableStateOf(false)
    override val isRealtime = true
    override var isProjectLooping by mutableStateOf(true)
    private var _isPlaying by observableMutableStateOf(false) {
        (if (isMainPosition) EchoInMirror.bus else suddenChangeListener)?.onSuddenChange()
    }

    override var isPlaying
        get() = _isPlaying
        set(value) {
            if (value == _isPlaying) return
            if (value) _isPlaying = true
            else {
                timeToPause = 2048
                lastTime = 0
            }
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

    private var lastTime = 0
    private fun checkTimeInPPQ() {
        if (timeToPause > 0) {
            if (_timeInPPQ > lastTime && timeToPause > 0) timeToPause -= bufferSize
            if (timeToPause <= 0) _isPlaying = false
            lastTime = _timeInPPQ
        } else if (_timeInPPQ !in projectRange) {
            if (isProjectLooping) {
                _timeInPPQ = projectRange.first
            } else {
                isPlaying = false
                _timeInPPQ = projectRange.last
            }
        }
    }
}