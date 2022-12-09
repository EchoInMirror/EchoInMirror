package cn.apisium.eim.api

import cn.apisium.eim.api.processor.dsp.Pan
import cn.apisium.eim.api.processor.dsp.Volume
import cn.apisium.eim.api.processor.AudioProcessor
import cn.apisium.eim.api.processor.LevelPeak

interface Track: AudioProcessor, Pan, Volume, Colorable {
    val subTracks: List<Track>
    val processorsChain: List<AudioProcessor>
    val levelPeak: LevelPeak
    fun addProcessor(processor: AudioProcessor, index: Int = -1)
    fun addSubTrack(track: Track, index: Int = -1)
}
