package com.eimsound.daw.api.processor

import com.eimsound.audioprocessor.AudioProcessor
import com.eimsound.audioprocessor.CurrentPosition
import com.eimsound.audioprocessor.MidiEventHandler
import com.eimsound.audioprocessor.Renderable
import com.eimsound.audioprocessor.dsp.*
import com.eimsound.daw.api.Colorable
import com.eimsound.daw.api.ProjectInformation
import com.eimsound.daw.api.TrackClipList
import com.eimsound.daw.utils.LevelMeter

enum class ChannelType {
    STEREO, LEFT, RIGHT, MONO, SIDE
}

/**
 * @see com.eimsound.daw.impl.processor.TrackImpl
 */
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
