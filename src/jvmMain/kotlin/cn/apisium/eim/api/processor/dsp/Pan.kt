package cn.apisium.eim.api.processor.dsp

interface Pan {
    var pan: Float
}

fun Pan.calcPanLeftChannel(): Float {
    val normalisedPan = 0.5F * (pan + 1)
    return 1 - normalisedPan
}

fun Pan.calcPanRightChannel(): Float {
    return 0.5F * (pan + 1)
}
