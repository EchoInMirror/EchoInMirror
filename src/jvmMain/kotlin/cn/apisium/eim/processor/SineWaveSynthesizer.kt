package cn.apisium.eim.processor

import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.api.processor.AudioProcessor
import cn.apisium.eim.utils.randomUUID
import kotlin.math.sin

class SineWaveSynthesizer(private val frequency: Double): AudioProcessor {
    override val inputChannelsCount = 0
    override val outputChannelsCount = 2
    override var name = "SineWaveSynthesizer"
    override val uuid = randomUUID()

    private var currentAngle = 0.0
    private val volume = 0.01F

    override suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Byte>?) {
        for (i in 0 until EchoInMirror.bufferSize) {
            val samplePos = sin(currentAngle).toFloat() * volume
            currentAngle += 2 * Math.PI * frequency / EchoInMirror.sampleRate

            buffers[0][i] = samplePos
            buffers[1][i] = samplePos
        }
    }
}
