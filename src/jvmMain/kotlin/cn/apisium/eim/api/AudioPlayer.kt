package cn.apisium.eim.api

import cn.apisium.eim.api.processor.AudioProcessor

interface AudioPlayer: AutoCloseable {
    var processor: AudioProcessor?
    val currentPosition: CurrentPosition
    fun open(sampleRate: Float, bufferSize: Int, bits: Int)
    fun setCurrentTime(currentPosition: Long)
}
