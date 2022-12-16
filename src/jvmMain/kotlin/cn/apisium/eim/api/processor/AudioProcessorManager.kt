package cn.apisium.eim.api.processor

interface AudioProcessorManager {
    val audioProcessorFactories: Map<String, AudioProcessorFactory<*>>

    fun addProcessorFactory(factory: AudioProcessorFactory<*>)
}
