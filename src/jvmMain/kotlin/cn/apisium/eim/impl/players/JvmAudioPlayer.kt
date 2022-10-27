package cn.apisium.eim.impl.players

import cn.apisium.eim.api.AudioPlayer
import cn.apisium.eim.api.processor.AudioProcessor
import cn.apisium.eim.impl.CurrentPositionImpl
import java.util.*
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

class JvmAudioPlayer : AudioPlayer, Runnable {
    override var processor: AudioProcessor? = null
    override val currentPosition = CurrentPositionImpl()
    private var thread: Thread? = null
    private var sdl: SourceDataLine? = null
    private var sampleRate = 44800F
    private var bufferSize = 0
    private var bits = 2
    private var channels = 2
    private var buffers = arrayOf(FloatArray(bufferSize), FloatArray(bufferSize))
    private var outputBuffer = ByteArray(2 * bufferSize * bits)

    override fun open(sampleRate: Float, bufferSize: Int, bits: Int) {
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

        val af = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, bits * 8, channels, bits * channels, sampleRate, false)
        sdl = AudioSystem.getSourceDataLine(af)
        sdl!!.open(af, outputBuffer.size)
        sdl!!.start()

        if (processor != null) processor!!.prepareToPlay(sampleRate, bufferSize)

        thread = Thread(this)
        thread!!.start()
    }

    override fun setCurrentTime(currentPosition: Long) {

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

    override fun run() {
        while (thread?.isAlive == true) {
            if (sdl == null || processor == null) {
                Thread.sleep(10)
                continue
            }
            buffers.forEach { Arrays.fill(it, 0F) }
            try {
                processor?.processBlock(buffers, currentPosition)
            } catch (e: Exception) {
                e.printStackTrace()
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

            currentPosition.update(currentPosition.timeInSamples, sampleRate)
        }
    }
}