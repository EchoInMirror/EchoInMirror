package com.eimsound.daw.data

import com.eimsound.daw.EchoInMirror
import kotlin.math.roundToInt

data class Quantification(val name: String, val value: Int = 1, val isSpecial: Boolean = false,
                          val timesSigNumerator: Boolean = false, val timesSigDenominator: Boolean = false,
                          val hasDividerAbove: Boolean = false)

val quantificationUnits = arrayOf(
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
val defaultQuantification = quantificationUnits[4]

fun getEditUnit() = EchoInMirror.quantification.run {
    if (value == 0) return@run 1
    var ret = EchoInMirror.currentPosition.ppq.toDouble() / value
    if (timesSigDenominator) ret *= EchoInMirror.currentPosition.timeSigDenominator
    if (timesSigNumerator) ret *= EchoInMirror.currentPosition.timeSigNumerator
    return@run ret.roundToInt().coerceAtLeast(1)
}
