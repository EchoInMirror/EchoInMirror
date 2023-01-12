package cn.apisium.eim.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.CurrentPosition

class CurrentPositionImpl: CurrentPosition {
    override var bpm by mutableStateOf(140.0)
    override var timeInSamples = 0L
    override var timeInSeconds by mutableStateOf(0.0)
    override var ppq by mutableStateOf(96)
    override var timeInPPQ = 0
    override var ppqPosition by mutableStateOf(0.0)
    override var bufferSize by mutableStateOf(1024)
    override var sampleRate by mutableStateOf(48000)
    override var timeSigNumerator by mutableStateOf(4)
    override var timeSigDenominator by mutableStateOf(4)
    override var loopingRange by mutableStateOf(0..0)
    override var projectRange by mutableStateOf(0..ppq * timeSigNumerator * 96)
    override val ppqCountOfBlock get() = (bufferSize / sampleRate / 60.0 * bpm * ppq).toInt()

    override var isLooping by mutableStateOf(false)
    override var isRecording by mutableStateOf(false)
    override val isRealtime = true

    private var _isPlaying by mutableStateOf(false)
    override var isPlaying
        get() = _isPlaying
        set(value) {
            val flag = _isPlaying != value
            _isPlaying = value
            if (flag) EchoInMirror.bus?.onSuddenChange()
        }

    override fun update(timeInSamples: Long) {
        this.timeInSamples = timeInSamples.coerceAtLeast(0)
        timeInSeconds = this.timeInSamples.toDouble() / sampleRate
        ppqPosition = timeInSeconds / 60.0 * bpm
        timeInPPQ = (ppqPosition * ppq).toInt()
        if (timeInPPQ !in projectRange) setCurrentTime(projectRange.first)
    }

    override fun setPPQPosition(ppqPosition: Double) {
        this.ppqPosition = ppqPosition.coerceAtLeast(0.0)
        timeInSeconds = this.ppqPosition / bpm * 60.0
        timeInSamples = (timeInSeconds * sampleRate).toLong()
        timeInPPQ = (this.ppqPosition * ppq * timeSigNumerator).toInt()
        if (timeInPPQ !in projectRange) setCurrentTime(projectRange.first)
        EchoInMirror.bus?.onSuddenChange()
    }

    override fun setCurrentTime(timeInPPQ: Int) {
        this.timeInPPQ = timeInPPQ.coerceIn(projectRange).coerceAtLeast(0)
        ppqPosition = this.timeInPPQ.toDouble() / ppq
        timeInSeconds = ppqPosition / bpm * 60.0
        timeInSamples = (timeInSeconds * sampleRate).toLong()
        EchoInMirror.bus?.onSuddenChange()
    }
}