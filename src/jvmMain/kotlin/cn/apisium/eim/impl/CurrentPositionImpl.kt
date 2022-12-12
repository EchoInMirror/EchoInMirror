package cn.apisium.eim.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.apisium.eim.api.CurrentPosition

class CurrentPositionImpl: CurrentPosition {
    override var bpm by mutableStateOf(140.0)
    override var timeInSamples = 0L
    override var timeInSeconds by mutableStateOf(0.0)
    override var ppq by mutableStateOf(96)
    override var ppqPosition by mutableStateOf(0.0)
    override var isPlaying by mutableStateOf(false)
    override var bufferSize by mutableStateOf(1024)
    override val sampleRate by mutableStateOf(44800)

    override fun update(timeInSamples: Long) {
        this.timeInSamples = timeInSamples
        timeInSeconds = timeInSamples.toDouble() / sampleRate
        ppqPosition = timeInSeconds / 60.0 * bpm
    }

    override fun setPPQPosition(ppqPosition: Double) {
        this.ppqPosition = ppqPosition
        timeInSeconds = ppqPosition / bpm * 60.0
        timeInSamples = (timeInSeconds * sampleRate).toLong()
    }
}