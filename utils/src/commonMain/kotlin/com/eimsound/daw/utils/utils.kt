package com.eimsound.daw.utils

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import kotlin.math.ceil
import kotlin.math.roundToInt

fun mapValue(value: Int, start1: Int, stop1: Int) = ((value - start1) / (stop1 - start1).toFloat()).coerceIn(0f, 1f)
fun mapValue(value: Float, start1: Float, stop1: Float) = ((value - start1) / (stop1 - start1)).coerceIn(0f, 1f)

fun randomId(): String = NanoIdUtils.randomNanoId(NanoIdUtils.DEFAULT_NUMBER_GENERATOR, NanoIdUtils.DEFAULT_ALPHABET, 8)

fun Int.fitInUnit(unit: Int) = this / unit * unit
fun Int.fitInUnitCeil(unit: Int) = ceil(toDouble() / unit).toInt() * unit

fun Float.fitInUnit(unit: Int) = (this / unit).roundToInt() * unit

@Suppress("NOTHING_TO_INLINE")
inline operator fun String.rem(other: Any?) = format(other)
