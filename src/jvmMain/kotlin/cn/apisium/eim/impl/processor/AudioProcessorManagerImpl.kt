package cn.apisium.eim.impl.processor

import androidx.compose.runtime.mutableStateMapOf
import cn.apisium.eim.api.processor.AudioProcessorFactory
import cn.apisium.eim.api.processor.AudioProcessorManager
import cn.apisium.eim.api.processor.NativeAudioPluginFactory

val AudioProcessorManager.nativeAudioPluginManager
    get() = audioProcessorFactories["NativeAudioPluginFactory"] as NativeAudioPluginFactory

class AudioProcessorManagerImpl: AudioProcessorManager {
    override val audioProcessorFactories = mutableStateMapOf<String, AudioProcessorFactory<*>>()

    init {
        addProcessorFactory(NativeAudioPluginFactoryImpl())
    }

    override fun addProcessorFactory(factory: AudioProcessorFactory<*>) {
        audioProcessorFactories[factory.name] = factory
    }
}
