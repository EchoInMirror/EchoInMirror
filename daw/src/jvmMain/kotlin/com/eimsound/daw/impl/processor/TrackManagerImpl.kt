package com.eimsound.daw.impl.processor

import androidx.compose.runtime.mutableStateMapOf
import com.eimsound.audioprocessor.AudioProcessorDescription
import com.eimsound.daw.api.ProjectInformation
import com.eimsound.daw.api.processor.*
import com.eimsound.daw.commons.NoSuchFactoryException
import com.eimsound.daw.commons.json.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.readText

private val trackManagerLogger = KotlinLogging.logger {  }
class TrackManagerImpl : TrackManager {
    override val factories = mutableStateMapOf<String, TrackFactory<*>>()

    init { reload() }

    override suspend fun createTrack(factory: String?) =
        (factories[factory] ?: factories.values.firstOrNull())?.createAudioProcessor(DefaultTrackDescription)
            ?: throw NoSuchFactoryException(factory ?: "DefaultTrackFactory")

    override suspend fun createTrack(path: Path): Track {
        val json = path.resolve("track.json").toJsonElement() as JsonObject
        val factory = json["factory"]?.asString()
        trackManagerLogger.info { "Creating track ${json["id"]} in \"$path\" with factory \"$factory\"" }
        return factories[factory]?.createAudioProcessor(path)
            ?: throw NoSuchFactoryException(factory ?: "Null")
    }

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
        return bus to { bus.restore(project.root) }
    }

    override fun reload() {
        factories.clear()
        factories.putAll(ServiceLoader.load(TrackFactory::class.java).associateBy { it.name })
    }
}

private val defaultTrackLogger = KotlinLogging.logger {  }
class DefaultTrackFactory : TrackFactory<Track> {
    override val canCreateBus = true
    override val name = "DefaultTrackFactory"
    override val displayName = "默认"
    override val descriptions = setOf(DefaultTrackDescription)

    override suspend fun createAudioProcessor(description: AudioProcessorDescription): TrackImpl {
        defaultTrackLogger.info { "Creating track ${description.identifier}" }
        return TrackImpl(description, this)
    }

    override suspend fun createAudioProcessor(path: Path): TrackImpl {
        defaultTrackLogger.info { "Creating track from \"$path\"" }
        return TrackImpl(DefaultTrackDescription, this).apply { restore(path) }
    }

    override suspend fun createBus(project: ProjectInformation) = BusImpl(project, DefaultTrackDescription, this)
}
