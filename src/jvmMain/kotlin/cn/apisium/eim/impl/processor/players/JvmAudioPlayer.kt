package cn.apisium.eim.impl.processor.players

import cn.apisium.eim.api.AudioPlayer
import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.api.processor.AudioProcessor
import kotlinx.coroutines.runBlocking
import java.util.*
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

class JvmAudioPlayer(currentPosition: CurrentPosition, processor: AudioProcessor?) : AudioPlayer(currentPosition, processor), Runnable {
    private var thread: Thread? = null
    private var sdl: SourceDataLine? = null
    private var sampleRate = 44800
    private var bufferSize = 0
    private var bits = 2
    private var channels = 2
    private var buffers = arrayOf(FloatArray(bufferSize), FloatArray(bufferSize))
    private var outputBuffer = ByteArray(2 * bufferSize * bits)

    override fun open(sampleRate: Int, bufferSize: Int, bits: Int) {
        if (thread != null) return
        if (sdl != null) {
            sdl!!.close()
            sdl = null
        }

        this.sampleRate = sampleRate
        this.bufferSize = bufferSize
        this.bits = bits
        buffers = arrayOf(FloatArray(bufferSize), FloatArray(bufferSize))
        outputBuffer = ByteArray(channels * bufferSize * bits)

        val af = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate.toFloat(), bits * 8, channels,
            bits * channels, sampleRate.toFloat(), false)
        sdl = AudioSystem.getSourceDataLine(af)
        sdl!!.open(af, outputBuffer.size)
        sdl!!.start()

        if (processor != null) processor!!.prepareToPlay()

        thread = Thread(this)
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
    }

    @Suppress("DuplicatedCode")
    override fun run() {
        while (thread?.isAlive == true) {
            if (sdl == null || processor == null) {
                Thread.sleep(10)
                continue
            }
            buffers.forEach { Arrays.fill(it, 0F) }

            runBlocking {
                try {
                    processor?.processBlock(buffers, currentPosition, ArrayList(0))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            for (j in 0 until channels) {
                for (i in 0 until bufferSize) {
                    var value = (buffers[j][i] * 32767).toInt()
                    for (k in 0 until bits) {
                        outputBuffer[i * channels * bits + j * bits + k] = value.toByte()
                        value = value shr 8
                    }
                }
            }
            sdl!!.write(outputBuffer, 0, outputBuffer.size)

            if (currentPosition.isPlaying) {
                currentPosition.update(currentPosition.timeInSamples + bufferSize)
            }
        }
    }
}