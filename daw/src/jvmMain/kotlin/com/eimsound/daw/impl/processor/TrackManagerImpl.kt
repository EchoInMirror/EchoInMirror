package com.eimsound.daw.impl.processor

import androidx.compose.runtime.mutableStateMapOf
import com.eimsound.audioprocessor.AudioProcessorDescription
import com.eimsound.daw.api.ProjectInformation
import com.eimsound.daw.api.processor.*
import com.eimsound.daw.utils.NoSuchFactoryException
import com.eimsound.daw.utils.asString
import com.eimsound.daw.utils.toJsonElement
import io.github.oshai.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText

private val logger = KotlinLogging.logger {  }
class TrackManagerImpl : TrackManager {
    override val factories = mutableStateMapOf<String, TrackFactory<*>>()

    init { reload() }

    override suspend fun createTrack(factory: String?) =
        (factories[factory] ?: factories.values.firstOrNull())?.createAudioProcessor(DefaultTrackDescription)
            ?: throw NoSuchFactoryException(factory ?: "DefaultTrackFactory")

    override suspend fun createTrack(path: String, json: JsonObject): Track {
        val factory = json["factory"]?.asString()
        logger.info("Creating clip ${json["id"]} in $path with factory $factory")
        return factories[factory]?.createAudioProcessor(path, json)
            ?: throw NoSuchFactoryException(factory ?: "Null")
    }

    override suspend fun createTrack(path: String, id: String) = createTrack(path,
        File(path, "track.json").toJsonElement() as JsonObject)

    override suspend fun createBus(project: ProjectInformation): Pair<Bus, suspend () -> Unit> {
        val file = project.root.resolve("track.json")
        if (!Files.exists(file)) {
            val bus = factories.values.firstOrNull { it.canCreateBus }?.createBus(project)
                ?: throw NoSuchFactoryException("DefaultTrackFactory")
            return bus to { }
        }
        val json = Json.parseToJsonElement(withContext(Dispatchers.IO) { file.readText() }).jsonObject
        val factoryName = json["factory"]?.asString()
        val factory = factories[factoryName] ?: throw NoSuchFactoryException(factoryName ?: "Null")
        val bus = factory.createBus(project)
        return bus to { bus.load(project.root.absolutePathString(), json) }
    }

    override fun reload() {
        factories.clear()
        factories.putAll(ServiceLoader.load(TrackFactory::class.java).associateBy { it.name })
    }
}

class DefaultTrackFactory : TrackFactory<Track> {
    override val canCreateBus = true
    override val name = "DefaultTrackFactory"
    override val displayName = "默认"
    override val descriptions = setOf(DefaultTrackDescription)

    override suspend fun createAudioProcessor(description: AudioProcessorDescription) = TrackImpl(description, this)

    override suspend fun createAudioProcessor(path: String, json: JsonObject) =
        TrackImpl(DefaultTrackDescription, this).apply { load(path, json) }

    override suspend fun createBus(project: ProjectInformation) = BusImpl(project, DefaultTrackDescription, this)
}
