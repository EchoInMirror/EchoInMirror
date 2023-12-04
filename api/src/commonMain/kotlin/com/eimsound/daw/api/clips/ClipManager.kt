package com.eimsound.daw.api.clips

import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.commons.NoSuchFactoryException
import com.eimsound.daw.commons.Reloadable
import kotlinx.serialization.json.JsonObject
import java.nio.file.Path
import java.util.*

/**
 * @see com.eimsound.daw.impl.clips.ClipManagerImpl
 */
interface ClipManager : Reloadable {
    companion object {
        val instance by lazy { ServiceLoader.load(ClipManager::class.java).first()!! }
    }

    val factories: Map<String, ClipFactory<*>>

    @Throws(NoSuchFactoryException::class)
    suspend fun createClip(factory: String): Clip
    @Throws(NoSuchFactoryException::class)
    suspend fun createClip(path: Path, json: JsonObject): Clip
    fun <T: Clip> createTrackClip(
        clip: T, time: Int = 0, duration: Int = clip.defaultDuration.coerceAtLeast(0),
        start: Int = 0, track: Track? = null
    ): TrackClip<T>
    suspend fun createTrackClip(path: Path, json: JsonObject): TrackClip<Clip>
}

val ClipManager.defaultMidiClipFactory get() = factories["MIDIClip"] as MidiClipFactory
val ClipManager.defaultAudioClipFactory get() = factories["AudioClip"] as AudioClipFactory
val ClipManager.defaultEnvelopeClipFactory get() = factories["EnvelopeClip"] as EnvelopeClipFactory
