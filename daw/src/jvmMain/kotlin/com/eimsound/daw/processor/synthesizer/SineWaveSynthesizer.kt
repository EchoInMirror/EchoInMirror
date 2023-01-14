package com.eimsound.daw.processor.synthesizer

import com.eimsound.audioprocessor.AudioProcessor
import com.eimsound.audioprocessor.AudioProcessorFactory
import com.eimsound.audioprocessor.CurrentPosition
import com.eimsound.audioprocessor.data.midi.MidiEvent
import com.eimsound.daw.impl.processor.EIMAudioProcessorDescription
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
