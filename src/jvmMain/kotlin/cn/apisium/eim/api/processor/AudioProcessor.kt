package cn.apisium.eim.api.processor

import cn.apisium.eim.api.CurrentPosition

interface AudioProcessor: AutoCloseable {
    val inputChannelsCount: Int
    val outputChannelsCount: Int
    var name: String
    fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Byte>? = null) { }
    fun prepareToPlay(sampleRate: Float, bufferSize: Int) { }
    override fun close() { }
}
