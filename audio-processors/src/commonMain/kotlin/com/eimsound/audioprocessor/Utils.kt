@file:Suppress("unused")

package com.eimsound.audioprocessor

fun Array<FloatArray>.mixWith(source: Array<FloatArray>) {
    for (i in 0 until size.coerceAtMost(source.size)) {
        for (j in 0 until this[i].size.coerceAtMost(source[i].size)) {
            this[i][j] += source[i][j]
        }
    }
}

fun Array<FloatArray>.mixWith(source: Array<FloatArray>, volume: Float) {
    for (i in 0 until size.coerceAtMost(source.size)) {
        for (j in 0 until this[i].size.coerceAtMost(source[i].size)) {
            this[i][j] += source[i][j] * volume
        }
    }
}

fun Array<FloatArray>.mixWith(source: Array<FloatArray>, volume: Float, limit: Float) {
    for (i in 0 until size.coerceAtMost(source.size)) {
        for (j in 0 until this[i].size.coerceAtMost(source[i].size)) {
            this[i][j] += source[i][j].coerceIn(-limit, limit) * volume
        }
    }
}

fun Array<FloatArray>.mixWith(source: Array<FloatArray>, volume: Float, limit: Float, length: Int) {
    for (i in 0 until size.coerceAtMost(source.size)) {
        for (j in 0 until length.coerceAtMost(this[i].size.coerceAtMost(source[i].size))) {
            this[i][j] += source[i][j].coerceIn(-limit, limit) * volume
        }
    }
}

fun Array<FloatArray>.clear() { repeat(size) { this[it].fill(0F) } }

class PauseProcessor {
    private var step = 0F
    private var cur = 0F
    fun processPause(buffers: Array<FloatArray>, offset: Int, length: Int, timeInSamples: Int) {
        if (timeInSamples > 0 && cur <= 0F) {
            cur = 1F
            step = 1F / timeInSamples
        }
        if (timeInSamples > 0) {
            buffers.forEach {
                var c = cur
                for (i in offset until length) {
                    it[i] *= c
                    c -= step
                }
            }
            cur -= step * length
        }
    }
}
