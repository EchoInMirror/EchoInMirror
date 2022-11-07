package cn.apisium.eim.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.apisium.eim.EchoInMirror

class CurrentPosition {
    var bpm by mutableStateOf(140.0)
    var timeInSamples = 0L
    var timeInSeconds by mutableStateOf(0.0)
    var ppq by mutableStateOf(96)
    var ppqPosition by mutableStateOf(0.0)
    var isPlaying by mutableStateOf(false)

    fun update(timeInSamples: Long) {
        this.timeInSamples = timeInSamples
        timeInSeconds = timeInSamples.toDouble() / EchoInMirror.sampleRate
        ppqPosition = timeInSeconds / 60.0 * bpm
    }

    fun setPPQPosition(ppqPosition: Double) {
        this.ppqPosition = ppqPosition
        timeInSeconds = ppqPosition / bpm * 60.0
        timeInSamples = (timeInSeconds * EchoInMirror.sampleRate).toLong()
    }
}
