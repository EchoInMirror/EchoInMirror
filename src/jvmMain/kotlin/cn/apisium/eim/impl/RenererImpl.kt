package cn.apisium.eim.impl
import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.api.Renderable
import cn.apisium.eim.api.Renderer
import javax.sound.sampled.*
import java.io.*

class RenderPosition:CurrentPosition{
    override var bpm =140.0
    override var timeInSamples = 0L
    override var timeInSeconds =  0.0
    override var ppq = 96
    override var timeInPPQ = 0
    override var ppqPosition =0.0
    override var bufferSize = 1024
    override var sampleRate = 44800
    override var timeSigNumerator =4
    override var timeSigDenominator =4
    override var isPlaying: Boolean = false

    constructor(other:CurrentPosition){
        bpm =other.bpm
        timeInSamples = other.timeInSamples
        timeInSeconds =  other.timeInSeconds
        ppq = other.ppq
        timeInPPQ = other.timeInPPQ
        ppqPosition =other.ppqPosition
        bufferSize = other.bufferSize
        sampleRate = other.sampleRate
        timeSigNumerator =other.timeSigNumerator
        timeSigDenominator =other.timeSigDenominator
        isPlaying = other.isPlaying
    }
    override val ppqCountOfBlock: Int
        get() = (bufferSize / sampleRate / 60.0 * bpm * ppq).toInt()
    override fun convertPPQToSamples(ppq: Int): Long {
        throw UnsupportedOperationException()
    }
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

class RendererImpl(formatEncoding:AudioFormat.Encoding, renderTarget:Renderable): Renderer(formatEncoding, renderTarget){
    override suspend fun start(startPosition: CurrentPosition, endPosition: CurrentPosition, file:File, audioType:AudioFileFormat.Type, callback: (Float) -> Unit) {
        this.renderTarget.onRenderStart()
        val channels = 2
        val bits = 2
        val currentRenderPosition = RenderPosition(startPosition)
        val format = AudioFormat(formatEncoding, startPosition.sampleRate.toFloat(), bits * 8, channels,
            bits * channels, startPosition.sampleRate.toFloat(), false)
        while (endPosition.convertPPQToSamples(currentRenderPosition.ppq) <= endPosition.timeInSamples)
        {
            val buffers = arrayOf(FloatArray(currentRenderPosition.bufferSize),FloatArray(currentRenderPosition.bufferSize))
            val midiBuffer = ArrayList<Int>()
            this.renderTarget.processBlock(buffers, currentRenderPosition, midiBuffer)

            currentRenderPosition.timeInSamples = currentRenderPosition.bufferSize.coerceAtLeast(0).toLong()
            currentRenderPosition.timeInSeconds = currentRenderPosition.timeInSamples.toDouble() / currentRenderPosition.sampleRate
            currentRenderPosition.ppqPosition = currentRenderPosition.timeInSeconds / 60.0 * currentRenderPosition.bpm
            currentRenderPosition.timeInPPQ = (currentRenderPosition.ppqPosition * currentRenderPosition.ppq).toInt()

            // TODO value of channels will change in future
            val outBuffer = ByteArray(channels*currentRenderPosition.bufferSize*bits)
            for (j in 0 until channels) {
                for (i in 0 until currentRenderPosition.bufferSize) {
                    var value = (buffers[j][i] * 32767).toInt()
                    for (k in 0 until bits) {
                        outBuffer[i * channels * bits + j * bits + k] = value.toByte()
                        value = value shr 8
                    }
                }
            }
            val bais = ByteArrayInputStream(outBuffer)
            val ais = AudioInputStream(
                bais, format,
                outBuffer.size.toLong()
            )
            AudioSystem.write(ais, audioType, file)

            callback(((startPosition.convertPPQToSamples(currentRenderPosition.ppq)-startPosition.timeInSamples)/(endPosition.timeInSamples-startPosition.timeInSamples)).toFloat())
        }
        this.renderTarget.onRenderEnd()
    }
}