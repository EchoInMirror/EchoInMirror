package cn.apisium.eim.api.processor

import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.utils.randomUUID

interface AudioProcessor: AutoCloseable {
    val inputChannelsCount: Int
    val outputChannelsCount: Int
    var name: String
    val uuid: Long
    suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Int>) { }
    fun prepareToPlay() { }
    fun onSuddenChange() { }
    override fun close() { }
}

abstract class AbstractAudioProcessor: AudioProcessor {
    override val inputChannelsCount = 2
    override val outputChannelsCount = 2
    override val uuid = randomUUID()
    override var name = this::class.simpleName ?: "AbstractAudioProcessor"
}
