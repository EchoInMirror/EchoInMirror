package cn.apisium.eim.api.processor

import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.utils.randomUUID

interface AudioProcessorDescription {
    val name: String
    val category: String?
    val manufacturerName: String?
    val version: String?
    val isInstrument: Boolean?
    val identifier: String?
}

interface AudioProcessor: AutoCloseable {
    val name: String get() =  description.name
    val description: AudioProcessorDescription
    val inputChannelsCount: Int
    val outputChannelsCount: Int
    val uuid: Long
    suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Int>) { }
    fun prepareToPlay() { }
    fun onSuddenChange() { }
    fun onClick() { }
    override fun close() { }
}

abstract class AbstractAudioProcessor(
    override val description: AudioProcessorDescription = AudioProcessorDescriptionImpl("AbstractAudioProcessor")
): AudioProcessor {
    override val inputChannelsCount = 2
    override val outputChannelsCount = 2
    override val uuid = randomUUID()

    constructor(name: String): this(AudioProcessorDescriptionImpl(name))
}

open class AudioProcessorDescriptionImpl(
    override val name: String,
    override val category: String? = null,
    override val manufacturerName: String? = null,
    override val version: String? = null,
    override val isInstrument: Boolean? = null,
    override val identifier: String? = null,
): AudioProcessorDescription
