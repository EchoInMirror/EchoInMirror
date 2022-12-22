package cn.apisium.eim.impl

import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.api.Renderable
import cn.apisium.eim.api.Renderer
import javax.sound.sampled.*
import java.io.*
import java.nio.ByteBuffer

class RenderPosition : CurrentPosition {
    override var bpm = 140.0
    override var timeInSamples = 0L
    override var timeInSeconds = 0.0
    override var ppq = 96
    override var timeInPPQ = 0
    override var ppqPosition = 0.0
    override var bufferSize = 1024
    override var sampleRate = 44800
    override var timeSigNumerator = 4
    override var timeSigDenominator = 4
    override var isPlaying: Boolean = false

    constructor(other: CurrentPosition) {
        bpm = other.bpm
        timeInSamples = other.timeInSamples
        timeInSeconds = other.timeInSeconds
        ppq = other.ppq
        timeInPPQ = other.timeInPPQ
        ppqPosition = other.ppqPosition
        bufferSize = other.bufferSize
        sampleRate = other.sampleRate
        timeSigNumerator = other.timeSigNumerator
        timeSigDenominator = other.timeSigDenominator
        isPlaying = other.isPlaying
    }

    constructor(ppq: Int, sampleRate: Int, timeInPPQ: Int) {
        this.ppq = ppq
        this.sampleRate = sampleRate
        this.timeInPPQ = timeInPPQ
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
        this.timeInSamples = timeInSamples.coerceAtLeast(0).toLong()
        this.timeInSeconds =
            this.timeInSamples.toDouble() / this.sampleRate
        this.ppqPosition = this.timeInSeconds / 60.0 * this.bpm
        this.timeInPPQ = (this.ppqPosition * this.ppq).toInt()
    }
}

class RendererImpl(renderTarget: Renderable) :
    Renderer(renderTarget) {
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
        this.renderTarget.onRenderStart()
        val channels = 2
        val bits = 2
        val currentRenderPosition = RenderPosition(ppq, sampleRate, startPosition)
        val format = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED, sampleRate.toFloat(), bits * 8, channels,
            bits * channels, sampleRate.toFloat(), false
        )
        val resbuffer = ByteBuffer.allocate(100000000)
        while (currentRenderPosition.timeInPPQ <= endPosition) {
            val buffers =
                arrayOf(FloatArray(currentRenderPosition.bufferSize), FloatArray(currentRenderPosition.bufferSize))
            val midiBuffer = ArrayList<Int>()
            this.renderTarget.processBlock(buffers, currentRenderPosition, midiBuffer)

            currentRenderPosition.update(currentRenderPosition.timeInSamples+currentRenderPosition.bufferSize)

            callback(((currentRenderPosition.timeInPPQ - startPosition) / (endPosition - startPosition)).toFloat())

            val outBuffer = ByteArray(channels * currentRenderPosition.bufferSize * bits)
            // TODO value of channels will change in future
            for (j in 0 until channels) {
                for (i in 0 until currentRenderPosition.bufferSize) {
                    var value = (buffers[j][i] * 32767).toInt()
                    for (k in 0 until bits) {
                        outBuffer[i * channels * bits + j * bits + k] = value.toByte()
                        value = value shr 8
                    }
                }
            }
            resbuffer.put(outBuffer)
        }
        resbuffer.flip()
        val outByte = ByteArray(resbuffer.limit()-resbuffer.position())
        resbuffer.get(outByte,0,outByte.size)
        val bais = ByteArrayInputStream(outByte)
        val ais = AudioInputStream(
            bais, format,
            outByte.size.toLong()
        )
        AudioFileFormat.Type.WAVE
        AudioSystem.write(ais, audioType, file)
        this.renderTarget.onRenderEnd()
    }
}