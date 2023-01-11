package cn.apisium.eim.utils.audiosources

import cn.apisium.eim.utils.samplesCount
import org.tritonus.share.sampled.FloatInputStream
import org.tritonus.share.sampled.FloatSampleBuffer
import javax.sound.sampled.AudioInputStream

interface AudioSource {
    val sampleRate: Float
    val channels: Int
    val length: Long
    fun getSamples(start: Int, buffers: Array<FloatArray>)
}

class MemoryAudioSource: AudioSource {
    override val length get() = sampleBuffer[0].size.toLong()
    override val channels get() = sampleBuffer.size
    private var sampleBuffer: Array<FloatArray>
    override var sampleRate: Float = 0F
        private set

    @Suppress("unused")
    constructor(sampleBuffer: Array<FloatArray>, sampleRate: Float) {
        this.sampleBuffer = sampleBuffer
        this.sampleRate = sampleRate
    }

    constructor(stream: AudioInputStream) {
        val format = stream.format
        println(format.sampleRate)
        if (stream.samplesCount != -1L) {
            // println(stream.samplesCount.toInt())
            sampleRate = format.sampleRate
            val buf = FloatSampleBuffer(format.channels, stream.samplesCount.toInt(), format.sampleRate)
            FloatInputStream(stream).use { it.read(buf) }
            sampleBuffer = Array(format.channels) { buf.getChannel(it) }
            return
        }
//        if (format.encoding != AudioFormat.Encoding.PCM_SIGNED || format.sampleSizeInBits !in arrayOf(16, 24, 32) ||
//                format.isBigEndian || format.frameSize != format.channels * format.sampleSizeInBits / 8) {
            throw IllegalArgumentException("Unsupported audio format: $format")
//        }
//        val g = ByteArray(1024)
//        val list = Array(format.channels) { ArrayList<Float>() }
//        sampleBuffer = Array(stream.format.channels) { list[it].toFloatArray() }
    }

    override fun getSamples(start: Int, buffers: Array<FloatArray>) {
        val len = length
        if (start > len) return
        for (i in 0 until channels.coerceAtMost(buffers.size)) {
            val buf = sampleBuffer[i]
            System.arraycopy(buf, start, buffers[i], 0, buffers[i].size.coerceAtMost((len - start).toInt()))
        }
    }
}
