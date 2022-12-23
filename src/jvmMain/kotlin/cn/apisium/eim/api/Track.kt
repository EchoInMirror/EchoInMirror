package cn.apisium.eim.api

import cn.apisium.eim.api.processor.AudioProcessor
import cn.apisium.eim.api.processor.LevelMeter
import cn.apisium.eim.api.processor.dsp.*
import cn.apisium.eim.data.midi.NoteMessageList

interface Track : AudioProcessor, Pan, Volume, Mute, Solo, Disabled, MidiEventHandler, Colorable, Renderable {
    val subTracks: MutableList<Track>
    val preProcessorsChain: MutableList<AudioProcessor>
    val postProcessorsChain: MutableList<AudioProcessor>
    val levelMeter: LevelMeter
    val notes: NoteMessageList
    override suspend fun processBlock(
        buffers: Array<FloatArray>,
        position: CurrentPosition,
        midiBuffer: ArrayList<Int>
    )
}
