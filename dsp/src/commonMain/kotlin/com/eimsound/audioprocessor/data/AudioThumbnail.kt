package com.eimsound.audioprocessor.data

import com.eimsound.audioprocessor.AudioSource
import com.eimsound.audioprocessor.AudioSourceManager
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val DEFAULT_SAMPLES_PRE_THUMB_SAMPLE = 32

class AudioThumbnail private constructor(
    val channels: Int,
    @Suppress("MemberVisibilityCanBePrivate") val lengthInSamples: Long,
    val sampleRate: Float,
    val samplesPerThumbSample: Int = DEFAULT_SAMPLES_PRE_THUMB_SAMPLE,
    size: Int?,
) {
    val size = size ?: ceil(lengthInSamples / samplesPerThumbSample.coerceAtLeast(DEFAULT_SAMPLES_PRE_THUMB_SAMPLE).toDouble()).toInt()
    private val minTree = Array(channels) { ByteArray(this.size * 4 + 1) }
    private val maxTree = Array(channels) { ByteArray(this.size * 4 + 1) }
    constructor(channels: Int,
                lengthInSamples: Long,
                sampleRate: Float,
                samplesPerThumbSample: Int = DEFAULT_SAMPLES_PRE_THUMB_SAMPLE
    ): this(channels, lengthInSamples, sampleRate, samplesPerThumbSample, null)

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
        buildTree()
    }

    constructor(data: ByteBuffer): this(data.get().toInt(), data.long, data.float, data.int, data.int) {
        repeat(channels) {
            data.get(minTree[it], 1, size)
            data.get(maxTree[it], 1, size)
        }
        buildTree()
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

    private fun buildTree() {
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

    fun toByteArray(): ByteArray {
        val data = ByteBuffer.allocate(21 + channels * size)
        data.put(channels.toByte()) // 1
        data.putLong(lengthInSamples) // 8
        data.putFloat(sampleRate) // 4
        data.putInt(samplesPerThumbSample) // 4
        data.putInt(size) // 4
        repeat(channels) {
            data.put(minTree[it], 1, size)
            data.put(maxTree[it], 1, size)
        }
        return data.array()
    }
}

class AudioThumbnailCache(file: File): AutoCloseable {
    private val db = Iq80DBFactory.factory.open(file, Options().createIfMissing(true))

    operator fun get(key: String) = db.get(key.toByteArray())?.let { AudioThumbnail(ByteBuffer.wrap(it)) }
    operator fun set(key: String, value: AudioThumbnail) = db.put(key.toByteArray(), value.toByteArray())
    operator fun contains(key: String) = db[key.toByteArray()] != null
    fun remove(key: String) = db.delete(key.toByteArray())
    operator fun get(file: Path): AudioThumbnail? {
        try {
            val real = file.toRealPath()
            val key = real.absolutePathString()
            val timeKey = "$key|time".toByteArray()
            val time = Files.getLastModifiedTime(real).toMillis()
            if (db.get(timeKey)?.let { ByteBuffer.wrap(it).long } == time) {
                val value = get(key)
                if (value != null) return value
            }
            val value = AudioThumbnail(AudioSourceManager.instance.createAudioSource(real.toFile()))
            set(key, value)
            db.put(timeKey, ByteBuffer.allocate(8).putLong(time).array())
            return value
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    override fun close() = db.close()
}
