package com.eimsound.audioprocessor.impl

import androidx.compose.runtime.mutableStateMapOf
import com.eimsound.audioprocessor.AudioProcessor
import com.eimsound.audioprocessor.AudioProcessorDescription
import com.eimsound.audioprocessor.AudioProcessorFactory
import com.eimsound.audioprocessor.AudioProcessorManager
import com.eimsound.daw.commons.NoSuchFactoryException
import com.eimsound.daw.commons.json.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.JsonObject
import java.nio.file.Path
import java.util.*

private val audioProcessorManagerLogger = KotlinLogging.logger {  }
class AudioProcessorManagerImpl: AudioProcessorManager {
    override val factories = mutableStateMapOf<String, AudioProcessorFactory<*>>()

    init { reload() }

    override fun reload() {
        factories.clear()
        ServiceLoader.load(AudioProcessorFactory::class.java).forEach { factories[it.name] = it }
    }

    override suspend fun createAudioProcessor(factory: String, description: AudioProcessorDescription): AudioProcessor {
        audioProcessorManagerLogger.info { "Creating audio processor ${description.name} with factory \"$factory\"" }
        return factories[factory]?.createAudioProcessor(description) ?: throw NoSuchFactoryException(factory)
    }

    override suspend fun createAudioProcessor(path: Path): AudioProcessor {
        val json = path.resolve("processor.json").toJsonElement() as JsonObject
        val factory = json["factory"]?.asString()
        audioProcessorManagerLogger.info { "Creating audio processor ${json["id"]} in \"$path\" with factory \"$factory\"" }
        return factories[factory]?.createAudioProcessor(path)
            ?: throw NoSuchFactoryException(factory ?: "Null")
    }
}
