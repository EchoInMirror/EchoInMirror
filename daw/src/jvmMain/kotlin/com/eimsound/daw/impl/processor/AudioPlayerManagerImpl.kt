package com.eimsound.daw.impl.processor

import androidx.compose.runtime.mutableStateMapOf
import com.eimsound.audioprocessor.*
import com.eimsound.daw.Configuration
import com.eimsound.daw.utils.NoSuchFactoryException
import com.eimsound.dsp.native.players.JvmAudioPlayerFactory
import com.eimsound.dsp.native.players.NativeAudioPlayerFactory
import kotlin.io.path.absolutePathString

class AudioPlayerManagerImpl : AudioPlayerManager {
    override val factories = mutableStateMapOf<String, AudioPlayerFactory>()

    init {
        registerFactory(JvmAudioPlayerFactory())
        registerFactory(NativeAudioPlayerFactory(Configuration.nativeHostPath.absolutePathString()))
    }

    override fun registerFactory(factory: AudioPlayerFactory) {
        factories[factory.name] = factory
    }

    override fun create(
        factory: String,
        name: String,
        currentPosition: CurrentPosition,
        processor: AudioProcessor
    ) = factories[factory]?.create(name, currentPosition, processor) ?: throw NoSuchFactoryException(factory)

    override fun createDefaultPlayer(currentPosition: CurrentPosition, processor: AudioProcessor): AudioPlayer {
        val factory = factories.values.firstOrNull() ?: throw NoSuchFactoryException("No audio player factory")
        return factory.create("", currentPosition, processor)
    }
}
