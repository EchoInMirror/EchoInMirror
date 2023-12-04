package com.eimsound.daw.api.clips

import com.eimsound.dsp.data.BaseEnvelopePointList
import com.eimsound.dsp.data.EnvelopePointList
import com.eimsound.dsp.data.midi.NoteMessageList

typealias MidiCCEvents = Map<Int, BaseEnvelopePointList>
typealias MutableMidiCCEvents = MutableMap<Int, EnvelopePointList>

fun MutableMidiCCEvents.copy() = mutableMapOf<Int, EnvelopePointList>().also {
    this@copy.forEach { (id, points) -> it[id] = points.copy() }
}

/**
 * @see com.eimsound.daw.impl.clips.midi.MidiClipImpl
 */
interface MidiClip : Clip {
    val notes: NoteMessageList
    val events: MutableMidiCCEvents
}

/**
 * @see com.eimsound.daw.impl.clips.midi.MidiClipFactoryImpl
 */
interface MidiClipFactory: ClipFactory<MidiClip>