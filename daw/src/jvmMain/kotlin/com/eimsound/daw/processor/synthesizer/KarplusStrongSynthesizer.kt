package com.eimsound.daw.processor.synthesizer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.audioprocessor.AbstractAudioProcessor
import com.eimsound.audioprocessor.AudioProcessorFactory
import com.eimsound.audioprocessor.PlayPosition
import com.eimsound.dsp.data.midi.MidiEvent
import com.eimsound.audioprocessor.interfaces.Volume
import com.eimsound.daw.impl.processor.EIMAudioProcessorDescription

val KarplusStrongSynthesizerDescription = EIMAudioProcessorDescription("KarplusStrongSynthesizer", isInstrument = true)

class KarplusStrongSynthesizer(factory: AudioProcessorFactory<*>, volume: Float = 1F):
    AbstractAudioProcessor(KarplusStrongSynthesizerDescription, factory), Volume {
    private val cacheBuffers = FloatArray(1024000)
    private var cacheSize = 0
    private val alpha = 0.995
    private val release = 8.0

    private var _volume by mutableStateOf(volume.coerceAtLeast(0F))
    override var volume: Float
        get() = _volume
        set(value) { _volume = value.coerceAtLeast(0F) }

    override var latency = 44800 * 3

    override suspend fun processBlock(buffers: Array<FloatArray>, position: PlayPosition, midiBuffer: ArrayList<Int>) {
        if (cacheSize > 0) {
            val size = cacheSize.coerceAtMost(buffers[0].size)
            for (i in 0 until size) {
                buffers[0][i] += cacheBuffers[i]
                buffers[1][i] += cacheBuffers[i]
            }
            if (size < cacheSize) {
                for (i in size until cacheSize) {
                    cacheBuffers[i - size] = cacheBuffers[i]
                    cacheBuffers[i] = 0F
                }
            }
            cacheSize -= size
        }
        for (i in 0 until midiBuffer.size step 2) {
            val event = MidiEvent(midiBuffer[i])
            if (!event.isNoteOn) continue
            val noteStartTime = midiBuffer[i + 1]
            val noteLength = (position.sampleRate / event.noteFrequency).toInt().coerceAtLeast(1)
            val noteData = FloatArray(noteLength)
            for (j in 0 until noteLength) {
                noteData[j] = ((Math.random() * 2 - 1) * event.velocity / 127.0).toFloat()
            }
            val size = (position.sampleRate * release).toInt()
            var cacheSize2 = 0
            for (j in 0 until size) {
                val index = j % noteLength
                val sample = noteData[index]
                noteData[index] = ((sample + noteData[(index + 1) % noteLength]) * alpha / 2).toFloat()
                if (j + noteStartTime < position.bufferSize) {
                    buffers[0][j + noteStartTime] += sample
                    buffers[1][j + noteStartTime] += sample
                } else {
                    cacheBuffers[cacheSize2++] += sample
                }
            }
            if (cacheSize2 > cacheSize) cacheSize = cacheSize2
        }
        midiBuffer.clear()
    }
}
