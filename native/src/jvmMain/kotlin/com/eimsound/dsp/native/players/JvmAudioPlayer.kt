package com.eimsound.dsp.native.players

import com.eimsound.audioprocessor.*
import com.eimsound.dsp.native.getSampleBits
import kotlinx.coroutines.runBlocking
import java.io.EOFException
import java.util.*
import javax.sound.sampled.*
import kotlin.math.roundToInt

class JvmAudioPlayer(
    factory: JvmAudioPlayerFactory,
    private val mixer: Mixer.Info,
    currentPosition: CurrentPosition,
    processor: AudioProcessor
) : AbstractAudioPlayer(factory, mixer.name, currentPosition, processor), Runnable {
    private var thread: Thread? = null
    private var sdl: SourceDataLine? = null
    private val bits = 2
    private val channels = 2
    private lateinit var buffers: Array<FloatArray>
    private lateinit var outputBuffer: ByteArray
    override var outputLatency: Int = 0
    override var inputLatency: Int = 0

    init {
        if (sdl != null) {
            sdl!!.close()
            sdl = null
        }

        var le: Throwable? = null
        try {
            tryOpenAudioDevice(currentPosition.sampleRate)
        } catch (e: Throwable) {
            le = e
            for (it in availableSampleRates) {
                try {
                    tryOpenAudioDevice(it)
                    break
                } catch (e2: Throwable) {
                    le = e2
                }
            }
        }

        if (sdl == null) throw le ?: LineUnavailableException("Failed to open audio player!")

        thread = Thread(this, this::class.simpleName)
        thread!!.start()
    }

    override fun close() {
        if (sdl != null) {
            sdl!!.close()
            sdl = null
        }
        if (thread != null) {
            thread!!.interrupt()
            thread = null
        }
        closeCallback?.invoke()
        closeCallback = null
    }

    @Suppress("DuplicatedCode")
    override fun run() {
        val sampleBits = getSampleBits(bits)
        val bufferSize = buffers[0].size
        try {
            while (thread?.isAlive == true && sdl?.isActive == true) {
                enterProcessBlock()
                buffers.forEach { it.fill(0F) }

                try {
                    runBlocking {
                        processor.processBlock(buffers, currentPosition, ArrayList(0))
                    }
                } catch (e: InterruptedException) {
                    throw e
                } catch (e: EOFException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                for (j in 0 until channels) {
                    for (i in 0 until bufferSize) {
                        var value = (buffers[j][i] * sampleBits).toInt()
                        for (k in 0 until bits) {
                            outputBuffer[i * channels * bits + j * bits + k] = value.toByte()
                            value = value shr 8
                        }
                    }
                }
                exitProcessBlock()

                sdl?.write(outputBuffer, 0, outputBuffer.size)

                if (currentPosition.isPlaying) {
                    currentPosition.update(currentPosition.timeInSamples + bufferSize)
                }
            }
        } catch (ignored: InterruptedException) { }
        catch (ignored: EOFException) { } finally {
            close()
        }
    }

    private fun tryOpenAudioDevice(sampleRate: Int) {
        val af = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate.toFloat(), bits * 8, channels,
            bits * channels, sampleRate.toFloat(), false)
        val bufferSize = currentPosition.bufferSize
        val catchBufferSize = channels * bufferSize * bits
        this.sdl = AudioSystem.getSourceDataLine(af, mixer).apply {
            open(af, catchBufferSize)
            start()
        }
        buffers = arrayOf(FloatArray(bufferSize), FloatArray(bufferSize))
        outputLatency = (bufferSize * 1.5).roundToInt()
        inputLatency = outputLatency
        outputBuffer = ByteArray(catchBufferSize)
        processor.prepareToPlay(sampleRate, bufferSize)
        if (currentPosition.sampleRate != sampleRate) currentPosition.setSampleRateAndBufferSize(sampleRate, bufferSize)
    }
}

class JvmAudioPlayerFactory : AudioPlayerFactory {
    override val name = "JVM"
    override suspend fun getPlayers() = AudioSystem.getMixerInfo().map { it.name }
    override fun create(name: String, currentPosition: CurrentPosition, processor: AudioProcessor): JvmAudioPlayer {
        val info = AudioSystem.getMixerInfo()
        val target = info.find { it.name == name }
        var le: Throwable? = null
        return if (target != null) JvmAudioPlayer(this, target, currentPosition, processor)
        else info.firstNotNullOfOrNull {
            try {
                JvmAudioPlayer(this, it, currentPosition, processor)
            } catch (e: Throwable) {
                le = e
                null
            }
        } ?: throw le ?: LineUnavailableException("No audio player with name $name found!")
    }
}
