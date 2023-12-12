package com.eimsound.daw.api.clips

import com.eimsound.audiosources.FileAudioSource
import com.eimsound.dsp.data.AudioThumbnail
import com.eimsound.dsp.data.EnvelopePointList
import kotlinx.serialization.Transient
import java.nio.file.Path

/**
 * @see com.eimsound.daw.impl.clips.audio.AudioClipImpl
 */
interface AudioClip : Clip, AutoCloseable {
    var target: FileAudioSource
    val timeInSeconds: Float
    var speedRatio: Float
    var semitones: Float
    var timeStretcher: String
    var bpm: Float
    @Transient
    val thumbnail: AudioThumbnail
    val volumeEnvelope: EnvelopePointList
    override fun copy(): AudioClip
}

/**
 * @see com.eimsound.daw.impl.clips.audio.AudioClipFactoryImpl
 */
interface AudioClipFactory: ClipFactory<AudioClip> {
    fun createClip(path: Path): AudioClip
    fun createClip(target: FileAudioSource): AudioClip
}
