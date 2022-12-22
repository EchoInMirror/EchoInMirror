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

class RenderPosition(override var ppq: Int, override var sampleRate: Int, override var timeInPPQ: Int) : CurrentPosition {
    override var bpm = 140.0
    override var timeInSamples = 0L
    override var timeInSeconds = 0.0
    override var ppqPosition = 0.0
    override var bufferSize = 1024
    override var timeSigNumerator = 4
    override var timeSigDenominator = 4
    override var isPlaying = true
        set(_) { throw UnsupportedOperationException() }
    override var isLooping = false
    override var isRecording = false
    override val isRealtime = false

    init {
        this.timeInSamples = this.convertPPQToSamples(timeInPPQ)
    }

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
        startPosition: Int,
        endPosition: Int,
        sampleRate: Int,
        ppq: Int,
        bpm: Double,
        file: File,
        audioType: AudioFileFormat.Type,
        callback: (Float) -> Unit
    ) {
        if (endPosition <= startPosition) throw IllegalArgumentException("endPosition must be greater than startPosition")
        renderTarget.onRenderStart()
        val channels = 2
        val bits = 2
        val position = RenderPosition(ppq, sampleRate, startPosition)
        val fullLength = channels * bits *
                position.convertPPQToSamples(endPosition - startPosition)
        val buffers =
            arrayOf(FloatArray(position.bufferSize), FloatArray(position.bufferSize))
        val sampleBits = getSampleBits(bits)
        val outputStream = CachedBufferInputStream(channels * bits * position.bufferSize, fullLength) { buffer ->
            if (position.timeInPPQ > endPosition) return@CachedBufferInputStream

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

            callback(((position.timeInPPQ - startPosition).toFloat() / (endPosition - startPosition)).coerceAtMost(0.9999F))
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