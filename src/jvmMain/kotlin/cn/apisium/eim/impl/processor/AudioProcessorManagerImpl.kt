package cn.apisium.eim.impl.processor

import cn.apisium.eim.api.processor.AudioProcessor
import cn.apisium.eim.api.processor.AudioProcessorFactory
import cn.apisium.eim.api.processor.AudioProcessorManager
import cn.apisium.eim.api.processor.NativeAudioPluginFactory

val AudioProcessorManager.nativeAudioPluginManager
    get() = audioProcessorFactories["NativeAudioPluginFactory"] as NativeAudioPluginFactory

class AudioProcessorManagerImpl: AudioProcessorManager {
    override val audioProcessors = mutableListOf<AudioProcessor>()
    override val audioProcessorFactories = mutableMapOf<String, AudioProcessorFactory<*>>()

    init {
        addProcessorFactory(NativeAudioPluginFactoryImpl())
    }

    override fun addProcessorFactory(factory: AudioProcessorFactory<*>) {
        audioProcessorFactories[factory.name] = factory
    }
}
