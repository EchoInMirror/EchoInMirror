package cn.apisium.eim.api.processor

interface AudioProcessorFactory<T: AudioProcessor> {
    val name: String
    val descriptions: Set<AudioProcessorDescription>
    fun createProcessor(description: AudioProcessorDescription): T
}
