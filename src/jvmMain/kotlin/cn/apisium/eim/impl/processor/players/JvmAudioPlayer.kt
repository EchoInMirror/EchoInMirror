package cn.apisium.eim.impl.processor.players

import cn.apisium.eim.api.AbstractAudioPlayer
import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.api.getAudioFormat
import cn.apisium.eim.api.processor.AudioProcessor
import cn.apisium.eim.utils.getSampleBits
import kotlinx.coroutines.runBlocking
import java.util.*
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

class JvmAudioPlayer(currentPosition: CurrentPosition, processor: AudioProcessor) :
    AbstractAudioPlayer(currentPosition, processor), Runnable {
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

        val af = currentPosition.getAudioFormat(bits, channels)
        sdl = AudioSystem.getSourceDataLine(af)
        sdl!!.open(af, outputBuffer.size)
        sdl!!.start()

        processor.prepareToPlay(sampleRate, bufferSize)

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
        val sampleBits = getSampleBits(bits)
        while (thread?.isAlive == true) {
            if (sdl == null) {
                Thread.sleep(10)
                continue
            }

            enterProcessBlock()
            buffers.forEach { it.fill(0F) }

            try {
                runBlocking {
                    processor.processBlock(buffers, currentPosition, ArrayList(0))
                }
            } catch (ignored: InterruptedException) { } catch (e: Exception) {
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
    }
}