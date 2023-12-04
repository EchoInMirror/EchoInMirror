package com.eimsound.dsp.timestretcher.dsp

class FIFOAudioBuffer(private val cacheSize: Int) {
    private var pushPos = 0
    private var popPos = 0
    private val buffer = FloatArray(cacheSize)
    val available: Int get() = if (pushPos >= popPos) pushPos - popPos else cacheSize - (popPos - pushPos)

    fun push(buffer: FloatArray) {
        val length = buffer.size
        var pushIndex = pushPos
        if (pushIndex >= cacheSize) pushIndex -= cacheSize
        val pushRemains = (length - (cacheSize - pushIndex)).coerceAtLeast(0)
        buffer.copyInto(this.buffer, pushIndex, 0, length - pushRemains)
        buffer.copyInto(this.buffer, 0, length - pushRemains, length)

        pushPos += length
        if (pushPos >= cacheSize) pushPos -= cacheSize
    }

    fun pop(buffer: FloatArray) {
        val length = buffer.size
        var popIndex = popPos
        if (popIndex >= cacheSize) popIndex -= cacheSize
        val popRemains = (length - (cacheSize - popIndex)).coerceAtLeast(0)
        this.buffer.copyInto(buffer, 0, popIndex, popIndex + length - popRemains)
        this.buffer.copyInto(buffer, length - popRemains, 0, popRemains)

        popPos += length
        if (popPos >= cacheSize) popPos -= cacheSize
    }
}