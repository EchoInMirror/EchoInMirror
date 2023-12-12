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
import java.io.File
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.Comparator
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val DEFAULT_SAMPLES_PRE_THUMB_SAMPLE = 32

class AudioThumbnail private constructor(
    val channels: Int,
    val lengthInSamples: Long,
    val sampleRate: Float,
    val samplesPerThumbSample: Int = DEFAULT_SAMPLES_PRE_THUMB_SAMPLE,
    size: Int?,
) {
    val size = size ?: ceil(lengthInSamples / samplesPerThumbSample
        .coerceAtLeast(DEFAULT_SAMPLES_PRE_THUMB_SAMPLE).toDouble()).toInt()
    private val minTree = Array(channels) { ByteArray(this.size * 4 + 1) }
    private val maxTree = Array(channels) { ByteArray(this.size * 4 + 1) }
    private val tempArray = FloatArray(channels * 2)
    private var modification by mutableStateOf<Byte>(0)

    constructor(
        channels: Int, lengthInSamples: Long, sampleRate: Float, samplesPerThumbSample: Int = DEFAULT_SAMPLES_PRE_THUMB_SAMPLE
    ): this(channels, lengthInSamples, sampleRate, samplesPerThumbSample, null)

    @OptIn(DelicateCoroutinesApi::class)
    constructor(
        source: AudioSource, samplesPerThumbSample: Int = DEFAULT_SAMPLES_PRE_THUMB_SAMPLE,
        onComplete: ((audioThumbnail: AudioThumbnail?, cause: Throwable?) -> Unit)? = null
    ): this(source.channels, source.length, source.sampleRate, samplesPerThumbSample) {
        val spts = this.samplesPerThumbSample
        GlobalScope.launch(Dispatchers.Default) {
            val times = 1024 / spts
            val bufferSize = times * spts
            val buffers = Array(channels) { FloatArray(bufferSize) }
            var i = 1
            var read = 0
            out@while (source.nextBlock(buffers, bufferSize).also { read += it } > 0 && read < lengthInSamples) {
                repeat(times) { k ->
                    val curIndex = k * spts
                    repeat(channels) { ch ->
                        var min: Byte = 127
                        var max: Byte = -128
                        val channel = buffers[ch]
                        repeat(spts) { j ->
                            val amp = channel[j + curIndex]
                            val v = (amp * 127F).roundToInt().coerceIn(-128, 127).toByte()
                            if (amp < min) min = v
                            if (amp > max) max = v
                        }
                        minTree[ch][i] = min
                        maxTree[ch][i] = max
                    }
                    i++
                }
            }
            buildTree()
            modification++
        }.apply {
            if (onComplete != null) invokeOnCompletion { cause ->
                onComplete(if (cause == null) this@AudioThumbnail else null, cause)
            }
        }
    }

    constructor(data: ByteBuffer): this(data.get().toInt(), data.long, data.float, data.int, data.int) {
        repeat(channels) {
            data.get(minTree[it], 1, size)
            data.get(maxTree[it], 1, size)
        }
        buildTree()
        modification++
    }

    constructor(file: Path): this(Files.newByteChannel(file)
        .use {
            val data = ByteBuffer.allocateDirect(it.size().toInt())
            it.read(data)
            data.flip()
            data
        })

    fun writeTo(file: Path) {
        Files.newByteChannel(
            file,
            StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
        ).use { it.write(toByteBuffer()) }
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

    @Suppress("MemberVisibilityCanBePrivate")
    fun toByteBuffer(): ByteBuffer {
        val data = ByteBuffer.allocateDirect(21 + channels * size * 2)
            .put(channels.toByte()) // 1
            .putLong(lengthInSamples) // 8
            .putFloat(sampleRate) // 4
            .putInt(samplesPerThumbSample) // 4
            .putInt(size) // 4
        repeat(channels) {
            data.put(minTree[it], 1, size).put(maxTree[it], 1, size)
        }
        data.flip()
        return data
    }

    fun copy() = AudioThumbnail(channels, lengthInSamples, sampleRate, samplesPerThumbSample, size).apply {
        repeat(channels) {
            minTree[it].copyInto(this@AudioThumbnail.minTree[it])
            maxTree[it].copyInto(this@AudioThumbnail.maxTree[it])
        }
    }
}

private fun sha1(path: String): String {
    val bytes = path.toByteArray()
    val sha1 = java.security.MessageDigest.getInstance("SHA-1").digest(bytes)
    val sb = StringBuilder()
    sha1.forEach { sb.append(String.format("%02x", it)) }
    return sb.toString()
}

class AudioThumbnailCache(
    private val dir: Path, private val samplesPerThumbSample: Int = DEFAULT_SAMPLES_PRE_THUMB_SAMPLE
): AutoCloseable {
    private val map = ConcurrentHashMap<String, WeakReference<AudioThumbnail>>()

    private fun getKey(path: Path) =
        sha1(path.toString() + try {
            Files.getLastModifiedTime(path).toMillis()
        } catch (e: Throwable) {
            0L
        })
    operator fun set(path: Path, value: AudioThumbnail) {
        val key = getKey(path)
        map[key] = WeakReference(value)
        val file = dir.resolve(key)
        if (!Files.exists(file)) {
            Files.createDirectories(dir)
            value.writeTo(file)
        }
    }
    operator fun contains(path: Path) = getKey(path).let { map[it]?.get() != null || Files.exists(dir.resolve(it)) }

    fun get(
        file: File, audioSource: AudioSource? = null,
        onComplete: ((audioThumbnail: AudioThumbnail?, cause: Throwable?) -> Unit)
    ) { get(file.toPath(), audioSource, onComplete) }

    @OptIn(DelicateCoroutinesApi::class)
    fun get(
        file: Path, audioSource: AudioSource? = null,
        onComplete: ((audioThumbnail: AudioThumbnail?, cause: Throwable?) -> Unit)
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            val key = getKey(file)
            val obj = map[key]?.get()
            if (obj != null) {
                onComplete(obj, null)
                return@launch
            }
            val f = dir.resolve(key)
            try {
                if (Files.exists(f)) {
                    val value = AudioThumbnail(f)
                    map[key] = WeakReference(value)
                    onComplete(value, null)
                    return@launch
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            map[key] = WeakReference(AudioThumbnail(
                audioSource ?: AudioSourceManager.createProxyFileSource(file),
                samplesPerThumbSample
            ) { v, err ->
                onComplete.invoke(v, err)
                if (v != null && err == null) {
                    Files.createDirectories(dir)
                    if (Files.exists(f)) return@AudioThumbnail
                    v.writeTo(f)
                }
            })
        }
    }

    override fun close() {
        map.clear()
        if (!Files.exists(dir)) return
        try {
            Files.list(dir)
                .sorted(Comparator.comparingLong {
                    Files.readAttributes(it, BasicFileAttributes::class.java).lastAccessTime().toMillis()
                })
                .limit(200)
                .forEach { Files.delete(it) }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
