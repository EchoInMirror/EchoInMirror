package cn.apisium.eim.api

interface CurrentPosition {
    var bpm: Double
    val timeInSamples: Long
    val timeInSeconds: Double
    var ppq: Int
    val timeInPPQ: Int
    var ppqPosition: Double
    var isPlaying: Boolean
    var bufferSize: Int
    var sampleRate: Int
    var timeSigNumerator: Int
    var timeSigDenominator: Int
    val ppqCountOfBlock: Int

    fun update(timeInSamples: Long)
    fun setPPQPosition(ppqPosition: Double)
    fun setCurrentTime(timeInPPQ: Int)
    fun convertPPQToSamples(ppq: Int): Long
}
