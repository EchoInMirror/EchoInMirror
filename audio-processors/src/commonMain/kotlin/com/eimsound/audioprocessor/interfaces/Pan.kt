package com.eimsound.audioprocessor.interfaces

import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class PannerRule {
    /**
     * Regular 6 dB or linear panning rule, allows the panned sound to be
     * perceived as having a constant level when summed to mono.
     */
    LINEAR,
    /**
     * Both left and right are 1 when pan value is 0, with left decreasing
     * to 0 above this value and right decreasing to 0 below it.
     */
    BALANCED,
    /**
     * Alternate version of the regular 3 dB panning rule with a sine curve.
     */
    SINE3DB,
    /**
     * Alternate version of the regular 4.5 dB panning rule with a sine curve.
     */
    SINE4_5DB,
    /**
     * Alternate version of the regular 6 dB panning rule with a sine curve.
     */
    SINE6DB,
    /**
     * Regular 3 dB or constant power panning rule, allows the panned sound
     * to be perceived as having a constant level regardless of the pan position.
     */
    SQUARE_ROOT_3DB,
    /**
     * Regular 4.5 dB or constant power panning rule, allows the panned sound
     * to be perceived as having a constant level regardless of the pan position.
     */
    SQUARE_ROOT_4_5DB,
}

interface Pan {
    var pan: Float
    val pannerRule: PannerRule
}

private const val PI_F = 3.1415927F
// 2 ^ (1 / 12)
private const val SQRT2 = 1.4142135F
// 2 ^ (3 / 4)
private const val SQRT4_5 = 2.828427F

fun Pan.calcPanLeftChannel(): Float {
    val normalisedPan = 0.5F * (pan + 1)
    return when (pannerRule) {
        PannerRule.LINEAR -> (1 - normalisedPan) * 2
        PannerRule.BALANCED -> min(1F, 1 - normalisedPan * 2)
        PannerRule.SINE3DB -> sin(0.5F * PI_F * (1 - normalisedPan)) * SQRT2
        PannerRule.SINE4_5DB -> sin(0.5F * PI_F * (1 - normalisedPan)).pow(1.5F) * SQRT4_5
        PannerRule.SINE6DB -> sin(0.5F * PI_F * (1 - normalisedPan)).pow(2) * 2
        PannerRule.SQUARE_ROOT_3DB -> sqrt(1 - normalisedPan)
        PannerRule.SQUARE_ROOT_4_5DB -> sqrt(sqrt(1 - normalisedPan)).pow(1.5F)
    }
}

fun Pan.calcPanRightChannel(): Float {
    val normalisedPan = 0.5F * (pan + 1)
    return when (pannerRule) {
        PannerRule.LINEAR -> normalisedPan * 2
        PannerRule.BALANCED -> min(1F, normalisedPan * 2)
        PannerRule.SINE3DB -> sin(0.5F * PI_F * normalisedPan) * SQRT2
        PannerRule.SINE4_5DB -> sin(0.5F * PI_F * normalisedPan).pow(1.5F) * SQRT4_5
        PannerRule.SINE6DB -> sin(0.5F * PI_F * normalisedPan).pow(2) * 2
        PannerRule.SQUARE_ROOT_3DB -> sqrt(normalisedPan)
        PannerRule.SQUARE_ROOT_4_5DB -> sqrt(sqrt(normalisedPan)).pow(1.5F)
    }
}
