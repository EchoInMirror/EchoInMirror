package cn.apisium.eim.api

import cn.apisium.eim.api.processor.dsp.Pan
import cn.apisium.eim.api.processor.dsp.Volume
import cn.apisium.eim.api.processor.AudioProcessor

interface Track: AudioProcessor, Pan, Volume {
    val processorsChain: List<AudioProcessor>
    fun addProcessor(processor: AudioProcessor, index: Int = -1)
}
