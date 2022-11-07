package cn.apisium.eim.api.processor

interface AudioProcessorManager {
    val audioProcessors: List<AudioProcessor>
    val audioProcessorFactories: Map<String, AudioProcessorFactory<*>>

    fun addProcessorFactory(factory: AudioProcessorFactory<*>)
}
