package com.eimsound.daw.processor.synthesizer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.audioprocessor.AudioProcessor
import com.eimsound.audioprocessor.AudioProcessorFactory
import com.eimsound.audioprocessor.CurrentPosition
import com.eimsound.audioprocessor.data.midi.MidiEvent
import com.eimsound.audioprocessor.interfaces.Volume
import com.eimsound.daw.impl.processor.EIMAudioProcessorDescription
import kotlin.math.sin

val SineWaveSynthesizerDescription = EIMAudioProcessorDescription("SineWaveSynthesizer", isInstrument = true)

class SineWaveSynthesizer(
    factory: AudioProcessorFactory<AudioProcessor>,
    private val isMix: Boolean = false,
    volume: Float = 1F
): Synthesizer(SineWaveSynthesizerDescription, factory), Volume {
    private val angels = DoubleArray(127)
    private var _volume by mutableStateOf(volume.coerceAtLeast(0F))
    override var volume: Float
        get() = _volume
        set(value) { _volume = value.coerceAtLeast(0F) }

    override suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Int>) {
        var midiIndex = 0
        for (i in 0 until position.bufferSize) {
            while (midiIndex < midiBuffer.size && midiBuffer[midiIndex + 1] <= i) {
                markNoteEvent(MidiEvent(midiBuffer[midiIndex]))
                midiIndex += 2
            }
            forEachNotes {
                angels[it.note] += it.noteFrequency * 2 * Math.PI / position.sampleRate
                val sample = (sin(angels[it.note]) * (it.velocity / 127.0) * volume).toFloat()
                if (isMix) {
                    buffers[0][i] += sample
                    buffers[1][i] += sample
                } else {
                    buffers[0][i] = sample
                    buffers[1][i] = sample
                }
            }
        }
        midiBuffer.clear()
    }
}
