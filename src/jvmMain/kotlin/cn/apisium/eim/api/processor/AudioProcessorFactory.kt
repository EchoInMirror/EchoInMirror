package cn.apisium.eim.api.processor

import java.nio.file.Path

interface AudioProcessorFactory<T: AudioProcessor> {
    val name: String
    fun createProcessor(identifier: String? = null, file: Path? = null): T
}
