package cn.apisium.eim.processor

import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.api.processor.AudioProcessor
import cn.apisium.eim.utils.randomUUID

class KarplusStrongSynthesizer: AudioProcessor {
    override val inputChannelsCount = 0
    override val outputChannelsCount = 2
    override var name = "KarplusStrongSynthesizer"
    override val uuid = randomUUID()

    override suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Byte>) {
    }
}
