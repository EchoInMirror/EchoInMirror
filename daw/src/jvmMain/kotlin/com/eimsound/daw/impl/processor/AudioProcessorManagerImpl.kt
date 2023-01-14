package com.eimsound.daw.impl.processor

import androidx.compose.runtime.mutableStateMapOf
import com.eimsound.audioprocessor.*
import com.eimsound.daw.Configuration
import com.eimsound.daw.NATIVE_AUDIO_PLUGIN_CONFIG
import com.eimsound.daw.api.ProjectInformation
import com.eimsound.daw.api.processor.DefaultTrackDescription
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.api.processor.TrackFactory
import com.eimsound.daw.utils.NoSuchFactoryException
import com.eimsound.dsp.native.NativeAudioPluginFactoryImpl
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

val AudioProcessorManager.nativeAudioPluginManager
    get() = factories["NativeAudioPluginFactory"] as NativeAudioPluginFactory

class AudioProcessorManagerImpl: AudioProcessorManager {
    override val factories = mutableStateMapOf<String, AudioProcessorFactory<*>>()

    init {
        registerFactory(NativeAudioPluginFactoryImpl(NATIVE_AUDIO_PLUGIN_CONFIG, Configuration.nativeHostPath))
        registerFactory(EIMAudioProcessorFactory())
    }

    override fun registerFactory(factory: AudioProcessorFactory<*>) {
        factories[factory.name] = factory
    }

    override suspend fun createAudioProcessor(factory: String, description: AudioProcessorDescription) =
        factories[factory]?.createAudioProcessor(description) ?: throw NoSuchFactoryException(factory)

    override suspend fun createAudioProcessor(path: String, id: String) =
        createAudioProcessor(path, ObjectMapper().readTree(File(path, "$id.json")))

    override suspend fun createAudioProcessor(path: String, json: JsonNode): AudioProcessor {
        val factory = json["factory"]?.asText()
        return factories[factory]?.createAudioProcessor(path, json)
            ?: throw NoSuchFactoryException(factory ?: "Null")
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
