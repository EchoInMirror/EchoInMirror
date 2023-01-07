package cn.apisium.eim.api.processor

import cn.apisium.eim.api.*
import cn.apisium.eim.api.processor.dsp.*

enum class ChannelType {
    STEREO, LEFT, RIGHT, MONO, SIDE
}

interface Track : AudioProcessor, Pan, Volume, Mute, Solo, Disabled, MidiEventHandler, Colorable, Renderable {
    override var name: String
    val subTracks: MutableList<Track>
    val preProcessorsChain: MutableList<AudioProcessor>
    val postProcessorsChain: MutableList<AudioProcessor>
    val levelMeter: LevelMeter
    val clips: TrackClipList
    var height: Int
    override suspend fun processBlock(
        buffers: Array<FloatArray>,
        position: CurrentPosition,
        midiBuffer: ArrayList<Int>
    )
}

interface Bus : Track {
    val project: ProjectInformation
    var channelType: ChannelType
    val lastSaveTime: Long
    suspend fun save()
}
