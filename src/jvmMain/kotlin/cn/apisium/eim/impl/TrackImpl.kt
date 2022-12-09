package cn.apisium.eim.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.api.Track
import cn.apisium.eim.api.processor.AudioProcessor
import cn.apisium.eim.api.processor.LevelPeakImpl
import cn.apisium.eim.api.processor.dsp.calcPanLeftChannel
import cn.apisium.eim.api.processor.dsp.calcPanRightChannel
import cn.apisium.eim.utils.randomColor
import cn.apisium.eim.utils.randomUUID

open class TrackImpl(
    override var name: String
) : Track {
    override val inputChannelsCount = 2
    override val outputChannelsCount = 2
    override var pan by mutableStateOf(0F)
    override var volume by mutableStateOf(1F)
    override var color by mutableStateOf(randomColor())
    override val uuid = randomUUID()

    override val levelPeak = LevelPeakImpl()

    override val processorsChain = arrayListOf<AudioProcessor>()
    override val subTracks = arrayListOf<Track>()

    override suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Byte>?) {
        processorsChain.forEach { it.processBlock(buffers, position, midiBuffer) }
        subTracks.forEach { it.processBlock(buffers, position, midiBuffer) }
        levelPeak.left = 0F
        levelPeak.right = 0F
        for (i in buffers[0].indices) {
            buffers[0][i] *= calcPanLeftChannel() * volume
            val tmp = buffers[0][i]
            if (tmp > levelPeak.left) levelPeak.left = tmp
        }
        for (i in buffers[1].indices) {
            buffers[1][i] *= calcPanRightChannel() * volume
            val tmp = buffers[1][i]
            if (tmp > levelPeak.right) levelPeak.right = tmp
        }
    }

    override fun prepareToPlay() {
        processorsChain.forEach(AudioProcessor::prepareToPlay)
        subTracks.forEach(Track::prepareToPlay)
    }

    override fun close() {
        processorsChain.forEach { it.close() }
        processorsChain.clear()
        subTracks.forEach { it.close() }
        subTracks.clear()
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
