package cn.apisium.eim.api

interface CurrentPosition {
    val bpm: Double
    val timeInSamples: Long
    val timeInSeconds: Double
    val ppq: Int
    val ppqPosition: Double
    val currentPosition: Long
}
