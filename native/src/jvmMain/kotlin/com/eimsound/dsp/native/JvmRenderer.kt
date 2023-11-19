package com.eimsound.dsp.native

import com.eimsound.audioprocessor.*
import com.eimsound.daw.utils.ByteBufOutputStream
import com.eimsound.daw.utils.CachedBufferInputStream
import com.eimsound.daw.utils.range
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

private fun getJvmAudioFormat(format: RenderFormat) = when (format) {
    RenderFormat.WAV -> AudioFileFormat.Type.WAVE
    RenderFormat.AU -> AudioFileFormat.Type.AU
    RenderFormat.AIFF -> AudioFileFormat.Type.AIFF
    else -> null
}

private val logger = KotlinLogging.logger { }
class JvmRenderer(private val renderTarget: Renderable) : Renderer {
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
        val fullLengthInPPQ = range.range
        val fullLength = channels * bits0 * position.convertPPQToSamples(fullLengthInPPQ)
        val buffers =
            arrayOf(FloatArray(position.bufferSize), FloatArray(position.bufferSize))
        val sampleBits = getSampleBits(bits0)

        val jvmFormat = getJvmAudioFormat(format)
        if (jvmFormat == null) {
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
                    val output = ByteBufOutputStream(false, p.outputStream)
                    while (position.timeInPPQ < range.last) {
                        buffers.forEach { it.fill(0f) }
                        try {
                            runBlocking {
                                renderTarget.processBlock(buffers, position, ArrayList())
                            }
                        } catch (e: Throwable) {
                            logger.error(e) { "Error while rendering" }
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
                    logger
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
                            logger.error(e) { "Error while rendering" }
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
                    ), jvmFormat, file
                )
            }
        } finally {
            this.renderTarget.onRenderEnd()
            callback(1F)
        }
    }
}
