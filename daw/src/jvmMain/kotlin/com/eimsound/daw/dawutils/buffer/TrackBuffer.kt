package com.eimsound.daw.dawutils.buffer

class TrackBuffer(channels: Int, bufferSize: Int) {
    private var position = 0
    var buffers = Array(channels) { FloatArray(bufferSize) }
        private set
    private var latencyBuffers: Array<FloatArray> = emptyArray()
    private var oldLatency = 0

    fun putBuffers(buffers: Array<FloatArray>, channels: Int, bufferSize: Int) {
        if (buffers.size != channels || buffers[0].size != bufferSize) {
            this.buffers = Array(channels) { FloatArray(bufferSize) }
        }
        repeat(buffers.size.coerceAtMost(this.buffers.size)) {
            buffers[it].copyInto(this.buffers[it])
        }
    }

    fun popAndMixBuffersTo(buffers: Array<FloatArray>, latency: Int) {
        if (buffers.isEmpty()) return
        val bufferSize = buffers[0].size
        if (latencyBuffers.size < buffers.size || latencyBuffers[0].size < latency + bufferSize) {
            latencyBuffers = Array(buffers.size) { FloatArray(latency + bufferSize) }
            position = 0
        } else if (oldLatency != latency) {
            oldLatency = latency
            position = 0
            latencyBuffers.forEach { it.fill(0F) }
        }

        if (buffers.isEmpty()) return
        val latencyCacheSize = latencyBuffers[0].size

        if (latency == 0) {
            repeat(buffers.size.coerceAtMost(this.buffers.size)) { ch ->
                val outBuffer = buffers[ch]
                val buffer = this.buffers[ch]
                repeat(bufferSize) { i ->
                    outBuffer[i] += buffer[i]
                }
            }
            return
        }

        repeat(buffers.size.coerceAtMost(this.buffers.size)) { ch ->
            val buffer = buffers[ch]
            val latencyBuffer = latencyBuffers[ch]

            // pop and mix latencyBuffers to buffer
            repeat(bufferSize) { i ->
                var cur = position + i
                if (cur >= latencyCacheSize) cur -= latencyCacheSize
                buffer[i] += latencyBuffer[cur]
            }

            var pushIndex = position + latency
            if (pushIndex >= latencyCacheSize) pushIndex -= latencyCacheSize
            val pushRemains = (bufferSize - (latencyCacheSize - pushIndex)).coerceAtLeast(0)
            this.buffers[ch].copyInto(latencyBuffer, pushIndex, 0, bufferSize - pushRemains)
            this.buffers[ch].copyInto(latencyBuffer, 0, bufferSize - pushRemains, bufferSize)
        }

        position += bufferSize
        if (position >= latencyCacheSize) position -= latencyCacheSize
    }
}

class TrackBuffers {
    private val buffers = mutableListOf<TrackBuffer>()

    fun checkSize(size: Int, channels: Int, bufferSize: Int) {
        if (buffers.size < size) {
            repeat(size - buffers.size) {
                buffers.add(TrackBuffer(channels, bufferSize))
            }
        } else if (buffers.size > size) {
            buffers.subList(size, buffers.size).clear()
        }
    }

    operator fun get(index: Int) = buffers[index]
}
