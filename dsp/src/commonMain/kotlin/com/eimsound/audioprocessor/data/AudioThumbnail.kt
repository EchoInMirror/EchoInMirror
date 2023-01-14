package com.eimsound.audioprocessor.data

import com.eimsound.audioprocessor.AudioSource
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val DEFAULT_SAMPLES_PRE_THUMB_SAMPLE = 32

class AudioThumbnail(
    val channels: Int,
    @Suppress("MemberVisibilityCanBePrivate") val lengthInSamples: Long,
    val sampleRate: Float,
    val samplesPerThumbSample: Int = DEFAULT_SAMPLES_PRE_THUMB_SAMPLE
) {
    val size = ceil(lengthInSamples / samplesPerThumbSample.coerceAtLeast(DEFAULT_SAMPLES_PRE_THUMB_SAMPLE).toDouble()).toInt()
    private val minTree = Array(channels) { ByteArray(size * 4 + 1) }
    private val maxTree = Array(channels) { ByteArray(size * 4 + 1) }

    constructor(source: AudioSource, samplesPerThumbSample: Int = DEFAULT_SAMPLES_PRE_THUMB_SAMPLE):
            this(source.channels, source.length, source.sampleRate, samplesPerThumbSample) {
        val buffers = Array(channels) { FloatArray(this.samplesPerThumbSample) }
        var pos = 0L
        var i = 1
        while (pos <= source.length) {
            if (source.getSamples(pos, buffers) == 0) break
            repeat(channels) { ch ->
                var min: Byte = 127
                var max: Byte = -128
                buffers[ch].forEach {
                    val v = (it * 127F).roundToInt().coerceIn(-128, 127).toByte()
                    if (it < min) min = v
                    if (it > max) max = v
                }
                minTree[ch][i] = min
                maxTree[ch][i] = max
            }
            i++
            pos += this.samplesPerThumbSample
        }
        repeat(channels) {
            val minT = minTree[it]
            val maxT = maxTree[it]
            for (k in 1..size) {
                val j = k + k.takeLowestOneBit()
                if (j <= size) {
                    if (minT[k] < minT[k]) minT[k] = minT[k]
                    if (maxT[k] > maxT[k]) maxT[k] = maxT[k]
                }
            }
        }
    }

    fun query(x: Int, y: Int): FloatArray {
        val data = FloatArray(channels * 2)
        repeat(channels) {
            @Suppress("NAME_SHADOWING") var y = y
            var min: Byte = 127
            var max: Byte = -128
            while (y >= x) {
                if (minTree[it][y] < min) min = minTree[it][y]
                if (maxTree[it][y] > max) max = maxTree[it][y]
                y--
                while(y - y.takeLowestOneBit() >= x) {
                    if (minTree[it][y] < min) min = minTree[it][y]
                    if (maxTree[it][y] > max) max = maxTree[it][y]
                    y -= y.takeLowestOneBit()
                }
            }
            data[it * 2] = min / 127F
            data[it * 2 + 1] = max / 127F
        }
        return data
    }

    inline fun query(widthInPx: Double, startTimeSeconds: Double = 0.0,
                     endTimeSeconds: Double = samplesPerThumbSample / sampleRate.toDouble(),
                     stepInPx: Float = 1F,
                     callback: (x: Float, channel: Int, min: Float, max: Float) -> Unit) {
        var x = startTimeSeconds * sampleRate / samplesPerThumbSample + 1
        val end = (endTimeSeconds * sampleRate / samplesPerThumbSample + 1).coerceAtMost(size.toDouble())
        val step = (end - x) / widthInPx * stepInPx
        var i = 0
        while (x <= end) {
            val y = x + step
            if (y > end) return
            val minMax = query(x.toInt(), y.toInt())
            repeat(channels) { ch -> callback(i * stepInPx, ch, minMax[ch * 2], minMax[ch * 2 + 1]) }
            i++
            x = y
        }
    }
}
