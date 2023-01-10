package cn.apisium.eim.impl

import cn.apisium.eim.api.*
import cn.apisium.eim.data.CachedBufferInputStream
import cn.apisium.eim.utils.EIMOutputStream
import cn.apisium.eim.utils.getSampleBits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
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
        set(_) {
            throw UnsupportedOperationException()
        }
    override var loopingRange = range
        set(_) {
            throw UnsupportedOperationException()
        }
    override var isPlaying = true
        set(_) {
            throw UnsupportedOperationException()
        }
    override var isLooping = false
    override var isRecording = false
    override val isRealtime = false
    override var timeInPPQ = range.first
    override var timeInSamples = convertPPQToSamples(timeInPPQ)

    override val ppqCountOfBlock: Int
        get() = (bufferSize / sampleRate / 60.0 * bpm * ppq).toInt()

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
        format: RenderFormat,
        bits: Int,
        bitRate: Int,
        compressionLevel: Int,
        callback: (Float) -> Unit
    ) {
        if (range.first >= range.last) throw IllegalArgumentException("end position must be greater than start position")
        if (range.step != 1) throw IllegalArgumentException("step must be 1")
        if (bits !in arrayOf(16, 24, 32)) throw IllegalArgumentException("bits must be 16, 24 or 32")
        renderTarget.onRenderStart()
        val channels = 2
        val bits0 = bits / 8
        val position = RenderPosition(ppq, sampleRate, range)
        val fullLengthInPPQ = range.last - range.first
        val fullLength = channels * bits0 * position.convertPPQToSamples(fullLengthInPPQ)
        val buffers =
            arrayOf(FloatArray(position.bufferSize), FloatArray(position.bufferSize))
        val sampleBits = getSampleBits(bits0)

        if (format.format == null) {
            withContext(Dispatchers.IO) {
                val pb = ProcessBuilder(
                    "ffmpeg.exe",
                    "-y",
                    "-f",
                    "f32le",
                    "-ar",
                    sampleRate.toString(),
                    "-ac",
                    "2",
                    "-i",
                    "-",
                    file.path
                )
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
                val p = pb.start()
                try {
                    val output = EIMOutputStream(false, p.outputStream)
                    while (position.timeInPPQ < range.last) {
                        buffers.forEach { it.fill(0f) }
                        try {
                            runBlocking {
                                renderTarget.processBlock(buffers, position, ArrayList())
                            }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }

                        for (i in 0 until position.bufferSize) {
                            for (j in 0 until channels) {
                                output.writeFloat(buffers[j][i])
                            }
                        }

                        output.flush()
                        position.timeInSamples += position.bufferSize
                        position.timeInSeconds =
                            position.timeInSamples.toDouble() / position.sampleRate
                        position.ppqPosition = position.timeInSeconds / 60.0 * position.bpm
                        position.timeInPPQ = (position.ppqPosition * position.ppq).toInt()

                        callback(((position.timeInPPQ - range.first).toFloat() / fullLengthInPPQ).coerceAtMost(0.9999F))
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                    p.destroy()
                }
            }
            this.renderTarget.onRenderEnd()
            callback(1f)
            return
        }

        try {
            withContext(Dispatchers.IO) {
                val scope = this
                val outputStream =
                    CachedBufferInputStream(channels * bits0 * position.bufferSize, fullLength) { buffer ->
                        if (position.timeInPPQ > range.last) return@CachedBufferInputStream

                        buffers.forEach { it.fill(0F) }
                        if (!scope.isActive) throw InterruptedException("Render cancelled!")
                        try {
                            runBlocking {
                                renderTarget.processBlock(buffers, position, ArrayList())
                            }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }

                        for (j in 0 until channels) {
                            for (i in 0 until position.bufferSize) {
                                var value = (buffers[j][i] * sampleBits).toInt()
                                for (k in 0 until bits0) {
                                    buffer[i * channels * bits0 + j * bits0 + k] = value.toByte()
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
                AudioSystem.write(
                    AudioInputStream(
                        outputStream, AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED, sampleRate.toFloat(), bits, channels,
                            bits0 * channels, sampleRate.toFloat(), false
                        ), fullLength
                    ), format.format, file
                )
            }
        } finally {
            this.renderTarget.onRenderEnd()
            callback(1F)
        }
    }
}