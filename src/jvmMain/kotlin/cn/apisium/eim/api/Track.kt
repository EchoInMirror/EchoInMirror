package cn.apisium.eim.api

import cn.apisium.eim.api.processor.dsp.Pan
import cn.apisium.eim.api.processor.dsp.Volume
import cn.apisium.eim.api.processor.AudioProcessor
import cn.apisium.eim.api.processor.LevelPeak
import cn.apisium.eim.data.midi.NoteMessage

interface Track: AudioProcessor, Pan, Volume, MidiEventHandler, Colorable {
    val subTracks: MutableList<Track>
    val preProcessorsChain: MutableList<AudioProcessor>
    val postProcessorsChain: MutableList<AudioProcessor>
    val levelPeak: LevelPeak
    val notes: MutableList<NoteMessage>
}
