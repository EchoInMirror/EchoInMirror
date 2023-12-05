package com.eimsound.audioprocessor.data

import com.eimsound.audioprocessor.CurrentPosition
import com.eimsound.daw.commons.DividerAbove
import kotlin.math.roundToInt

data class Quantification(
    val name: String, val value: Int = 1, val isSpecial: Boolean = false,
    val timesSigNumerator: Boolean = false, val timesSigDenominator: Boolean = false,
    override val hasDividerAbove: Boolean = false
): DividerAbove {
    override fun toString() = name
}

val QUANTIFICATION_UNITS = listOf(
    Quantification("小节", isSpecial = true, timesSigNumerator = true),
    Quantification("节拍", isSpecial = true, hasDividerAbove = true),
    Quantification("1/2 拍", value = 2),
    Quantification("1/3 拍", value = 3),
    Quantification("1/4 拍", value = 4),
    Quantification("1/6 拍", value = 6),
    Quantification("步进", value = 16, timesSigDenominator = true, isSpecial = true, hasDividerAbove = true),
    Quantification("1/2 步", value = 2 * 16, timesSigDenominator = true),
    Quantification("1/3 步", value = 3 * 16, timesSigDenominator = true),
    Quantification("1/4 步", value = 4 * 16, timesSigDenominator = true),
    Quantification("1/6 步", value = 6 * 16, timesSigDenominator = true),
    Quantification("无", value = 0, isSpecial = true, hasDividerAbove = true)
)
val defaultQuantification = QUANTIFICATION_UNITS[4]

fun Quantification.getEditUnit(ppq: Int, timeSigDenominator: Int, timeSigNumerator: Int): Int {
    if (value == 0) return 1
    var ret = ppq.toDouble() / value
    if (timesSigDenominator) ret *= timeSigDenominator
    if (timesSigNumerator) ret *= timeSigNumerator
    return ret.roundToInt().coerceAtLeast(1)
}
fun Quantification.getEditUnit(position: CurrentPosition) = getEditUnit(position.ppq, position.timeSigDenominator,
    position.timeSigNumerator)
