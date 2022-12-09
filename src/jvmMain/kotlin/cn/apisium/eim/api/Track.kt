package cn.apisium.eim.api

import cn.apisium.eim.api.processor.dsp.Pan
import cn.apisium.eim.api.processor.dsp.Volume
import cn.apisium.eim.api.processor.AudioProcessor
import cn.apisium.eim.api.processor.LevelPeak

interface Track: AudioProcessor, Pan, Volume, Colorable {
    val subTracks: MutableList<Track>
    val preProcessorsChain: MutableList<AudioProcessor>
    val postProcessorsChain: MutableList<AudioProcessor>
    val levelPeak: LevelPeak
}
