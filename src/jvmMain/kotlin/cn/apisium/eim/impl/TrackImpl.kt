package cn.apisium.eim.impl

import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.api.Track
import cn.apisium.eim.api.processor.AudioProcessor
import cn.apisium.eim.api.processor.dsp.calcPanLeftChannel
import cn.apisium.eim.api.processor.dsp.calcPanRightChannel
import cn.apisium.eim.utils.randomColor
import cn.apisium.eim.utils.randomUUID

open class TrackImpl(
    override var name: String
) : Track {
    override val inputChannelsCount = 2
    override val outputChannelsCount = 2
    override var pan = 0F
    override var volume = 1F
    override var color = randomColor()
    override val uuid = randomUUID()

    override val processorsChain = arrayListOf<AudioProcessor>()
    override val subTracks = arrayListOf<Track>()

    override suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Byte>?) {
        processorsChain.forEach { it.processBlock(buffers, position, midiBuffer) }
        subTracks.forEach { it.processBlock(buffers, position, midiBuffer) }
        for (i in buffers[0].indices) buffers[0][i] *= calcPanLeftChannel() * volume
        for (i in buffers[1].indices) buffers[1][i] *= calcPanRightChannel() * volume
    }

    override fun prepareToPlay() {
        processorsChain.forEach { it.prepareToPlay() }
    }

    override fun close() {
        processorsChain.forEach { it.close() }
        processorsChain.clear()
    }

    override fun addProcessor(processor: AudioProcessor, index: Int) {
        if (index < 0) processorsChain.add(processor)
        else processorsChain.add(index, processor)
    }

    override fun addSubTrack(track: Track, index: Int) {
        if (index < 0) subTracks.add(track)
        else subTracks.add(index, track)
    }
}
