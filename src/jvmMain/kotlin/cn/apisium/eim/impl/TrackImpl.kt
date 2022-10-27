package cn.apisium.eim.impl

import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.api.processor.AudioProcessor
import cn.apisium.eim.api.Track
import cn.apisium.eim.api.processor.dsp.calcPanLeftChannel
import cn.apisium.eim.api.processor.dsp.calcPanRightChannel

open class TrackImpl(
    override var name: String
) : Track, AutoCloseable {
    override val inputChannelsCount = 2
    override val outputChannelsCount = 2
    override var pan = 0F
    override var volume = 1F

    private val _processorsChain = arrayListOf<AudioProcessor>()
    override val processorsChain: List<AudioProcessor> = _processorsChain

    override fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Byte>?) {
        _processorsChain.forEach { it.processBlock(buffers, position, midiBuffer) }
        for (i in buffers[0].indices) buffers[0][i] *= calcPanLeftChannel() * volume
        for (i in buffers[1].indices) buffers[1][i] *= calcPanRightChannel() * volume
    }

    override fun prepareToPlay() {
        _processorsChain.forEach { it.prepareToPlay() }
    }

    override fun close() {
        _processorsChain.forEach { it.close() }
        _processorsChain.clear()
    }

    override fun addProcessor(processor: AudioProcessor, index: Int) {
        if (index < 0) _processorsChain.add(processor)
        else _processorsChain.add(index, processor)
    }
}
