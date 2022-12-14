package cn.apisium.eim.api

interface CurrentPosition {
    var bpm: Double
    val timeInSamples: Long
    val timeInSeconds: Double
    var ppq: Int
    val timeInPPQ: Int
    var ppqPosition: Double
    var isPlaying: Boolean
    var isLooping: Boolean
    var isRecording: Boolean
    val isRealtime: Boolean
    var bufferSize: Int
    var sampleRate: Int
    var timeSigNumerator: Int
    var timeSigDenominator: Int
    var projectRange: IntRange
    var loopingRange: IntRange
    val ppqCountOfBlock: Int

    fun update(timeInSamples: Long)
    fun setPPQPosition(ppqPosition: Double)
    fun setCurrentTime(timeInPPQ: Int)
}

fun CurrentPosition.convertPPQToSamples(ppq: Int) = (ppq.toDouble() / this.ppq / bpm * 60.0 * sampleRate).toLong()
val CurrentPosition.projectDisplayPPQ get() = projectRange.last.coerceAtLeast(timeSigDenominator * ppq * 64) +
        timeSigDenominator * ppq * 16
val CurrentPosition.oneBarPPQ get() = timeSigDenominator * ppq
