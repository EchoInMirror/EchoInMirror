package cn.apisium.eim.api.processor

interface ProcessAudioProcessor: AudioProcessor {
    val isLaunched: Boolean
    suspend fun launch(): Boolean
}
