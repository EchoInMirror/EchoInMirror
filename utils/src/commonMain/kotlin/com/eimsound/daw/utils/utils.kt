package com.eimsound.daw.utils

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import io.github.oshai.kotlinlogging.KLogger
import java.nio.file.Files
import kotlin.math.ceil
import kotlin.math.roundToInt

typealias FloatRange = ClosedFloatingPointRange<Float>

fun mapValue(value: Int, start1: Int, stop1: Int) = ((value - start1) / (stop1 - start1).toFloat()).coerceIn(0f, 1f)
fun mapValue(value: Float, start1: Float, stop1: Float) = ((value - start1) / (stop1 - start1)).coerceIn(0f, 1f)
fun mapValue(value: Float, range: IntRange) = ((value - range.first) / range.range).coerceIn(0f, 1f)
fun mapValue(value: Float, range: FloatRange) = ((value - range.start) / range.range).coerceIn(0f, 1f)

fun randomId(): String = NanoIdUtils.randomNanoId(NanoIdUtils.DEFAULT_NUMBER_GENERATOR, NanoIdUtils.DEFAULT_ALPHABET, 8)

fun Int.fitInUnit(unit: Int) = this / unit * unit
fun Int.fitInUnitCeil(unit: Int) = ceil(toDouble() / unit).toInt() * unit

fun Float.fitInUnit(unit: Int) = (this / unit).roundToInt() * unit

inline fun <T> List<T>.binarySearch(comparator: (T) -> Boolean): Int {
    var l = 0
    var r = size - 1
    while (l < r) {
        val mid = (l + r) ushr 1
        if (comparator(this[mid])) l = mid + 1
        else r = mid
    }
    return l
}

@Suppress("NOTHING_TO_INLINE")
inline fun Float.coerceIn(range: IntRange) = coerceIn(range.first.toFloat(), range.last.toFloat())

val IntRange.range get() = last - first
val FloatRange.range get() = endInclusive - start

inline fun <T> tryOrNull(logger: KLogger? = null, message: String? = null, block: () -> T): T? {
    return try { block() } catch (e: Throwable) {
        logger?.error(e) { "$message" }
        null
    }
}

fun createTempDirectory(prefix: String? = null) = Files.createTempDirectory(
    System.getProperty("eim.tempfiles.prefix", "EchoInMirror") + (if (prefix == null) "" else "-$prefix")
)
