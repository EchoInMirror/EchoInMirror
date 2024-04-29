package com.eimsound.audioprocessor.data

import com.eimsound.audioprocessor.PlayPosition
import com.eimsound.daw.commons.DividerAbove
import com.eimsound.daw.language.langs
import kotlin.math.roundToInt

open class Quantification(
    open val name: String,
    val value: Int = 1, val isSpecial: Boolean = false,
    val timesSigNumerator: Boolean = false, val timesSigDenominator: Boolean = false,
    override val hasDividerAbove: Boolean = false
): DividerAbove {
    override fun toString() = name
}

private class InternalQuantification(
    private val index: Int,
    value: Int = 1, isSpecial: Boolean = false,
    timesSigNumerator: Boolean = false, timesSigDenominator: Boolean = false,
    override val hasDividerAbove: Boolean = false
): Quantification("", value, isSpecial, timesSigNumerator, timesSigDenominator) {
    override val name get() = langs.quantificationUnits[index]
}

val QUANTIFICATION_UNITS = listOf<Quantification>(
    InternalQuantification(0, isSpecial = true, timesSigNumerator = true), // 小节
    InternalQuantification(1, isSpecial = true, hasDividerAbove = true), // 节拍
    InternalQuantification(2, value = 2), // 1/2 拍
    InternalQuantification(3, value = 3), // 1/3 拍
    InternalQuantification(4, value = 4), // 1/4 拍
    InternalQuantification(5, value = 6), // 1/6 拍
    InternalQuantification(6, value = 16, timesSigDenominator = true, isSpecial = true, hasDividerAbove = true), // 步进
    InternalQuantification(7, value = 2 * 16, timesSigDenominator = true), // 1/2 步
    InternalQuantification(8, value = 3 * 16, timesSigDenominator = true), // 1/3 步
    InternalQuantification(9, value = 4 * 16, timesSigDenominator = true), // 1/4 步
    InternalQuantification(10, value = 6 * 16, timesSigDenominator = true), // 1/6 步
    InternalQuantification(11, value = 0, isSpecial = true, hasDividerAbove = true) // 无
)
val defaultQuantification = QUANTIFICATION_UNITS[4]

fun Quantification.getEditUnit(ppq: Int, timeSigDenominator: Int, timeSigNumerator: Int): Int {
    if (value == 0) return 1
    var ret = ppq.toDouble() / value
    if (timesSigDenominator) ret *= timeSigDenominator
    if (timesSigNumerator) ret *= timeSigNumerator
    return ret.roundToInt().coerceAtLeast(1)
}
fun Quantification.getEditUnit(position: PlayPosition) = getEditUnit(position.ppq, position.timeSigDenominator,
    position.timeSigNumerator)
