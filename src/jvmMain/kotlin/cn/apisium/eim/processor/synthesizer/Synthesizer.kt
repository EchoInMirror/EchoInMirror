package cn.apisium.eim.processor.synthesizer

import cn.apisium.eim.api.processor.AbstractAudioProcessor
import cn.apisium.eim.api.processor.AudioProcessor
import cn.apisium.eim.api.processor.AudioProcessorFactory
import cn.apisium.eim.data.midi.MidiEvent
import cn.apisium.eim.data.midi.MidiNoteRecorder
import cn.apisium.eim.impl.processor.EIMAudioProcessorDescription

abstract class Synthesizer(
    description: EIMAudioProcessorDescription,
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

    @Suppress("DuplicatedCode")
    protected inline fun forEachNotes(block: (MidiEvent) -> Unit) {
        noteRecorder.forEachNotes { block(MidiEvent(notesData[it])) }
    }
}