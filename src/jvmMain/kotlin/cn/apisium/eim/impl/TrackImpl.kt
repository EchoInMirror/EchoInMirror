package cn.apisium.eim.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.api.Track
import cn.apisium.eim.api.processor.AbstractAudioProcessor
import cn.apisium.eim.api.processor.AudioProcessor
import cn.apisium.eim.api.processor.LevelPeakImpl
import cn.apisium.eim.api.processor.dsp.calcPanLeftChannel
import cn.apisium.eim.api.processor.dsp.calcPanRightChannel
import cn.apisium.eim.data.midi.MidiEvent
import cn.apisium.eim.utils.randomColor

open class TrackImpl(
    trackName: String
) : Track, AbstractAudioProcessor() {
    override var name by mutableStateOf(trackName)
    override var pan by mutableStateOf(0F)
    override var volume by mutableStateOf(1F)
    override var color by mutableStateOf(randomColor())

    override val levelPeak = LevelPeakImpl()

    override val preProcessorsChain = arrayListOf<AudioProcessor>()
    override val postProcessorsChain = arrayListOf<AudioProcessor>()
    override val subTracks = arrayListOf<Track>()
    private val pendingMidiBuffer = ArrayList<Int>()

    override suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Int>) {
        if (pendingMidiBuffer.isNotEmpty()) {
            midiBuffer.addAll(pendingMidiBuffer)
            pendingMidiBuffer.clear()
        }
        preProcessorsChain.forEach { it.processBlock(buffers, position, midiBuffer) }
        subTracks.forEach { it.processBlock(buffers, position, ArrayList(midiBuffer)) }
        postProcessorsChain.forEach { it.processBlock(buffers, position, midiBuffer) }
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
        preProcessorsChain.forEach(AudioProcessor::prepareToPlay)
        subTracks.forEach(Track::prepareToPlay)
        postProcessorsChain.forEach(AudioProcessor::prepareToPlay)
    }

    override fun close() {
        preProcessorsChain.forEach { it.close() }
        preProcessorsChain.clear()
        subTracks.forEach { it.close() }
        subTracks.clear()
        postProcessorsChain.forEach { it.close() }
        postProcessorsChain.clear()
    }

    override fun playMidiEvent(midiEvent: MidiEvent, time: Int) {
        pendingMidiBuffer.add(midiEvent.rawData)
        pendingMidiBuffer.add(time)
    }
}
