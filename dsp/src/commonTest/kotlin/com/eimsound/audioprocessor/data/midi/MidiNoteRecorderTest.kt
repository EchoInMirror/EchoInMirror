package com.eimsound.audioprocessor.data.midi

import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse

internal class MidiNoteRecorderTest {
    @Test
    fun isMarked() {
        val recorder = MidiNoteRecorder()
        recorder.markNote(61)
        recorder.markNote(62)
        assert(recorder.isMarked(61))
        assert(recorder.isMarked(62))
        assertFalse(recorder.isMarked(63))
        assertFalse(recorder.isMarked(60))

        recorder.unmarkNote(61)
        assert(recorder.isMarked(62))
        assertFalse(recorder.isMarked(60))
        assertFalse(recorder.isMarked(61))
    }

    @Test
    fun forEachNotes() {
        val recorder = MidiNoteRecorder()
        val exceptList = listOf(61, 62, 63, 64, 65, 121)
        exceptList.forEach(recorder::markNote)
        val list = arrayListOf<Int>()
        recorder.forEachNotes(list::add)
        assertContentEquals(list, exceptList)
    }
}