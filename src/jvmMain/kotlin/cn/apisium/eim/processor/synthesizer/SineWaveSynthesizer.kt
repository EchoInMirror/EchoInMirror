package cn.apisium.eim.processor.synthesizer

import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.data.midi.MidiEvent
import cn.apisium.eim.utils.randomUUID
import kotlin.math.sin

class SineWaveSynthesizer: Synthesizer() {
    override val inputChannelsCount = 0
    override val outputChannelsCount = 2
    override var name = "SineWaveSynthesizer"
    override val uuid = randomUUID()
    private val angels = DoubleArray(127)

    override suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Int>) {
        var midiIndex = 0
        for (i in 0 until EchoInMirror.bufferSize) {
            while (midiIndex < midiBuffer.size && midiBuffer[midiIndex + 1] <= i) {
                markNoteEvent(MidiEvent(midiBuffer[midiIndex]))
                midiIndex += 2
            }
            forEachNoteOn {
                angels[it.note] += it.noteFrequency * 2 * Math.PI / EchoInMirror.sampleRate
                val sample = sin(angels[it.note]) * (it.velocity / 127.0)
                buffers[0][i] += sample.toFloat()
                buffers[1][i] += sample.toFloat()
            }
        }
    }
}
