package com.eimsound.audioprocessor

fun Array<FloatArray>.mixWith(source: Array<FloatArray>) {
    for (i in 0 until size.coerceAtMost(source.size)) {
        for (j in 0 until this[i].size.coerceAtMost(source[i].size)) {
            this[i][j] += source[i][j]
        }
    }
}

fun Array<FloatArray>.mixWith(source: Array<FloatArray>, volume: Float = 1F) {
    for (i in 0 until size.coerceAtMost(source.size)) {
        for (j in 0 until this[i].size.coerceAtMost(source[i].size)) {
            this[i][j] += source[i][j] * volume
        }
    }
}
