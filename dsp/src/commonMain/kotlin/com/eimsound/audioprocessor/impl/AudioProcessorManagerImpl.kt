package com.eimsound.audioprocessor.impl

import androidx.compose.runtime.mutableStateMapOf
import com.eimsound.audioprocessor.*
import com.eimsound.daw.utils.NoSuchFactoryException
import com.eimsound.daw.utils.asString
import com.eimsound.daw.utils.toJsonElement
import kotlinx.serialization.json.JsonObject
import java.io.File
import java.util.*

val AudioProcessorManager.nativeAudioPluginManager
    get() = factories["NativeAudioPluginFactory"] as NativeAudioPluginFactory

class AudioProcessorManagerImpl: AudioProcessorManager {
    override val factories = mutableStateMapOf<String, AudioProcessorFactory<*>>()

    init { reload() }

    override fun reload() {
        factories.clear()
        ServiceLoader.load(AudioProcessorFactory::class.java).forEach { factories[it.name] = it }
    }

    override suspend fun createAudioProcessor(factory: String, description: AudioProcessorDescription) =
        factories[factory]?.createAudioProcessor(description) ?: throw NoSuchFactoryException(factory)

    override suspend fun createAudioProcessor(path: String, id: String) = createAudioProcessor(path,
        File(path).toJsonElement() as JsonObject)

    override suspend fun createAudioProcessor(path: String, json: JsonObject): AudioProcessor {
        val factory = json["factory"]?.asString()
        return factories[factory]?.createAudioProcessor(path, json)
            ?: throw NoSuchFactoryException(factory ?: "Null")
    }
}
