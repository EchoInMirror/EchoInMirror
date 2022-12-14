package cn.apisium.eim.processor.synthesizer

import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.api.processor.AudioProcessor
import cn.apisium.eim.api.processor.AudioProcessorFactory
import cn.apisium.eim.data.midi.MidiEvent
import cn.apisium.eim.impl.processor.EIMAudioProcessorDescription
import kotlin.math.sin

val SineWaveSynthesizerDescription = EIMAudioProcessorDescription("SineWaveSynthesizer", isInstrument = true)

class SineWaveSynthesizer(
    description: EIMAudioProcessorDescription,
    factory: AudioProcessorFactory<AudioProcessor>,
): Synthesizer(description, factory) {
    private val angels = DoubleArray(127)

    override suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Int>) {
        var midiIndex = 0
        for (i in 0 until position.bufferSize) {
            while (midiIndex < midiBuffer.size && midiBuffer[midiIndex + 1] <= i) {
                markNoteEvent(MidiEvent(midiBuffer[midiIndex]))
                midiIndex += 2
            }
            forEachNotes {
                angels[it.note] += it.noteFrequency * 2 * Math.PI / position.sampleRate
                val sample = sin(angels[it.note]) * (it.velocity / 127.0)
                buffers[0][i] += sample.toFloat()
                buffers[1][i] += sample.toFloat()
            }
        }
        midiBuffer.clear()
    }
}
