package com.eimsound.daw.impl.processor

import androidx.compose.runtime.mutableStateMapOf
import com.eimsound.audioprocessor.AudioPlayerFactory
import com.eimsound.audioprocessor.AudioPlayerManager
import com.eimsound.audioprocessor.AudioProcessor
import com.eimsound.audioprocessor.CurrentPosition
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
}