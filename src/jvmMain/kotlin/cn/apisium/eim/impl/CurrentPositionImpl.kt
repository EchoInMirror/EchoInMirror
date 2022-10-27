package cn.apisium.eim.impl

import cn.apisium.eim.api.CurrentPosition

class CurrentPositionImpl: CurrentPosition {
    override var bpm = 140.0
    override var timeInSamples = 0L
    override var timeInSeconds = 0.0
    override var ppq = 96
    override var ppqPosition = 0.0
    override var currentPosition = 0L

    fun update(timeInSamples: Long, sampleRate: Float) {
        this.timeInSamples = timeInSamples
        timeInSeconds = timeInSamples.toDouble() / sampleRate
        ppqPosition = timeInSeconds / 60.0 * bpm
        currentPosition = (ppqPosition * ppq).toLong()
    }

    @Suppress("unused")
    fun setCurrentPosition(currentPosition: Long, sampleRate: Float) {
        this.currentPosition = currentPosition
        ppqPosition = currentPosition.toDouble() / ppq
        timeInSeconds = ppqPosition / bpm * 60.0
        timeInSamples = (timeInSeconds * sampleRate).toLong()
    }
}
