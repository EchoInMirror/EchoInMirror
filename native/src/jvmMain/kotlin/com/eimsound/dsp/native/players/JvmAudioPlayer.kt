package com.eimsound.dsp.native.players

import com.eimsound.audioprocessor.*
import com.eimsound.daw.utils.lowerBound
import com.eimsound.dsp.native.getSampleBits
import kotlinx.coroutines.runBlocking
import java.io.EOFException
import javax.sound.sampled.*
import kotlin.math.roundToInt

private val FORMATS by lazy {
    arrayOf(16000, 22050, 24000, 32000, 44100, 48000, 88200, 96000)
        .map { AudioFormat(AudioFormat.Encoding.PCM_SIGNED, it.toFloat(), 2 * 8, 2,
            2 * 2, it.toFloat(), false) }
}

class JvmAudioPlayer(
    factory: JvmAudioPlayerFactory,
    private val mixer: Mixer.Info,
    currentPosition: MutableCurrentPosition,
    processor: AudioProcessor,
    preferredSampleRate: Int?
) : AbstractAudioPlayer(factory, mixer.name, 2, currentPosition, processor, preferredSampleRate), Runnable {
    private var thread: Thread? = null
    private var sdl: SourceDataLine? = null
    private val bits = 2
    private lateinit var outputBuffer: ByteArray
    override var outputLatency: Int = 0
    override var inputLatency: Int = 0
    override val availableSampleRates = AudioSystem.getMixer(mixer).sourceLineInfo.flatMap { info ->
        if (info is DataLine.Info) {
            FORMATS.filter { info.isFormatSupported(it) }.map { it.sampleRate.toInt() }
        }
        else emptyList()
    }.distinct()

    init {
        if (sdl != null) {
            sdl!!.close()
            sdl = null
        }

        var le: Throwable? = null
        try {
            if (!availableSampleRates.contains(sampleRate)) {
                sampleRate = availableSampleRates.getOrNull(availableSampleRates.lowerBound { it < currentPosition.sampleRate })
                    ?: throw LineUnavailableException("Failed to open audio player!")
            }
            tryOpenAudioDevice()
        } catch (e: Throwable) {
            le = e
        }

        if (sdl == null) throw le ?: LineUnavailableException("Failed to open audio player!")

        thread = Thread(this, this::class.simpleName).apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
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
        try {
            while (thread?.isAlive == true && sdl?.isOpen == true) {
                enterProcessBlock()

                try {
                    runBlocking {
                        val buffers = process()
                        val bufferSize = currentPosition.bufferSize
                        for (j in 0 until channels) {
                            for (i in 0 until bufferSize) {
                                var value = (buffers[j][i] * sampleBits).toInt()
                                for (k in 0 until bits) {
                                    outputBuffer[i * channels * bits + j * bits + k] = value.toByte()
                                    value = value shr 8
                                }
                            }
                        }
                    }
                } catch (e: InterruptedException) {
                    throw e
                } catch (e: EOFException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                exitProcessBlock()

                sdl?.write(outputBuffer, 0, outputBuffer.size)
            }
        } catch (ignored: InterruptedException) { }
        catch (ignored: EOFException) { } finally {
            close()
        }
    }

    private fun tryOpenAudioDevice() {
        val af = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate.toFloat(), bits * 8, channels,
            bits * channels, sampleRate.toFloat(), false)
        val bufferSize = currentPosition.bufferSize
        val catchBufferSize = channels * bufferSize * bits
        this.sdl = AudioSystem.getSourceDataLine(af, mixer).apply {
            open(af, catchBufferSize)
            start()
        }
        outputLatency = (bufferSize * 1.5).roundToInt()
        inputLatency = outputLatency
        outputBuffer = ByteArray(catchBufferSize)
        runBlocking { prepareToPlay() }
    }
}

class JvmAudioPlayerFactory : AudioPlayerFactory {
    override val name = "JVM"
    override suspend fun getPlayers() = AudioSystem.getMixerInfo().mapNotNull {
        if (AudioSystem.getMixer(it).sourceLineInfo.any { i -> i is DataLine.Info }) it.name
        else null
    }
    override fun create(
        name: String, currentPosition: MutableCurrentPosition, processor: AudioProcessor,
        preferredSampleRate: Int?
    ): JvmAudioPlayer {
        val info = AudioSystem.getMixerInfo()
        val target = info.find { it.name == name }
        var le: Throwable? = null
        return if (target != null) JvmAudioPlayer(this, target, currentPosition, processor, preferredSampleRate)
        else info.firstNotNullOfOrNull {
            try {
                JvmAudioPlayer(this, it, currentPosition, processor, preferredSampleRate)
            } catch (e: Throwable) {
                le = e
                null
            }
        } ?: throw le ?: LineUnavailableException("No audio player with name $name found!")
    }
}
