package com.eimsound.daw.impl.processor

import androidx.compose.runtime.mutableStateMapOf
import com.eimsound.daw.api.ProjectInformation
import com.eimsound.daw.api.processor.*
import com.eimsound.daw.utils.NoSuchFactoryException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.nio.file.Files
import kotlin.io.path.absolutePathString

class TrackManagerImpl : TrackManager {
    override val factories = mutableStateMapOf<String, TrackFactory<*>>()

    init {
        registerFactory(DefaultTrackFactory())
    }

    override fun registerFactory(factory: TrackFactory<*>) {
        factories[factory.name] = factory
    }

    override suspend fun createTrack(factory: String?) =
        (factories[factory] ?: factories.values.firstOrNull())?.createAudioProcessor(DefaultTrackDescription)
            ?: throw NoSuchFactoryException(factory ?: "DefaultTrackFactory")

    override suspend fun createTrack(path: String, json: JsonNode): Track {
        val factory = json["factory"]?.asText()
        return factories[factory]?.createAudioProcessor(path, json)
            ?: throw NoSuchFactoryException(factory ?: "Null")
    }

    override suspend fun createTrack(path: String, id: String) =
        createTrack(path, ObjectMapper().readTree(File(path, "track.json")))

    override suspend fun createBus(project: ProjectInformation): Pair<Bus, suspend () -> Unit> {
        val file = project.root.resolve("track.json")
        if (!Files.exists(file)) {
            val bus = factories.values.firstOrNull { it.canCreateBus }?.createBus(project)
                ?: throw NoSuchFactoryException("DefaultTrackFactory")
            return bus to { }
        }
        val json = ObjectMapper().readTree(file.toFile())
        val factoryName = json["factory"]?.asText()
        val factory = factories[factoryName] ?: throw NoSuchFactoryException(factoryName ?: "Null")
        val bus = factory.createBus(project)
        return bus to { bus.load(project.root.absolutePathString(), json) }
    }
}