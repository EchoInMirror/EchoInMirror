package com.eimsound.dsp.data

class AudioBufferQueue(
    private val channels: Int,
    private val cacheSize: Int
) {
    private var pushPos = 0
    private var popPos = 0
    private val buffers = Array(channels) { FloatArray(cacheSize) }
    val available: Int get() = if (pushPos >= popPos) pushPos - popPos else cacheSize - (popPos - pushPos)

    fun push(buffers: Array<FloatArray>) {
        if (buffers.size != channels) {
            this.buffers.forEach { it.fill(0F) }
            return
        }

        val length = buffers[0].size
        repeat(channels.coerceAtMost(buffers.size)) {
            var pushIndex = pushPos
            if (pushIndex >= cacheSize) pushIndex -= cacheSize
            val pushRemains = (length - (cacheSize - pushIndex)).coerceAtLeast(0)
            buffers[it].copyInto(this.buffers[it], pushIndex, 0, length - pushRemains)
            buffers[it].copyInto(this.buffers[it], 0, length - pushRemains, length)
        }

        pushPos += length
        if (pushPos >= cacheSize) pushPos -= cacheSize
    }

    fun pop(buffers: Array<FloatArray>) {
        if (buffers.size != channels) {
            buffers.forEach { it.fill(0F) }
            return
        }

        val length = buffers[0].size
        repeat(channels.coerceAtMost(buffers.size)) {
            var popIndex = popPos
            if (popIndex >= cacheSize) popIndex -= cacheSize
            val popRemains = (length - (cacheSize - popIndex)).coerceAtLeast(0)
            this.buffers[it].copyInto(buffers[it], 0, popIndex, popIndex + length - popRemains)
            this.buffers[it].copyInto(buffers[it], length - popRemains, 0, popRemains)
        }

        popPos += length
        if (popPos >= cacheSize) popPos -= cacheSize
    }

    fun seekPopIndex(index: Int) {
        popPos += index
        if (popPos >= cacheSize) popPos -= cacheSize
        else if (popPos < 0) popPos += cacheSize
    }
}
