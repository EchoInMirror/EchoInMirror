package com.eimsound.dsp.data

class AudioBufferQueue(
    val channels: Int,
    private val cacheSize: Int
) {
    private var pushPos = 0
    private var popPos = 0
    private val buffers = Array(channels) { FloatArray(cacheSize) }
    val available: Int get() = if (pushPos >= popPos) pushPos - popPos else cacheSize - (popPos - pushPos)

    fun push(buffers: Array<FloatArray>) {
        push(buffers, 0, buffers[0].size)
    }

    fun push(buffers: Array<FloatArray>, offset: Int, length: Int) {
        if (buffers.size != channels) {
            this.buffers.forEach { it.fill(0F) }
            return
        }

        repeat(channels.coerceAtMost(buffers.size)) {
            var pushIndex = pushPos
            if (pushIndex >= cacheSize) pushIndex -= cacheSize
            val pushRemains = (length - (cacheSize - pushIndex)).coerceAtLeast(0)
            buffers[it].copyInto(this.buffers[it], pushIndex, offset, offset + length - pushRemains)
            buffers[it].copyInto(this.buffers[it], 0, offset + length - pushRemains, offset + length)
        }

        pushPos += length
        if (pushPos >= cacheSize) pushPos -= cacheSize
    }

    fun pop(buffers: Array<FloatArray>) {
        pop(buffers, 0, buffers[0].size.coerceAtMost(available))
    }

    fun pop(buffers: Array<FloatArray>, offset: Int, length: Int) {
        if (buffers.size != channels) {
            buffers.forEach { it.fill(0F) }
            return
        }

        repeat(channels.coerceAtMost(buffers.size)) {
            var popIndex = popPos
            if (popIndex >= cacheSize) popIndex -= cacheSize
            val popRemains = (length - (cacheSize - popIndex)).coerceAtLeast(0)
            this.buffers[it].copyInto(buffers[it], offset, popIndex, popIndex + length - popRemains)
            this.buffers[it].copyInto(buffers[it], offset + length - popRemains, 0, popRemains)
        }

        popPos += length
        if (popPos >= cacheSize) popPos -= cacheSize
    }

    fun seekPopIndex(index: Int) {
        popPos += index
        if (popPos >= cacheSize) popPos -= cacheSize
        else if (popPos < 0) popPos += cacheSize
    }

    fun clear() {
        pushPos = 0
        popPos = 0
    }
}
