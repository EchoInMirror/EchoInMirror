package cn.apisium.eim.impl

import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.api.Renderable
import cn.apisium.eim.api.Renderer
import cn.apisium.eim.data.CachedBufferInputStream
import cn.apisium.eim.utils.getSampleBits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

class RenderPosition(override var ppq: Int, override var sampleRate: Int, range: IntRange) : CurrentPosition {
    override var bpm = 140.0
    override var timeInSeconds = 0.0
    override var ppqPosition = 0.0
    override var bufferSize = 1024
    override var timeSigNumerator = 4
    override var timeSigDenominator = 4
    override var projectRange = range
        set(_) { throw UnsupportedOperationException() }
    override var loopingRange = range
        set(_) { throw UnsupportedOperationException() }
    override var isPlaying = true
        set(_) { throw UnsupportedOperationException() }
    override var isLooping = false
    override var isRecording = false
    override val isRealtime = false
    override var timeInPPQ = range.first
    override var timeInSamples = this.convertPPQToSamples(timeInPPQ)

    override val ppqCountOfBlock: Int
        get() = (bufferSize / sampleRate / 60.0 * bpm * ppq).toInt()

    override fun convertPPQToSamples(ppq: Int): Long = (ppq.toDouble() / this.ppq / bpm * 60.0 * sampleRate).toLong()
    override fun setCurrentTime(timeInPPQ: Int) {
        throw UnsupportedOperationException()
    }

    override fun setPPQPosition(ppqPosition: Double) {
        throw UnsupportedOperationException()
    }

    override fun update(timeInSamples: Long) {
        throw UnsupportedOperationException()
    }
}

class RendererImpl(private val renderTarget: Renderable) : Renderer {
    override suspend fun start(
        range: IntRange,
        sampleRate: Int,
        ppq: Int,
        bpm: Double,
        file: File,
        audioType: AudioFileFormat.Type,
        callback: (Float) -> Unit
    ) {
        if (range.first <= range.last) throw IllegalArgumentException("end position must be greater than start position")
        if (range.step != 1) throw IllegalArgumentException("step must be 1")
        renderTarget.onRenderStart()
        val channels = 2
        val bits = 2
        val position = RenderPosition(ppq, sampleRate, range)
        val fullLengthInPPQ = range.first - range.last
        val fullLength = channels * bits * position.convertPPQToSamples(fullLengthInPPQ)
        val buffers =
            arrayOf(FloatArray(position.bufferSize), FloatArray(position.bufferSize))
        val sampleBits = getSampleBits(bits)
        val outputStream = CachedBufferInputStream(channels * bits * position.bufferSize, fullLength) { buffer ->
            if (position.timeInPPQ > range.last) return@CachedBufferInputStream

            buffers.forEach { it.fill(0F) }
            try {
                runBlocking { renderTarget.processBlock(buffers, position, ArrayList()) }
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            for (j in 0 until channels) {
                for (i in 0 until position.bufferSize) {
                    var value = (buffers[j][i] * sampleBits).toInt()
                    for (k in 0 until bits) {
                        buffer[i * channels * bits + j * bits + k] = value.toByte()
                        value = value shr 8
                    }
                }
            }

            position.timeInSamples += position.bufferSize
            position.timeInSeconds =
                position.timeInSamples.toDouble() / position.sampleRate
            position.ppqPosition = position.timeInSeconds / 60.0 * position.bpm
            position.timeInPPQ = (position.ppqPosition * position.ppq).toInt()

            callback(((position.timeInPPQ - range.first).toFloat() / fullLengthInPPQ).coerceAtMost(0.9999F))
        }
        val format = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED, sampleRate.toFloat(), bits * 8, channels,
            bits * channels, sampleRate.toFloat(), false
        )
        withContext(Dispatchers.IO) {
            AudioSystem.write(AudioInputStream(outputStream, format, fullLength), audioType, file)
        }
        this.renderTarget.onRenderEnd()
        callback(1F)
    }
}