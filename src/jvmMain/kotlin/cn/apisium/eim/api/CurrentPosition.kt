package cn.apisium.eim.api

interface CurrentPosition {
    var bpm: Double
    val timeInSamples: Long
    val timeInSeconds: Double
    val ppq: Int
    val ppqPosition: Double
    var isPlaying: Boolean
    val bufferSize: Int
    val sampleRate: Int

    fun update(timeInSamples: Long)
    fun setPPQPosition(ppqPosition: Double)
}
