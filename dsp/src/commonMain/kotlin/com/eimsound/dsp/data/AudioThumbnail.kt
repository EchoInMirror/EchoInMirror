package com.eimsound.dsp.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.audiosources.AudioSource
import com.eimsound.audiosources.AudioSourceManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.mapdb.DBMaker
import org.mapdb.HTreeMap
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val DEFAULT_SAMPLES_PRE_THUMB_SAMPLE = 32

class AudioThumbnail private constructor(
    modifiedTime: Long = 0L,
    val channels: Int,
    val lengthInSamples: Long,
    val sampleRate: Float,
    val samplesPerThumbSample: Int = DEFAULT_SAMPLES_PRE_THUMB_SAMPLE,
    size: Int?,
) {
    var modifiedTime = modifiedTime
        internal set
    val size = size ?: ceil(lengthInSamples / samplesPerThumbSample
        .coerceAtLeast(DEFAULT_SAMPLES_PRE_THUMB_SAMPLE).toDouble()).toInt()
    private val minTree = Array(channels) { ByteArray(this.size * 4 + 1) }
    private val maxTree = Array(channels) { ByteArray(this.size * 4 + 1) }
    private val tempArray = FloatArray(channels * 2)
    private var modification by mutableStateOf<Byte>(0)

    constructor(
        channels: Int, lengthInSamples: Long, sampleRate: Float, samplesPerThumbSample: Int = DEFAULT_SAMPLES_PRE_THUMB_SAMPLE
    ): this(0L, channels, lengthInSamples, sampleRate, samplesPerThumbSample, null)

    @OptIn(DelicateCoroutinesApi::class)
    constructor(
        source: AudioSource, samplesPerThumbSample: Int = DEFAULT_SAMPLES_PRE_THUMB_SAMPLE,
        onComplete: ((cause: Throwable?) -> Unit)? = null
    ): this(source.channels, source.length, source.sampleRate, samplesPerThumbSample) {
        GlobalScope.launch(Dispatchers.IO) {
            val times = 1024 / this@AudioThumbnail.samplesPerThumbSample
            val buffers = Array(channels) { FloatArray(times * this@AudioThumbnail.samplesPerThumbSample) }
            var pos = 0L
            var i = 1
            out@while (pos <= source.length) {
                if (source.getSamples(pos, 0, times * this@AudioThumbnail.samplesPerThumbSample, buffers) < 1) break
                for (k in 0 until times) {
                    val curIndex = k * this@AudioThumbnail.samplesPerThumbSample
                    repeat(channels) { ch ->
                        var min: Byte = 127
                        var max: Byte = -128
                        val channel = buffers[ch]
                        repeat(this@AudioThumbnail.samplesPerThumbSample) { j ->
                            val amp = channel[j + curIndex]
                            val v = (amp * 127F).roundToInt().coerceIn(-128, 127).toByte()
                            if (amp < min) min = v
                            if (amp > max) max = v
                        }
                        minTree[ch][i] = min
                        maxTree[ch][i] = max
                    }
                    i++
                    pos += this@AudioThumbnail.samplesPerThumbSample
                    if (pos > source.length) break@out
                }
            }
            buildTree()
            modification++
        }.apply {
            if (onComplete != null) invokeOnCompletion(onComplete)
        }
    }

    constructor(data: ByteBuffer): this(
        data.order(ByteOrder.LITTLE_ENDIAN).long, data.get().toInt(), data.long, data.float, data.int, data.int
    ) {
        repeat(channels) {
            data.get(minTree[it], 1, size)
            data.get(maxTree[it], 1, size)
        }
        buildTree()
        modification++
    }

    fun read() { modification }

    fun query(x: Int, y: Int): FloatArray {
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
            tempArray[it * 2] = min / 127F
            tempArray[it * 2 + 1] = max / 127F
        }
        return tempArray
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

    inline fun query(
        widthInPx: Double, startTimeSeconds: Double = 0.0,
        endTimeSeconds: Double = lengthInSamples / sampleRate.toDouble(),
        stepInPx: Float = 1F,
        callback: (x: Float, channel: Int, min: Float, max: Float) -> Unit
    ) {
        var x = startTimeSeconds * sampleRate / samplesPerThumbSample + 1
        val end = (endTimeSeconds * sampleRate / samplesPerThumbSample + 1).coerceAtMost(size.toDouble())
        val step = (end - x) / widthInPx * stepInPx
        if (x == end || step == 0.0 || step.isNaN()) return
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
        val data = ByteBuffer.allocate(29 + channels * size * 2).order(ByteOrder.LITTLE_ENDIAN)
            .putLong(modifiedTime) // 8
            .put(channels.toByte()) // 1
            .putLong(lengthInSamples) // 8
            .putFloat(sampleRate) // 4
            .putInt(samplesPerThumbSample) // 4
            .putInt(size) // 4
        repeat(channels) {
            data.put(minTree[it], 1, size).put(maxTree[it], 1, size)
        }
        return data.array()
    }

    fun copy() = AudioThumbnail(modifiedTime, channels, lengthInSamples, sampleRate, samplesPerThumbSample, size).apply {
        repeat(channels) {
            minTree[it].copyInto(this@AudioThumbnail.minTree[it])
            maxTree[it].copyInto(this@AudioThumbnail.maxTree[it])
        }
    }
}

class AudioThumbnailCache(
    private val file: File, private val samplesPerThumbSample: Int = DEFAULT_SAMPLES_PRE_THUMB_SAMPLE
): AutoCloseable {
    @Suppress("UNCHECKED_CAST")
    private val db = DBMaker.memoryDB().make().hashMap("audioThumbnails").expireMaxSize(200).createOrOpen() as HTreeMap<String, ByteArray>

    operator fun get(key: String) = db[key]?.let { AudioThumbnail(ByteBuffer.wrap(it)) }
    operator fun set(key: String, time: Long = Files.getLastModifiedTime(Paths.get(key)).toMillis(), value: AudioThumbnail) {
        value.modifiedTime = time
        db[key] = value.toByteArray()
    }
    operator fun contains(key: String) = db[key] != null
    fun remove(key: String) = db.remove(key)
    operator fun get(
        file: File, audioSource: AudioSource? = null, onComplete: ((cause: Throwable?) -> Unit)? = null
    ): AudioThumbnail? = get(file.toPath(), audioSource, onComplete)
    operator fun get(
        file: Path, audioSource: AudioSource? = null, onComplete: ((cause: Throwable?) -> Unit)? = null
    ): AudioThumbnail? {
        try {
            val real = file.toRealPath()
            val time = Files.getLastModifiedTime(real).toMillis()
            val byteArray = db[file.toString()]
            if (byteArray != null) {
                val buf = ByteBuffer.wrap(byteArray)
                if (buf.long == time) {
                    onComplete?.invoke(null)
                    return AudioThumbnail(buf.position(0))
                }
            }
            val value = AudioThumbnail(
                audioSource ?: AudioSourceManager.instance.createAudioSource(real),
                samplesPerThumbSample, onComplete
            )
            set(file.toString(), time, value)
            return value
        } catch (e: Throwable) {
            e.printStackTrace()
            onComplete?.invoke(e)
            return null
        }
    }

    override fun close() = db.close()
}
