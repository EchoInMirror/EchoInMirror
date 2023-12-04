package com.eimsound.daw.api.clips

import com.eimsound.audiosources.AudioSource
import com.eimsound.dsp.data.AudioThumbnail
import com.eimsound.dsp.data.EnvelopePointList
import com.eimsound.dsp.timestretcher.TimeStretcher
import kotlinx.serialization.Transient
import java.nio.file.Path

/**
 * @see com.eimsound.daw.impl.clips.audio.AudioClipImpl
 */
interface AudioClip : Clip, AutoCloseable {
    var target: AudioSource
    val timeInSeconds: Float
    @Transient
    val timeStretcher: TimeStretcher
    @Transient
    val thumbnail: AudioThumbnail
    val volumeEnvelope: EnvelopePointList
}

/**
 * @see com.eimsound.daw.impl.clips.audio.AudioClipFactoryImpl
 */
interface AudioClipFactory: ClipFactory<AudioClip> {
    fun createClip(path: Path): AudioClip
    fun createClip(target: AudioSource): AudioClip
}
