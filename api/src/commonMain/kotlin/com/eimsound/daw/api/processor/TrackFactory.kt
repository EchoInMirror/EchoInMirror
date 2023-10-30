package com.eimsound.daw.api.processor

import com.eimsound.audioprocessor.AudioProcessorFactory
import com.eimsound.audioprocessor.DefaultAudioProcessorDescription
import com.eimsound.daw.api.ProjectInformation
import com.eimsound.daw.utils.NoSuchFactoryException
import com.eimsound.daw.utils.Reloadable
import java.nio.file.Path
import java.util.*

object DefaultTrackDescription : DefaultAudioProcessorDescription("Track", "Track")
interface TrackFactory<T: Track> : AudioProcessorFactory<T> {
    val canCreateBus: Boolean
    suspend fun createBus(project: ProjectInformation): Bus = throw UnsupportedOperationException()
}

/**
 * @see com.eimsound.daw.impl.processor.TrackManagerImpl
 */
interface TrackManager : Reloadable {
    companion object {
        val instance by lazy { ServiceLoader.load(TrackManager::class.java).first()!! }
    }
    val factories: Map<String, TrackFactory<*>>

    @Throws(NoSuchFactoryException::class)
    suspend fun createTrack(factory: String? = null): Track
    @Throws(NoSuchFactoryException::class)
    suspend fun createTrack(path: Path): Track

    suspend fun createBus(project: ProjectInformation): Pair<Bus, suspend () -> Unit>
}
