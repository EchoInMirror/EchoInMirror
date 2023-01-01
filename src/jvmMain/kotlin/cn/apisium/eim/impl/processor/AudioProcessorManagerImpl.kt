package cn.apisium.eim.impl.processor

import androidx.compose.runtime.mutableStateMapOf
import cn.apisium.eim.api.ProjectInformation
import cn.apisium.eim.api.processor.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.nio.file.Files
import kotlin.io.path.absolutePathString

val AudioProcessorManager.nativeAudioPluginManager
    get() = audioProcessorFactories["NativeAudioPluginFactory"] as NativeAudioPluginFactory

class AudioProcessorManagerImpl: AudioProcessorManager {
    override val audioProcessorFactories = mutableStateMapOf<String, AudioProcessorFactory<*>>()
    override val trackFactories = mutableStateMapOf<String, TrackFactory<*>>()

    init {
        addAudioProcessorFactory(NativeAudioPluginFactoryImpl())
        addAudioProcessorFactory(EIMAudioProcessorFactory())
        addTrackFactory(DefaultTrackFactory())
    }

    override fun addAudioProcessorFactory(factory: AudioProcessorFactory<*>) {
        audioProcessorFactories[factory.name] = factory
    }

    override fun addTrackFactory(factory: TrackFactory<*>) {
        trackFactories[factory.name] = factory
    }

    override suspend fun createAudioProcessor(factory: String, description: AudioProcessorDescription) =
        audioProcessorFactories[factory]?.createAudioProcessor(description) ?: throw NoSuchFactoryException(factory)

    override suspend fun createAudioProcessor(path: String, id: String) =
        createAudioProcessor(path, ObjectMapper().readTree(File(path, "$id.json")))

    override suspend fun createAudioProcessor(path: String, json: JsonNode): AudioProcessor {
        val factory = json["factory"]?.asText()
        return audioProcessorFactories[factory]?.createAudioProcessor(path, json)
            ?: throw NoSuchFactoryException(factory ?: "Null")
    }

    override suspend fun createTrack(factory: String?) =
        (trackFactories[factory] ?: trackFactories.values.firstOrNull())?.createAudioProcessor(DefaultTrackDescription)
            ?: throw NoSuchFactoryException(factory ?: "DefaultTrackFactory")

    override suspend fun createTrack(path: String, json: JsonNode): Track {
        val factory = json["factory"]?.asText()
        return trackFactories[factory]?.createAudioProcessor(path, json)
            ?: throw NoSuchFactoryException(factory ?: "Null")
    }

    override suspend fun createTrack(path: String, id: String) =
        createTrack(path, ObjectMapper().readTree(File(path, "track.json")))

    override suspend fun createBus(project: ProjectInformation): Pair<Bus, suspend () -> Unit> {
        val file = project.root.resolve("track.json")
        if (!Files.exists(file)) {
            val bus = trackFactories.values.firstOrNull { it.canCreateBus }?.createBus(project)
                ?: throw NoSuchFactoryException("DefaultTrackFactory")
            return bus to { }
        }
        val json = ObjectMapper().readTree(file.toFile())
        val factoryName = json["factory"]?.asText()
        val factory = trackFactories[factoryName] ?: throw NoSuchFactoryException(factoryName ?: "Null")
        val bus = factory.createBus(project)
        return bus to { bus.load(project.root.absolutePathString(), json) }
    }
}

class DefaultTrackFactory : TrackFactory<Track> {
    override val canCreateBus = true
    override val name = "DefaultTrackFactory"
    override val descriptions = setOf(DefaultTrackDescription)

    override suspend fun createAudioProcessor(description: AudioProcessorDescription) = TrackImpl(description, this)

    override suspend fun createAudioProcessor(path: String, json: JsonNode) =
        TrackImpl(DefaultTrackDescription, this).apply { load(path, json) }

    override suspend fun createBus(project: ProjectInformation) = BusImpl(project, DefaultTrackDescription, this)
}
