package com.eimsound.daw.api.processor

import com.eimsound.audioprocessor.AudioProcessorFactory
import com.eimsound.audioprocessor.DefaultAudioProcessorDescription
import com.eimsound.daw.api.ProjectInformation
import com.eimsound.daw.utils.NoSuchFactoryException
import com.fasterxml.jackson.databind.JsonNode

object DefaultTrackDescription : DefaultAudioProcessorDescription("Track")
interface TrackFactory<T: Track> : AudioProcessorFactory<T> {
    val canCreateBus: Boolean
    suspend fun createBus(project: ProjectInformation): Bus = throw UnsupportedOperationException()
}

interface TrackManager {
    val factories: Map<String, TrackFactory<*>>
    fun registerFactory(factory: TrackFactory<*>)

    @Throws(NoSuchFactoryException::class)
    suspend fun createTrack(factory: String? = null): Track
    @Throws(NoSuchFactoryException::class)
    suspend fun createTrack(path: String, json: JsonNode): Track
    @Throws(NoSuchFactoryException::class)
    suspend fun createTrack(path: String, id: String): Track

    suspend fun createBus(project: ProjectInformation): Pair<Bus, suspend () -> Unit>
}
