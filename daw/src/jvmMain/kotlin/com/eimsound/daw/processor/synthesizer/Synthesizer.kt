package com.eimsound.daw.processor.synthesizer

import com.eimsound.audioprocessor.AbstractAudioProcessor
import com.eimsound.audioprocessor.AudioProcessor
import com.eimsound.audioprocessor.AudioProcessorDescription
import com.eimsound.audioprocessor.AudioProcessorFactory
import com.eimsound.audioprocessor.data.midi.MidiEvent
import com.eimsound.audioprocessor.data.midi.MidiNoteRecorder

abstract class Synthesizer(
    description: AudioProcessorDescription,
    factory: AudioProcessorFactory<AudioProcessor>,
) : AbstractAudioProcessor(description, factory) {
    override val inputChannelsCount = 0
    protected val noteRecorder = MidiNoteRecorder()
    protected var notesData = IntArray(128)

    protected fun markNoteEvent(midi: MidiEvent) {
        if (midi.isNoteOn) {
            noteRecorder.markNote(midi.note)
            notesData[midi.note] = midi.rawData
        } else if (midi.isNoteOff) {
            noteRecorder.unmarkNote(midi.note)
            notesData[midi.note] = 0
        }
    }

    protected inline fun forEachNotes(block: (MidiEvent) -> Unit) {
        noteRecorder.forEachNotes { block(MidiEvent(notesData[it])) }
    }

    override fun onSuddenChange() { noteRecorder.reset() }
}