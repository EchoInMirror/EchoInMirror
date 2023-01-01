package cn.apisium.eim.api.processor

import cn.apisium.eim.api.ProjectInformation
import com.fasterxml.jackson.databind.JsonNode
import kotlin.jvm.Throws

class NoSuchFactoryException(name: String): Exception("No such factory: $name")
class NoSuchAudioProcessorException(name: String, factory: String): Exception("No such audio processor: $name of $factory")

interface AudioProcessorManager {
    val audioProcessorFactories: Map<String, AudioProcessorFactory<*>>
    val trackFactories: Map<String, TrackFactory<*>>

    fun addAudioProcessorFactory(factory: AudioProcessorFactory<*>)
    fun addTrackFactory(factory: TrackFactory<*>)

    @Throws(NoSuchFactoryException::class)
    suspend fun createAudioProcessor(factory: String, description: AudioProcessorDescription): AudioProcessor
    @Throws(NoSuchFactoryException::class)
    suspend fun createAudioProcessor(path: String, id: String): AudioProcessor
    @Throws(NoSuchFactoryException::class)
    suspend fun createAudioProcessor(path: String, json: JsonNode): AudioProcessor

    @Throws(NoSuchFactoryException::class)
    suspend fun createTrack(factory: String? = null): Track
    @Throws(NoSuchFactoryException::class)
    suspend fun createTrack(path: String, json: JsonNode): Track
    @Throws(NoSuchFactoryException::class)
    suspend fun createTrack(path: String, id: String): Track

    suspend fun createBus(project: ProjectInformation): Bus
}
