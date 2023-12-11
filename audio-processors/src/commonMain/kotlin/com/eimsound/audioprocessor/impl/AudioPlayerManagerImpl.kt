package com.eimsound.audioprocessor.impl

import androidx.compose.runtime.mutableStateMapOf
import com.eimsound.audioprocessor.*
import com.eimsound.daw.commons.NoSuchFactoryException
import java.util.*

class AudioPlayerManagerImpl : AudioPlayerManager {
    override val factories = mutableStateMapOf<String, AudioPlayerFactory>()

    init { reload() }

    override fun create(
        factory: String,
        name: String,
        currentPosition: MutablePlayPosition,
        processor: AudioProcessor,
        preferredSampleRate: Int?,
        preferredBufferSize: Int?,
    ) = factories[factory]?.create(name, currentPosition, processor, preferredSampleRate, preferredBufferSize)
        ?: throw NoSuchFactoryException(factory)

    override fun createDefaultPlayer(currentPosition: MutablePlayPosition, processor: AudioProcessor): AudioPlayer {
        val factory = factories.values.firstOrNull() ?: throw NoSuchFactoryException("No audio player factory")
        return factory.create("", currentPosition, processor)
    }

    override fun reload() {
        factories.clear()
        factories.putAll(ServiceLoader.load(AudioPlayerFactory::class.java).filter { it.isEnabled }.associateBy { it.name })
    }
}
